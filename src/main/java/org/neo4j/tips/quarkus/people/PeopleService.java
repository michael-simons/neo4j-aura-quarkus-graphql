package org.neo4j.tips.quarkus.people;

import static org.neo4j.cypherdsl.core.Cypher.anonParameter;
import static org.neo4j.cypherdsl.core.Cypher.match;
import static org.neo4j.cypherdsl.core.Cypher.name;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Functions.collect;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.driver.Driver;
import org.neo4j.tips.quarkus.movies.Movie;
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

	public CompletableFuture<List<Person>> findPeople(String nameFilter, Movie movieFilter,
		DataFetchingFieldSelectionSet selectionSet) {

		var returnedExpressions = new ArrayList<Expression>();
		var person = node("Person").named("p");

		var match = match(person).with(person);
		if (movieFilter != null) {
			var movie = node("Movie").named("m");
			match = match(person.relationshipTo(movie, "ACTED_IN"))
				.where(movie.internalId().eq(anonParameter(movieFilter.id())))
				.with(person);
		}

		if (selectionSet.contains("actedIn")) {
			var movie = node("Movie").named("m");
			var actedIn = name("actedIn");

			match = match
				.optionalMatch(person.relationshipTo(movie, "ACTED_IN"))
				.with(person, collect(movie).as(actedIn));
			returnedExpressions.add(actedIn);
		}

		if (selectionSet.contains("wrote")) {
			var book = node("Book").named("b");
			var wrote = name("wrote");

			var newVariables = new HashSet<>(returnedExpressions);
			newVariables.addAll(List.of(person.getRequiredSymbolicName(), collect(book).as("wrote")));
			match = match
				.optionalMatch(person.relationshipTo(book, "WROTE"))
				.with(newVariables.toArray(Expression[]::new));
			returnedExpressions.add(wrote);
		}

		Stream.concat(Stream.of("name"), selectionSet.getImmediateFields().stream().map(SelectedField::getName))
			.distinct()
			.filter(n -> !("actedIn".equals(n) || "wrote".equals(n)))
			.map(n -> person.property(n).as(n))
			.forEach(returnedExpressions::add);

		var statement = makeExecutable(
			match
				.where(Optional.ofNullable(nameFilter).map(String::trim).filter(Predicate.not(String::isBlank))
					.map(v -> person.property("name").contains(anonParameter(nameFilter)))
					.orElseGet(Conditions::noCondition))
				.returning(returnedExpressions.toArray(Expression[]::new))
				.build()
		);
		return executeReadStatement(statement, Person::of);
	}

	public CompletableFuture<String> getShortBio(Person person) {

		var wikiUri = URI
			.create("https://en.wikipedia.org/api/rest_v1/page/summary/" + person.name().replaceAll("\\s", "_"));
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
