package org.neo4j.tips.quarkus.movies;

import java.util.List;

import org.neo4j.tips.quarkus.people.Person;

public record Actor(

	Person node,

	List<String> roles
) {

	public Actor {
		roles = List.copyOf(roles);
	}
}
