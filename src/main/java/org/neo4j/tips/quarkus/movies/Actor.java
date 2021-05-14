package org.neo4j.tips.quarkus.movies;

import java.util.List;

import org.neo4j.tips.quarkus.people.Person;

public class Actor {

	private List<String> roles;

	private Person node;

	public Actor(Person node, List<String> roles) {
		this.roles = roles;
		this.node = node;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public Person getNode() {
		return node;
	}

	public void setNode(Person node) {
		this.node = node;
	}
}
