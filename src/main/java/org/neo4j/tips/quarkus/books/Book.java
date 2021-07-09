package org.neo4j.tips.quarkus.books;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.graphql.Id;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.tips.quarkus.people.Person;
import org.neo4j.tips.quarkus.utils.RecordMapAccessor;

public class Book {

	enum State {

		READ,
		UNREAD,
		UNKNOWN;

		static State of(String value) {
			if (value == null) {
				return null;
			}
			switch (value) {
				case "R":
					return State.READ;
				case "U":
					return State.UNREAD;
				default:
					return State.UNKNOWN;
			}
		}
	}

	public static Book of(Record r) {
		return of(new RecordMapAccessor(r));
	}

	public static Book of(MapAccessor r) {

		var book = new Book();

		book.id = r instanceof Node ? ((Node) r).id() : r.get("id").asLong();
		book.title = r.get("title").asString();
		book.state = State.of(r.get("state").asString());
		book.authors = r.containsKey("authors") ? r.get("authors")
			.asList(Value::asNode)
			.stream()
			.map(Person::of)
			.sorted(Comparator.comparing(Person::getName))
			.collect(Collectors.toList()) : null;
		return book;
	}

	@Id
	private Long id;

	private String title;

	private State state;

	private List<Person> authors;

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public List<Person> getAuthors() {
		return authors;
	}

	public State getState() {
		return state;
	}
}
