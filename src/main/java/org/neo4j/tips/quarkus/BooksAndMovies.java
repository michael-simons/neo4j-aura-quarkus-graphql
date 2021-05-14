package org.neo4j.tips.quarkus;

import graphql.schema.DataFetchingEnvironment;
import io.smallrye.graphql.api.Context;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.neo4j.tips.quarkus.movies.Movie;
import org.neo4j.tips.quarkus.movies.MovieService;
import org.neo4j.tips.quarkus.people.PeopleService;
import org.neo4j.tips.quarkus.people.Person;

@GraphQLApi
public class BooksAndMovies {

	private final Context context;

	private final PeopleService peopleService;

	private final MovieService movieService;

	@Inject
	public BooksAndMovies(Context context, PeopleService peopleService, MovieService movieService) {
		this.context = context;
		this.peopleService = peopleService;
		this.movieService = movieService;
	}

	@Query("people")
	public CompletableFuture<List<Person>> getPeople(@Name("nameFilter") String nameFilter) {

		DataFetchingEnvironment env = context.unwrap(DataFetchingEnvironment.class);
		return peopleService.findPeople(nameFilter, env.getSelectionSet());
	}

	@Query("movies")
	public CompletableFuture<List<Movie>> getMovies(@Name("titleFilter") String titleFilter) {

		DataFetchingEnvironment env = context.unwrap(DataFetchingEnvironment.class);
		return movieService.findMovies(titleFilter, null, env.getSelectionSet());
	}

	public CompletionStage<List<Movie>> actedIn(@Source Person person) {

		if (person.getActedIn() != null) {
			return CompletableFuture.completedFuture(person.getActedIn());
		}

		DataFetchingEnvironment env = context.unwrap(DataFetchingEnvironment.class);
		return movieService.findMovies(null, person, env.getSelectionSet());
	}

	@Description("A short biographie of the person, maybe empty if there is none to be found.")
	public CompletionStage<String> shortBio(@Source Person person) {

		return peopleService.getShortBio(person);
	}
}
