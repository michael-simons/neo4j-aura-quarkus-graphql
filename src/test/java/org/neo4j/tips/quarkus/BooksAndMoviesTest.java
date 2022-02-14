package org.neo4j.tips.quarkus;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BooksAndMoviesTest {

	@Test
	public void testQuery() {

		var query = """
			{
				   "query": "{
				   		people(nameFilter: \\"Michael Simons\\") {
				   		     name
				   		     wrote {
				   		     	title
				   		     }
				   		}
				   	} "
			}
			""";

		given()
			.when()
			.contentType("application/json")
			.body(query)
			.post("/graphql")
			.then()
			.statusCode(200)
			.body("data.people[0].wrote.title",
				Matchers.containsInAnyOrder("Spring Boot 2 – Moderne Softwareentwicklung mit Spring.",
					"arc42 by Example"));
	}
}