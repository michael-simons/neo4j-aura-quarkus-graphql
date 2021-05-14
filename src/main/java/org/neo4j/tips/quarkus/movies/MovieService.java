package org.neo4j.tips.quarkus.movies;

import static org.neo4j.cypherdsl.core.Cypher.anonParameter;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.executables.ExecutableStatement.makeExecutable;

import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.SelectedField;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;

import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.tips.quarkus.people.Person;
import org.neo4j.tips.quarkus.utils.RecordMapAccessor;

@ApplicationScoped
public class MovieService {

	private final Driver driver;

	public MovieService(Driver driver) {
		this.driver = driver;
	}

	public CompletableFuture<List<Movie>> findMovies(
		String titleFilter,
		Person personFilter,
		DataFetchingFieldSelectionSet selectionSet
	) {

		var m = node("Movie").named("m");

		PatternElement patternToMatch = m;
		if (personFilter != null) {
			var p = node("Person").named("p").withProperties("name", anonParameter(personFilter.getName()));
			patternToMatch = p.relationshipTo(m, "ACTED_IN");
		}

		var returnedExpressions = new ArrayList<Expression>();
		returnedExpressions.add(Functions.id(m).as("id"));
		if (selectionSet.contains("actors") || personFilter != null) {
			var a = Cypher.name("a");
			var r = Cypher.name("actedIn");
			patternToMatch = m.relationshipFrom(node("Person").named(a), "ACTED_IN").named(r);
			returnedExpressions
				.add(Functions.collect(Cypher.mapOf("roles", r.property("roles"), "person", a)).as("actors"));
		}

		var match = Cypher.match(patternToMatch)
			.where(Optional.ofNullable(titleFilter).map(String::trim).filter(Predicate.not(String::isBlank))
				.map(v -> m.property("title").contains(anonParameter(titleFilter)))
				.orElseGet(Conditions::noCondition));

		selectionSet.getImmediateFields().stream().map(SelectedField::getName)
			.distinct()
			.filter(n -> !("actors".equals(n) || "id".equals(n)))
			.map(n -> m.property(n).as(n))
			.forEach(returnedExpressions::add);

		var statement = makeExecutable(match.returning(returnedExpressions.toArray(Expression[]::new)).build());
		System.out.println("running " + statement.getCypher());
		var session = driver.asyncSession();
		return session
			.readTransactionAsync(tx -> statement.fetchWith(tx, Movie::of))
			.thenCompose(result -> session.closeAsync().thenApply(i -> result))
			.toCompletableFuture();
	}
}
