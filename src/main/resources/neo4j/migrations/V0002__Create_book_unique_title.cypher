CREATE CONSTRAINT book_title_unique ON (n:Book) ASSERT n.title IS UNIQUE;
