# neo4j-aura-quarkus-graphql project

This project uses 

* [Neo4j Aura](https://www.google.com/search?client=safari&rls=en&q=neo4j+aura&ie=UTF-8&oe=UTF-8)
* The [Neo4j Java Driver](https://github.com/neo4j/neo4j-java-driver) with [Quarkus](https://quarkus.io)
* and the [Neo4j-Cypher-DSL](https://github.com/neo4j-contrib/cypher-dsl)

to build a rather dynamic GraphQL application that provides access to the "Neo4j Movie Graph", 
that comes bundled with Neo4j as well as a list of books from my [good reads project](https://github.com/michael-simons/goodreads).

The application is running as a natively compiled application on
https://neo4j-aura-quarkus-graphql.herokuapp.com. The image has been build with:

```
./mvnw clean package\
  -Pnative\
  -Dquarkus.native.container-build=true\
  -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-native-image:22.2-java17\
  -Dquarkus.docker.dockerfile-native-path=./src/main/docker/Dockerfile.native-distroless\
  -Dquarkus.container-image.build=true\
  -Dquarkus.container-image.group=registry.heroku.com/neo4j-aura-quarkus-graphql\
  -Dquarkus.container-image.name=web\
  -Dquarkus.container-image.tag=latest
```

We publish things as recommended in the [guide](https://quarkus.io/guides/deploying-to-heroku):

```
docker push registry.heroku.com/neo4j-aura-quarkus-graphql/web
heroku container:release web --app neo4j-aura-quarkus-graphql
```

## Run Neo4j

There are several ways to run a Neo4j instance.
Either download an instance on https://neo4j.com/downloads (Desktop or stand-alone) or use the official Docker image.

### Just use Quarkus dev-services

If you have Docker installed and don't change the default configuration, Quarkus will bring up a Neo4j instance via [Testcontainers](https://www.testcontainers.org) for you.

### Using the Docker image

To quickly get a temporary instance with the configured credentials up and running, just enter

```
docker run -p7474:7474 -p7687:7687 --env NEO4J_AUTH=neo4j/secret neo4j:4.3
```

in you terminal. If you want to read up which folders can be mapped and other environment variables, 
check out https://neo4j.com/docs/operations-manual/current/docker/.

### Create data

The easiest way to populate the database with the example dataset is to open the Neo4j browser (default: http://localhost:7474)
and start the movie example by typing `:play movies`.
On the second page of the interactive manual is the creation query.
Execute it, and the movie dataset will get created.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/neo4j-aura-quarkus-graphql-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.html.
