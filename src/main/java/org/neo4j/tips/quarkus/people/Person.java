package org.neo4j.tips.quarkus.people;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Id;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.tips.quarkus.movies.Movie;
import org.neo4j.tips.quarkus.utils.RecordMapAccessor;

@Description("A person has some information about themselves and maybe played in a movie or is an author and wrote books.")
public class Person {

	public static Person of(Record r) {
		return of(new RecordMapAccessor(r));
	}

	public static Person of(MapAccessor r) {
		var person = new Person();

		person.name = r.get("name").asString();
		person.born = r.containsKey("born") ? r.get("born").asInt() : null;
		person.actedIn = r.containsKey("actedIn") ? r.get("actedIn")
			.asList(Value::asNode)
			.stream()
			.map(Movie::of)
			.collect(Collectors.toList()) : null;
		return person;
	}

	@Id
	private String name;

	private Integer born;

	private List<Movie> actedIn;

	public String getName() {
		return name;
	}

	public Integer getBorn() {
		return born;
	}

	public List<Movie> getActedIn() {
		return actedIn;
	}
}
