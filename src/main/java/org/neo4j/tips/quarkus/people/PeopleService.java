package org.neo4j.tips.quarkus.people;

import static org.neo4j.cypherdsl.core.Cypher.anonParameter;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.executables.ExecutableStatement.makeExecutable;

import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.SelectedField;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.driver.Driver;
import org.neo4j.tips.quarkus.utils.Neo4jService;

import com.fasterxml.jackson.databind.ObjectMapper;

@Singleton
public class PeopleService extends Neo4jService {

	private final ObjectMapper objectMapper;

	private final HttpClient httpClient;

	public PeopleService(Driver driver, ObjectMapper objectMapper) {
		super(driver);

		this.objectMapper = objectMapper;

		this.httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	}

	public CompletableFuture<List<Person>> findPeople(String nameFilter, DataFetchingFieldSelectionSet selectionSet) {

		var p = node("Person").named("p");

		var optionalMatches = new ArrayList<PatternElement>();
		var returnedExpressions = new ArrayList<Expression>();
		if (selectionSet.contains("actedIn")) {
			var m = node("Movie").named("m");
			optionalMatches.add(p.relationshipTo(m, "ACTED_IN"));
			returnedExpressions.add(Functions.collect(m).as("actedIn"));
		}

		if (selectionSet.contains("wrote")) {
			var b = node("Book").named("b");
			optionalMatches.add((p.relationshipTo(b, "WROTE")));
			returnedExpressions.add(Functions.collect(b).as("wrote"));
		}

		var match = Cypher.match(p)
			.where(Optional.ofNullable(nameFilter).map(String::trim).filter(Predicate.not(String::isBlank))
				.map(v -> p.property("name").contains(anonParameter(nameFilter)))
				.orElseGet(Conditions::noCondition));

		if (!optionalMatches.isEmpty()) {
			match = match
				.with(p)
				.optionalMatch(optionalMatches.toArray(PatternElement[]::new))
				.where(Conditions.noCondition());
		}

		Stream.concat(Stream.of("name"), selectionSet.getImmediateFields().stream().map(SelectedField::getName))
			.distinct()
			.filter(n -> !("actedIn".equals(n) || "wrote".equals(n)))
			.map(n -> p.property(n).as(n))
			.forEach(returnedExpressions::add);

		var statement = makeExecutable(
			match.returning(returnedExpressions.toArray(Expression[]::new))
				.build()
		);
		return executeStatement(statement, Person::of);
	}

	public CompletableFuture<String> getShortBio(Person person) {

		var wikiUri = URI
			.create("https://en.wikipedia.org/api/rest_v1/page/summary/" + person.getName().replaceAll("\\s", "_"));
		return httpClient
			.sendAsync(HttpRequest.newBuilder().uri(wikiUri).build(), HttpResponse.BodyHandlers.ofInputStream())
			.thenApply(response -> {
				if (response.statusCode() != 200) {
					return "n/a";
				}
				try {
					var summary = objectMapper.readTree(response.body());
					return summary.get("extract").textValue();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
	}
}
