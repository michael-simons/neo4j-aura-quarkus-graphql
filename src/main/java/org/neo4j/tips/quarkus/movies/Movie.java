package org.neo4j.tips.quarkus.movies;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Id;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.tips.quarkus.people.Person;
import org.neo4j.tips.quarkus.utils.RecordMapAccessor;

@Description("A movie")
public record Movie(
	@Id
	Long id,

	String title,

	String tagline,

	Integer released,

	ActorConnection actors
) {
	public static Movie of(Record r) {
		return of(new RecordMapAccessor(r));
	}

	public static Movie of(MapAccessor r) {

		return new Movie(
			r instanceof Node node ? node.id() : r.get("id").asLong(),
			r.get("title").asString(),
			r.get("tagline").asString(),
			r.containsKey("released") ? r.get("released").asInt() : null,
			r.containsKey("actors") ? new ActorConnection(r.get("actors").asList(v -> {
				var person = Person.of(v.get("person").asNode());
				return new Actor(person, v.get("roles").asList(Value::asString));
			})) : null);
	}
}
