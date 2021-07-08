package org.neo4j.tips.quarkus.config;

import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;

import javax.enterprise.event.Observes;

// Also remember to configure
// quarkus.smallrye-graphql.events.enabled=true
public final class GraphQLConfig {

	public GraphQL.Builder configureMaxAllowedQueryComplexity(@Observes GraphQL.Builder builder) {

		return builder.instrumentation(new MaxQueryComplexityInstrumentation(64));
	}
}
