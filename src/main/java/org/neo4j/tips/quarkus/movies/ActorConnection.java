package org.neo4j.tips.quarkus.movies;

import java.util.List;

public record ActorConnection(List<Actor> edges) {

	public ActorConnection {
		edges = List.copyOf(edges);
	}
}
