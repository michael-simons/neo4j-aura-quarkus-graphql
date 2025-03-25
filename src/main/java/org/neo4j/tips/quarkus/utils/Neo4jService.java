package org.neo4j.tips.quarkus.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.async.AsyncSession;

public abstract class Neo4jService {

	protected final Driver driver;

	protected Neo4jService(Driver driver) {
		this.driver = driver;
	}

	protected <T> CompletableFuture<List<T>> executeReadStatement(Statement statement,
		Function<Record, T> mapper) {

		var session = driver.session(AsyncSession.class);
		return session.executeReadAsync(tx -> tx.runAsync(statement.getCypher(), statement.getCatalog().getParameters()).thenCompose((c) -> c.listAsync(mapper)))
			.thenCompose(result -> session.closeAsync().thenApply(i -> result))
			.toCompletableFuture();
	}

	protected <T> CompletableFuture<List<T>> executeWriteStatement(Statement statement,
		Function<Record, T> mapper) {

		var session = driver.session(AsyncSession.class);
		return session.executeWriteAsync(tx -> tx.runAsync(statement.getCypher(), statement.getCatalog().getParameters()).thenCompose((c) -> c.listAsync(mapper)))
			.thenCompose(result -> session.closeAsync().thenApply(i -> result))
			.toCompletableFuture();
	}
}
