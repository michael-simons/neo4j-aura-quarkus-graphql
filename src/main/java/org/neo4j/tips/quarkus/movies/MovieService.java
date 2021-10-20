package org.neo4j.tips.quarkus.movies;

import static org.neo4j.cypherdsl.core.Cypher.anonParameter;
import static org.neo4j.cypherdsl.core.Cypher.mapOf;
import static org.neo4j.cypherdsl.core.Cypher.match;
import static org.neo4j.cypherdsl.core.Cypher.name;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Functions.collect;
import static org.neo4j.cypherdsl.core.executables.ExecutableStatement.makeExecutable;

import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.SelectedField;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.tips.quarkus.people.Person;
import org.neo4j.tips.quarkus.utils.Neo4jService;

@Singleton
public class MovieService extends Neo4jService {

	public MovieService(Driver driver) {
		super(driver);
	}

	public CompletableFuture<List<Actor>> findRoles(Movie movie, List<Person> people) {

		var p = node("Person").named("p");
		var m = node("Movie").named("m");
		var statement = makeExecutable(match(p.relationshipTo(m, "ACTED_IN").named("r"))
			.where(m.internalId().eq(anonParameter(movie.id())))
			.returning(p.property("name").as("name"), name("r").property("roles").as("roles")).build());

		var peopleByName = people.stream().collect(Collectors.toMap(Person::name, Function.identity()));

		return executeReadStatement(statement,
			r -> new Actor(peopleByName.get(r.get("name").asString()), r.get("roles").asList(Value::asString)));
	}

	public CompletableFuture<List<Movie>> findMovies(
		String titleFilter,
		Person personFilter,
		DataFetchingFieldSelectionSet selectionSet
	) {

		var m = node("Movie").named("m");

		PatternElement patternToMatch = m;
		if (personFilter != null) {
			var p = node("Person").named("p").withProperties("name", anonParameter(personFilter.name()));
			patternToMatch = p.relationshipTo(m, "ACTED_IN");
		}

		var match = match(patternToMatch);

		var returnedExpressions = new ArrayList<Expression>();
		returnedExpressions.add(Functions.id(m).as("id"));
		if (selectionSet.contains("actors") || personFilter != null) {
			var a = name("a");
			var r = name("actedIn");
			match = match.match(m.relationshipFrom(node("Person").named(a), "ACTED_IN").named(r));
			returnedExpressions.add(collect(mapOf("roles", r.property("roles"), "person", a)).as("actors"));
		}

		selectionSet.getImmediateFields().stream().map(SelectedField::getName)
			.distinct()
			.filter(n -> !("actors".equals(n) || "id".equals(n)))
			.map(n -> m.property(n).as(n))
			.forEach(returnedExpressions::add);

		var statement = makeExecutable(
			match.where(Optional.ofNullable(titleFilter).map(String::trim).filter(Predicate.not(String::isBlank))
				.map(v -> m.property("title").contains(anonParameter(titleFilter)))
				.orElseGet(Conditions::noCondition))
				.returning(returnedExpressions.toArray(Expression[]::new))
				.build()
		);
		return executeReadStatement(statement, Movie::of);
	}
}
