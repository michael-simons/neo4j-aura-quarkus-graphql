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

	public CompletableFuture<List<Book>> updateBooks(boolean unreadOnly) {

		var row = name("row");
		var author = name("author");
		var authors = name("authors");

		var person = node("Person").withProperties("name", trim(author)).named("a");
		var book = node("Book").named("b");

		var bookTitle = book.property("title");
		var bookState = book.property("state");

		var conditions = createDefaultBookCondition(book, unreadOnly);

		var statement = makeExecutable(
			loadCSV(URI.create("https://raw.githubusercontent.com/michael-simons/goodreads/master/all.csv"), false)
			.as(row).withFieldTerminator(";")
			.merge(book.withProperties(bookTitle, trim(valueAt(row, 1))))
			.set(
				book.property("type").to(valueAt(row, 2)),
				bookState.to(valueAt(row, 3))
			)
			.with(book, row)
			.unwind(split(valueAt(row, 0), "&")).as(author)
			.with(book, split(author, ",").as(author))
			.with(book, trim(coalesce(valueAt(author, 1), literalOf(""))).concat(literalOf(" "))
				.concat(trim(valueAt(author, 0))).as(author))
			.merge(person)
			.merge(person.relationshipTo(book, "WROTE").named("r"))
			.with(book, collect(person).as(authors))
			.where(conditions)
			.returning(book.internalId().as("id"), bookTitle, bookState, authors )
			.build());

		return executeWriteStatement(statement, Book::of);
	}

	public CompletableFuture<List<Book>> findBooks(
		String titleFilter,
		Person personFilter,
		boolean unreadOnly,
		DataFetchingFieldSelectionSet selectionSet
	) {

		var book = node("Book").named("b");
		var conditions = createDefaultBookCondition(book, unreadOnly);

		PatternElement patternToMatch = book;
		if (personFilter != null) {
			var p = node("Person").named("p");
			patternToMatch = p.relationshipTo(book, "WROTE");
			conditions = conditions.and(p.property("name").contains(anonParameter(personFilter.getName())));
		}

		var match = match(patternToMatch);

		var returnedExpressions = new ArrayList<Expression>();
		returnedExpressions.add(Functions.id(book).as("id"));
		if (selectionSet.contains("authors") || personFilter != null) {
			var a = name("a");
			var r = name("w");
			match = match.match(book.relationshipFrom(node("Person").named(a), "WROTE").named(r));
			returnedExpressions.add(collect(a).as("authors"));
		}

		selectionSet.getImmediateFields().stream().map(SelectedField::getName)
			.distinct()
			.filter(n -> !("authors".equals(n) || "id".equals(n)))
			.map(n -> book.property(n).as(n))
			.forEach(returnedExpressions::add);

		var statement = makeExecutable(
			match.where(Optional.ofNullable(titleFilter).map(String::trim).filter(Predicate.not(String::isBlank))
				.map(v -> book.property("title").contains(anonParameter(titleFilter)))
				.orElseGet(Conditions::noCondition))
				.and(conditions)
				.returning(returnedExpressions.toArray(Expression[]::new))
				.build()
		);
		return executeReadStatement(statement, Book::of);
	}

	private static Condition createDefaultBookCondition(Node bookNode, boolean unreadOnly) {

		var conditions = Conditions.noCondition();
		if (unreadOnly) {
			conditions = bookNode.property("state").isEqualTo(literalOf("U"));
		}
		return conditions;
	}
}
