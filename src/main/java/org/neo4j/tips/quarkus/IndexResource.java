package org.neo4j.tips.quarkus;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class IndexResource {

	@GET
	public Response forward() {
		return Response.seeOther(URI.create("/q/graphql-ui")).build();
	}
}
