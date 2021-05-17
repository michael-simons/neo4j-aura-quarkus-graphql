package org.neo4j.tips.quarkus.people;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Id;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.tips.quarkus.books.Book;
import org.neo4j.tips.quarkus.movies.Movie;
import org.neo4j.tips.quarkus.utils.RecordMapAccessor;

@Description("A person has some information about themselves and maybe played in a movie or is an author and wrote books.")
public class Person {

	public static Person withName(String name) {
		var person = new Person();
		person.name = name;
		return person;
	}

	public static Person of(Record r) {
		return of(new RecordMapAccessor(r));
	}

	public static Person of(MapAccessor r) {
		var person = new Person();

		person.name = r.get("name").asString();
		person.born = r.containsKey("born") && !r.get("born").isNull() ? r.get("born").asInt(0) : null;
		person.actedIn = r.containsKey("actedIn") ? r.get("actedIn")
			.asList(Value::asNode)
			.stream()
			.map(Movie::of)
			.sorted(Comparator.nullsLast(Comparator.comparing(Movie::getTitle)))
			.collect(Collectors.toList()) : null;
		person.wrote = r.containsKey("wrote") ? r.get("wrote")
			.asList(Value::asNode)
			.stream()
			.map(Book::of)
			.sorted(Comparator.nullsLast(Comparator.comparing(Book::getTitle)))
			.collect(Collectors.toList()) : null;
		return person;
	}

	@Id
	private String name;

	private Integer born;

	private List<Movie> actedIn;

	private List<Book> wrote;

	public String getName() {
		return name;
	}

	public Integer getBorn() {
		return born;
	}

	public List<Movie> getActedIn() {
		return actedIn;
	}

	public List<Book> getWrote() {
		return wrote;
	}
}
