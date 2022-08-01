package org.neo4j.tips.quarkus.books;

import java.util.Comparator;
import java.util.List;

import org.eclipse.microprofile.graphql.Id;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.tips.quarkus.people.Person;
import org.neo4j.tips.quarkus.utils.RecordMapAccessor;

public record Book(

	@Id
	Long id,

	String title,

	State state,

	List<Person> authors
) {

	enum State {

		READ,
		UNREAD,
		UNKNOWN;

		static State of(String value) {
			if (value == null) {
				return null;
			}
			return switch (value) {
				case "R" -> State.READ;
				case "U" -> State.UNREAD;
				default -> State.UNKNOWN;
			};
		}
	}

	public static Book of(Record r) {
		return of(new RecordMapAccessor(r));
	}

	public static Book of(MapAccessor r) {

		return new Book(
			r instanceof Node node ? node.id() : r.get("id").asLong(),
			r.get("title").asString(r.get("b.title").asString()),
			State.of(r.get("state").asString(r.get("b.state").asString())),
			r.containsKey("authors") ? r.get("authors")
				.asList(Value::asNode)
				.stream()
				.map(Person::of)
				.sorted(Comparator.comparing(Person::name))
				.toList() : null
		);
	}
}
