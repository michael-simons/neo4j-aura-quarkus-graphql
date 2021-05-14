package org.neo4j.tips.quarkus.movies;

import java.util.List;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Id;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.tips.quarkus.people.Person;
import org.neo4j.tips.quarkus.utils.RecordMapAccessor;

@Description("A movie")
public class Movie {

	public static Movie of(Record r) {
		return of(new RecordMapAccessor(r));
	}

	public static Movie of(MapAccessor r) {

		var movie = new Movie();

		movie.id = r instanceof Node ? ((Node) r).id() : r.get("id").asLong();
		movie.title = r.get("title").asString();
		movie.tagline = r.get("tagline").asString();
		movie.released = r.containsKey("released") ? r.get("released").asInt() : null;
		movie.actors = new ActorConnection(r.get("actors").asList(v -> {
			var person = Person.of(v.get("person").asNode());
			return new Actor(person, v.get("roles").asList(Value::asString));
		}, List.of()));
		return movie;
	}

	@Id
	private Long id;

	private String title;

	private String tagline;

	private Integer released;

	private ActorConnection actors;

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getTagline() {
		return tagline;
	}

	public Integer getReleased() {
		return released;
	}

	public ActorConnection getActors() {
		return actors;
	}
}
