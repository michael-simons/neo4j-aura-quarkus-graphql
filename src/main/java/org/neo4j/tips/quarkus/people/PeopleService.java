package org.neo4j.tips.quarkus.people;

import static org.neo4j.cypherdsl.core.Cypher.anonParameter;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.driver.Record;
import org.neo4j.tips.quarkus.movies.MovieService;
import org.neo4j.tips.quarkus.utils.RecordMapAccessor;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public final class PeopleService {

	private final Driver driver;

	private final ObjectMapper objectMapper;

	private final HttpClient httpClient;

	public PeopleService(Driver driver, ObjectMapper objectMapper) {

		this.driver = driver;
		this.objectMapper = objectMapper;

		this.httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1)
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
	}

	public CompletableFuture<List<Person>> findPeople(String nameFilter, DataFetchingFieldSelectionSet selectionSet) {

		var p = Cypher.node("Person").named("p");

		PatternElement patternToMatch = p;
		var returnedExpressions = new ArrayList<Expression>();
		if (selectionSet.contains("actedIn")) {
			var m = Cypher.node("Movie").named("m");
			patternToMatch = p.relationshipTo(m, "ACTED_IN");
			returnedExpressions.add(Functions.collect(m).as("actedIn"));
		}

		var match = Cypher.match(patternToMatch)
			.where(Optional.ofNullable(nameFilter).map(String::trim).filter(Predicate.not(String::isBlank))
				.map(v -> p.property("name").contains(anonParameter(nameFilter)))
				.orElseGet(Conditions::noCondition));

		Stream.concat(Stream.of("name"), selectionSet.getImmediateFields().stream().map(SelectedField::getName))
			.distinct()
			.filter(n -> !"actedIn".equals(n))
			.map(n -> p.property(n).as(n))
			.forEach(returnedExpressions::add);

		var statement = makeExecutable(match.returning(returnedExpressions.toArray(Expression[]::new)).build());
		var session = driver.asyncSession();
		System.out.println("running "+ statement.getCypher());
		return session
			.readTransactionAsync(tx -> statement.fetchWith(tx, Person::of))
			.thenCompose(result -> session.closeAsync().thenApply(i -> result))
			.toCompletableFuture();
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
