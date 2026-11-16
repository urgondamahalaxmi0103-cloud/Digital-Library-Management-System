package com.mahalaxmi.digitallibrary.book;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final JdbcTemplate jdbcTemplate;

    public BookController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Display all books
    @GetMapping
    public List<Book> getBooks() {

        String sql = "SELECT BOOKID, BOOKNAME, PRICE " +
                     "FROM SYSTEM.BOOKS ORDER BY BOOKID";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
            new Book(
                rs.getInt("BOOKID"),
                rs.getString("BOOKNAME"),
                rs.getDouble("PRICE")
            )
        );
    }

    // Add a new book
    @PostMapping
    public Book addBook(@RequestBody Book book) {

        String sql = "INSERT INTO SYSTEM.BOOKS " +
                     "(BOOKID, BOOKNAME, PRICE) VALUES (?, ?, ?)";

        jdbcTemplate.update(
            sql,
            book.getBookid(),
            book.getBookname(),
            book.getPrice()
        );

        return book;
    }

    // Search book by name
    @GetMapping("/search")
    public List<Book> searchBook(@RequestParam String name) {

        String sql = "SELECT BOOKID, BOOKNAME, PRICE " +
                     "FROM SYSTEM.BOOKS " +
                     "WHERE UPPER(BOOKNAME) LIKE UPPER(?)";

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new Book(
                rs.getInt("BOOKID"),
                rs.getString("BOOKNAME"),
                rs.getDouble("PRICE")
            ),
            "%" + name + "%"
        );
    }
}