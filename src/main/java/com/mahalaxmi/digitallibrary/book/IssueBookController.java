
package com.mahalaxmi.digitallibrary.book;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IssueBookController {

    private final JdbcTemplate jdbcTemplate;

    public IssueBookController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/api/issued-books")
    public ResponseEntity<?> issueBook(
            @RequestBody IssueBookRequest request) {

        try {
            int bookId = request.getBookid();

            if (request.getIssuedDate() == null ||
                request.getIssuedDate().isBlank()) {
                return ResponseEntity.badRequest()
                    .body("Please select an issue date.");
            }

            LocalDate issueDate =
                LocalDate.parse(request.getIssuedDate());

            if (issueDate.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest()
                    .body("Issue date cannot be in the future.");
            }

            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SYSTEM.BOOKS WHERE BOOKID = ?",
                Integer.class,
                bookId
            );

            if (count == null || count == 0) {
                return ResponseEntity.badRequest()
                    .body("Book not found.");
            }

            String sql =
                "INSERT INTO SYSTEM.ISSUEDBOOKS " +
                "(ISSUEID, BOOKID, ISSUEDDATE) " +
                "VALUES (SYSTEM.ISSUEDBOOKS_SEQ.NEXTVAL, ?, ?)";

            jdbcTemplate.update(
                sql,
                bookId,
                Date.valueOf(issueDate)
            );

            return ResponseEntity.ok(
                "Book issued successfully!"
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body("Unable to issue book: " + e.getMessage());
        }
    }

    @GetMapping("/api/issued-books")
    public List<Map<String, Object>> getIssuedBooks() {

        String sql =
            "SELECT I.ISSUEID, I.BOOKID, " +
            "B.BOOKNAME, I.ISSUEDDATE " +
            "FROM SYSTEM.ISSUEDBOOKS I " +
            "JOIN SYSTEM.BOOKS B " +
            "ON I.BOOKID = B.BOOKID " +
            "ORDER BY I.ISSUEID DESC";

        return jdbcTemplate.queryForList(sql);
    }
}
