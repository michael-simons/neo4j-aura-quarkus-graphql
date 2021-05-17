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
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.executables.ExecutableResultStatement;
import org.neo4j.driver.Driver;
import org.neo4j.tips.quarkus.people.Person;
import org.neo4j.tips.quarkus.utils.Neo4jService;

@Singleton
public class BookService extends Neo4jService {

	public BookService(Driver driver) {
		super(driver);
	}

	public CompletableFuture<List<Book>> updateBooks() {

		var row = name("row");
		var author = name("author");
		var book = name("b");

		var personNode = node("Person").withProperties("name", trim(author)).named("a");
		var bookNode = node("Book").withProperties("title", trim(valueAt(row, 1))).named(book);

		ExecutableResultStatement statement;
		statement = makeExecutable(
			loadCSV(URI.create("https://raw.githubusercontent.com/michael-simons/goodreads/master/all.csv"), false)
			.as(row).withFieldTerminator(";")
			.merge(bookNode)
			.set(book.property("type").to(valueAt(row, 2)))
			.with(book, row)
			.unwind(split(valueAt(row, 0), "&")).as(author)
			.with(book, split(author, ",").as(author))
			.with(book, trim(coalesce(valueAt(author, 1), literalOf(""))).concat(literalOf(" "))
				.concat(trim(valueAt(author, 0))).as(author))
			.merge(personNode)
			.merge(personNode.relationshipTo(bookNode, "WROTE").named("r"))
			.returning(bookNode.internalId().as("id"), book.property("title").as("title"),
				collect(personNode).as("authors"))
			.build());

		return executeWriteStatement(statement, Book::of);
	}

	public CompletableFuture<List<Book>> findBooks(
		String titleFilter,
		Person personFilter,
		DataFetchingFieldSelectionSet selectionSet
	) {

		var b = node("Book").named("b");

		PatternElement patternToMatch = b;
		Condition authorCondition = Conditions.noCondition();
		if (personFilter != null) {
			var p = node("Person").named("p");
			patternToMatch = p.relationshipTo(b, "WROTE");
			authorCondition = p.property("name").contains(anonParameter(personFilter.getName()));
		}

		var match = match(patternToMatch);

		var returnedExpressions = new ArrayList<Expression>();
		returnedExpressions.add(Functions.id(b).as("id"));
		if (selectionSet.contains("authors") || personFilter != null) {
			var a = name("a");
			var r = name("w");
			match = match.match(b.relationshipFrom(node("Person").named(a), "WROTE").named(r));
			returnedExpressions.add(collect(a).as("authors"));
		}

		selectionSet.getImmediateFields().stream().map(SelectedField::getName)
			.distinct()
			.filter(n -> !("authors".equals(n) || "id".equals(n)))
			.map(n -> b.property(n).as(n))
			.forEach(returnedExpressions::add);

		var statement = makeExecutable(
			match.where(Optional.ofNullable(titleFilter).map(String::trim).filter(Predicate.not(String::isBlank))
				.map(v -> b.property("title").contains(anonParameter(titleFilter)))
				.orElseGet(Conditions::noCondition))
				.and(authorCondition)
				.returning(returnedExpressions.toArray(Expression[]::new))
				.build()
		);
		return executeReadStatement(statement, Book::of);
	}
}
