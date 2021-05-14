package org.neo4j.tips.quarkus.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.neo4j.cypherdsl.core.executables.ExecutableResultStatement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;

public class Neo4jService {

	protected final Driver driver;

	protected Neo4jService(Driver driver) {
		this.driver = driver;
	}

	protected <T> CompletableFuture<List<T>> executeReadStatement(ExecutableResultStatement statement,
		Function<Record, T> mapper) {

		var session = driver.asyncSession();
		return session.readTransactionAsync(tx -> statement.fetchWith(tx, mapper))
			.thenCompose(result -> session.closeAsync().thenApply(i -> result))
			.toCompletableFuture();
	}

	protected <T> CompletableFuture<List<T>> executeWriteStatement(ExecutableResultStatement statement,
		Function<Record, T> mapper) {

		var session = driver.asyncSession();
		return session.writeTransactionAsync(tx -> statement.fetchWith(tx, mapper))
			.thenCompose(result -> session.closeAsync().thenApply(i -> result))
			.toCompletableFuture();
	}
}
