CREATE CONSTRAINT book_title_unique FOR (n:Book) REQUIRE n.title IS UNIQUE;
