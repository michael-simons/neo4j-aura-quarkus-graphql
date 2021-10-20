package org.neo4j.tips.quarkus;

import graphql.schema.DataFetchingEnvironment;
import io.smallrye.graphql.api.Context;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.neo4j.tips.quarkus.books.Book;
import org.neo4j.tips.quarkus.books.BookService;
import org.neo4j.tips.quarkus.movies.ActorConnection;
import org.neo4j.tips.quarkus.movies.Movie;
import org.neo4j.tips.quarkus.movies.MovieService;
import org.neo4j.tips.quarkus.people.PeopleService;
import org.neo4j.tips.quarkus.people.Person;

@GraphQLApi
@ApplicationScoped
public class BooksAndMovies {

	private final Context context;

	private final PeopleService peopleService;

	private final MovieService movieService;

	private final BookService bookService;

	@Inject
	public BooksAndMovies(Context context, PeopleService peopleService, MovieService movieService,
		BookService bookService
	) {
		this.context = context;
		this.peopleService = peopleService;
		this.movieService = movieService;
		this.bookService = bookService;
	}

	@Query("people")
	public CompletableFuture<List<Person>> getPeople(@Name("nameFilter") String nameFilter) {

		var env = context.unwrap(DataFetchingEnvironment.class);
		return peopleService.findPeople(nameFilter, null, env.getSelectionSet());
	}

	@Query("movies")
	public CompletableFuture<List<Movie>> getMovies(@Name("titleFilter") String titleFilter) {

		var env = context.unwrap(DataFetchingEnvironment.class);
		return movieService.findMovies(titleFilter, null, env.getSelectionSet());
	}

	@Query("books")
	public CompletableFuture<List<Book>> getBooks(
		@Name("titleFilter") String titleFilter,
		@Name("authorFilter") String authorFilter,
		@Name("unreadOnly") @DefaultValue("false") boolean unreadOnly
	) {

		var env = context.unwrap(DataFetchingEnvironment.class);
		return bookService.findBooks(titleFilter, Person.withName(authorFilter), unreadOnly, env.getSelectionSet());
	}

	@Mutation
	public CompletableFuture<List<Book>> updateBooks(
		@Name("titleFilter") String titleFilter,
		@Name("authorFilter") String authorFilter,
		@Name("unreadOnly") @DefaultValue("false") boolean unreadOnly
	) {

		return bookService.updateBooks(titleFilter, Person.withName(authorFilter), unreadOnly);
	}

	public CompletionStage<List<Movie>> actedIn(@Source Person person) {

		if (person.actedIn() != null) {
			return CompletableFuture.completedFuture(person.actedIn());
		}

		var env = context.unwrap(DataFetchingEnvironment.class);
		return movieService.findMovies(null, person, env.getSelectionSet());
	}

	public CompletionStage<ActorConnection> actors(@Source Movie movie) {

		if (movie.actors() != null) {
			return CompletableFuture.completedFuture(movie.actors());
		}

		var env = context.unwrap(DataFetchingEnvironment.class);
		return peopleService.findPeople(null, movie, env.getSelectionSet())
			.thenCompose(people -> movieService.findRoles(movie, people))
			.thenApply(actors -> new ActorConnection(actors));
	}

	public CompletionStage<List<Book>> wrote(@Source Person person) {

		if (person.wrote() != null) {
			return CompletableFuture.completedFuture(person.wrote());
		}

		var env = context.unwrap(DataFetchingEnvironment.class);
		return bookService.findBooks(null, person, false, env.getSelectionSet());
	}

	@Description("A short biographie of the person, maybe empty if there is none to be found.")
	public CompletionStage<String> shortBio(@Source Person person) {

		return peopleService.getShortBio(person);
	}
}
