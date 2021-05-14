package org.neo4j.tips.quarkus.movies;

import java.util.List;

public class ActorConnection {

	private final List<Actor> edges;

	public ActorConnection(List<Actor> edges) {
		this.edges = edges;
	}

	public List<Actor> getEdges() {
		return edges;
	}
}
