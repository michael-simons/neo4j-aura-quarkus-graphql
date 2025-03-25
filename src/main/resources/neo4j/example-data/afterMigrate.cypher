MERGE (p:Person {name: 'Michael Simons'})
MERGE (p) -[:WROTE] -> (:Book {title: 'arc42 by Example', type: 'S', state: 'R'})
MERGE (p) -[:WROTE] -> (:Book {title: 'Spring Boot 2 – Moderne Softwareentwicklung mit Spring 5', type: 'S', state: 'R'})
MERGE (p) -[:WROTE] -> (:Book {title: 'DuckDB in Action', type: 'S', state: 'R'});
