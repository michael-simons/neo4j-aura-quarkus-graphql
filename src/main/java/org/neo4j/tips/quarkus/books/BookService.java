package org.neo4j.tips.quarkus.books;

import static org.neo4j.cypherdsl.core.Cypher.anonParameter;
import static org.neo4j.cypherdsl.core.Cypher.literalOf;
import static org.neo4j.cypherdsl.core.Cypher.loadCSV;
import static org.neo4j.cypherdsl.core.Cypher.match;
import static org.neo4j.cypherdsl.core.Cypher.name;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Cypher.valueAt;
import static org.neo4j.cypherdsl.core.Functions.coalesce;
import static org.neo4j.cypherdsl.core.Functions.collect;
import static org.neo4j.cypherdsl.core.Functions.split;
import static org.neo4j.cypherdsl.core.Functions.toLower;
import static org.neo4j.cypherdsl.core.Functions.trim;
import static org.neo4j.cypherdsl.core.executables.ExecutableStatement.makeExecutable;

import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.SelectedField;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.inject.Singleton;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.driver.Driver;
import org.neo4j.tips.quarkus.people.Person;
import org.neo4j.tips.quarkus.utils.Neo4jService;

@Singleton
public class BookService extends Neo4jService {

	public BookService(Driver driver) {
		super(driver);
	}

	public CompletableFuture<List<Book>> updateBooks(
		String titleFilter,
		Person authorFilter,
		boolean unreadOnly
	) {

		var row = name("row");
		var authorName = name("author");

		var book = node("Book").named("b");
		var bookTitle = book.property("title");
		var bookState = book.property("state");

		var author = node("Person").withProperties("name", trim(authorName)).named("a");
		var authors = name("authors");

		var conditions = createDefaultBookCondition(book, unreadOnly);
		var additionalConditions = createAdditionalConditions(book, author, titleFilter, authorFilter);

		var statement = makeExecutable(
			loadCSV(URI.create("https://raw.githubusercontent.com/michael-simons/goodreads/master/all.csv"), false)
			.as(row).withFieldTerminator(";")
			.merge(book.withProperties(bookTitle, trim(valueAt(row, 1))))
			.set(
				book.property("type").to(valueAt(row, 2)),
				bookState.to(valueAt(row, 3))
			)
			.with(book, row)
			.unwind(split(valueAt(row, 0), "&")).as(authorName)
			.with(book, split(authorName, ",").as(authorName))
			.with(book, trim(coalesce(valueAt(authorName, 1), literalOf(""))).concat(literalOf(" "))
				.concat(trim(valueAt(authorName, 0))).as(authorName))
			.merge(author)
			.merge(author.relationshipTo(book, "WROTE").named("r"))
			.with(book, author)
			.where(conditions).and(additionalConditions)
			.with(book, collect(author).as(authors))
			.returning(book.internalId().as("id"), bookTitle, bookState, authors )
			.build());

		return executeWriteStatement(statement, Book::of);
	}

	public CompletableFuture<List<Book>> findBooks(
		String titleFilter,
		Person authorFilter,
		boolean unreadOnly,
		DataFetchingFieldSelectionSet selectionSet
	) {

		var book = node("Book").named("b");
		var possibleAuthor = node("Person").named("p");
		var author = node("Person").named("a");

		var conditions = createDefaultBookCondition(book, unreadOnly);
		var additionalConditions = createAdditionalConditions(book, possibleAuthor, titleFilter, authorFilter);

		PatternElement patternToMatch = book;
		if (additionalConditions != Conditions.noCondition()) {
			patternToMatch = possibleAuthor.relationshipTo(book, "WROTE");
			additionalConditions = additionalConditions.and(author.isEqualTo(possibleAuthor));
		}

		var match = match(patternToMatch);

		var returnedExpressions = new ArrayList<Expression>();
		returnedExpressions.add(Functions.id(book).as("id"));
		if (selectionSet.contains("authors") || authorFilter != null) {
			match = match.match(book.relationshipFrom(author, "WROTE"));
			returnedExpressions.add(collect(author).as("authors"));
		}

		Predicate<String> isRequiredField = (String n) ->  "authors".equals(n) || "id".equals(n);
		selectionSet.getImmediateFields().stream().map(SelectedField::getName)
			.distinct()
			.filter(isRequiredField.negate())
			.map(n -> book.property(n).as(n))
			.forEach(returnedExpressions::add);

		var statement = makeExecutable(
			match.where(conditions).and(additionalConditions)
				.returning(returnedExpressions.toArray(Expression[]::new))
				.build()
		);

		return executeReadStatement(statement, Book::of);
	}

	private static Condition createDefaultBookCondition(Node book, boolean unreadOnly) {

		var conditions = Conditions.noCondition();
		if (unreadOnly) {
			conditions = book.property("state").isEqualTo(literalOf("U"));
		}
		return conditions;
	}

	private static Condition createAdditionalConditions(Node book, Node author, String titleFilter, Person authorFilter) {

		var additionalConditions = Optional.ofNullable(titleFilter).map(String::trim)
			.filter(Predicate.not(String::isBlank))
			.map(v -> toLower(book.property("title")).contains(toLower(anonParameter(titleFilter))))
			.orElseGet(Conditions::noCondition);

		if (authorFilter != null) {
			additionalConditions = additionalConditions
				.or(toLower(author.property("name")).contains(toLower(anonParameter(authorFilter.name()))));
		}
		return additionalConditions;
	}
}
