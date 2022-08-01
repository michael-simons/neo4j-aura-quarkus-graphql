package org.neo4j.tips.quarkus.people;

import java.util.Comparator;
import java.util.List;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Id;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.tips.quarkus.books.Book;
import org.neo4j.tips.quarkus.movies.Movie;
import org.neo4j.tips.quarkus.utils.RecordMapAccessor;

@Description("A person has some information about themselves and maybe played in a movie or is an author and wrote books.")
public record Person(
	@Id
	String name,

	Integer born,

	List<Movie> actedIn,

	List<Book> wrote
) {

	Person(String name) {
		this(name, null, null, null);
	}

	public static Person withName(String name) {

		if (name == null || name.isBlank()) {
			return null;
		}

		return new Person(name);
	}

	public static Person of(Record r) {
		return of(new RecordMapAccessor(r));
	}

	public static Person of(MapAccessor r) {

		return new Person(
			r.get("name").asString(),
			r.containsKey("born") && !r.get("born").isNull() ? r.get("born").asInt(0) : null,
			r.containsKey("actedIn") ? r.get("actedIn")
				.asList(Value::asNode)
				.stream()
				.map(Movie::of)
				.sorted(Comparator.nullsLast(Comparator.comparing(Movie::title)))
				.toList() : null,
			r.containsKey("wrote") ? r.get("wrote")
				.asList(Value::asNode)
				.stream()
				.map(Book::of)
				.sorted(Comparator.nullsLast(Comparator.comparing(Book::title)))
				.toList() : null
		);
	}
}
