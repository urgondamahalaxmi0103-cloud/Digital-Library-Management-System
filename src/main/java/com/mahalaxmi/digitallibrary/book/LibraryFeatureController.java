
package com.mahalaxmi.digitallibrary.book;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LibraryFeatureController {

    private final JdbcTemplate jdbc;

    public LibraryFeatureController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 1. USER REGISTRATION
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> data) {
        try {
            String name = String.valueOf(data.get("fullName"));
            String email = String.valueOf(data.get("email"));
            String password = String.valueOf(data.get("password"));

            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                return ResponseEntity.badRequest().body("All fields are required.");
            }

            jdbc.update(
                "INSERT INTO SYSTEM.LIBRARY_USERS " +
                "(USER_ID,FULL_NAME,EMAIL,PASSWORD,ROLE) " +
                "VALUES (SYSTEM.LIBRARY_USERS_SEQ.NEXTVAL,?,?,?,'USER')",
                name, email, password
            );

            return ResponseEntity.ok("Registration successful!");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Registration failed: " + e.getMessage());
        }
    }

    // 2. ADMIN AND USER LOGIN
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> data) {
        try {
            String email = String.valueOf(data.get("email"));
            String password = String.valueOf(data.get("password"));

            List<Map<String, Object>> users = jdbc.queryForList(
                "SELECT USER_ID,FULL_NAME,EMAIL,ROLE " +
                "FROM SYSTEM.LIBRARY_USERS " +
                "WHERE EMAIL=? AND PASSWORD=?",
                email, password
            );

            if (users.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("Invalid email or password.");
            }

            return ResponseEntity.ok(users.get(0));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Login failed: " + e.getMessage());
        }
    }

    // 3. SEARCH BOOKS BY TITLE OR AUTHOR OR CATEGORY
    @GetMapping("/catalog")
    public List<Map<String, Object>> catalog(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {

        StringBuilder sql = new StringBuilder(
            "SELECT * FROM SYSTEM.BOOKS WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(
                " AND (LOWER(BOOKNAME) LIKE ? OR LOWER(AUTHOR) LIKE ?)"
            );

            String value = "%" + search.toLowerCase() + "%";
            params.add(value);
            params.add(value);
        }

        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(CATEGORY)=?");
            params.add(category.toLowerCase());
        }

        sql.append(" ORDER BY BOOKID");

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // 4. ADD BOOK WITH COMPLETE DETAILS
    @PostMapping("/admin/books")
    public ResponseEntity<?> addCompleteBook(
            @RequestBody Map<String, Object> data) {
        try {
            jdbc.update(
                "INSERT INTO SYSTEM.BOOKS " +
                "(BOOKID,BOOKNAME,PRICE,AUTHOR,ISBN,CATEGORY,QUANTITY) " +
                "VALUES (?,?,?,?,?,?,?)",
                Integer.parseInt(data.get("bookid").toString()),
                data.get("bookname"),
                Double.parseDouble(data.get("price").toString()),
                data.get("author"),
                data.get("isbn"),
                data.get("category"),
                Integer.parseInt(data.get("quantity").toString())
            );

            return ResponseEntity.ok("Book added successfully!");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Unable to add book: " + e.getMessage());
        }
    }

    // 5. EDIT BOOK
    @PutMapping("/books/{id}")
    public ResponseEntity<?> updateBook(
            @PathVariable int id,
            @RequestBody Map<String, Object> data) {
        try {
            int rows = jdbc.update(
                "UPDATE SYSTEM.BOOKS SET BOOKNAME=?, PRICE=?, " +
                "AUTHOR=?, ISBN=?, CATEGORY=?, QUANTITY=? " +
                "WHERE BOOKID=?",
                data.get("bookname"),
                Double.parseDouble(data.get("price").toString()),
                data.get("author"),
                data.get("isbn"),
                data.get("category"),
                Integer.parseInt(data.get("quantity").toString()),
                id
            );

            if (rows == 0) {
                return ResponseEntity.badRequest().body("Book not found.");
            }

            return ResponseEntity.ok("Book updated successfully!");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Update failed: " + e.getMessage());
        }
    }

    // 6. DELETE BOOK
    @DeleteMapping("/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable int id) {
        try {
            int rows = jdbc.update(
                "DELETE FROM SYSTEM.BOOKS WHERE BOOKID=?", id
            );

            if (rows == 0) {
                return ResponseEntity.badRequest().body("Book not found.");
            }

            return ResponseEntity.ok("Book deleted successfully!");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Cannot delete book: " + e.getMessage());
        }
    }

    // 7. ISSUE BOOK WITH 14-DAY DUE DATE
    @PostMapping("/library/issue")
    public ResponseEntity<?> issueBook(
            @RequestBody Map<String, Object> data) {
        try {
            int bookId = Integer.parseInt(data.get("bookId").toString());
            int userId = Integer.parseInt(data.get("userId").toString());

            LocalDate issueDate = LocalDate.now();
            LocalDate dueDate = issueDate.plusDays(14);

            int rows = jdbc.update(
                "UPDATE SYSTEM.BOOKS " +
                "SET QUANTITY=NVL(QUANTITY,1)-1 " +
                "WHERE BOOKID=? AND NVL(QUANTITY,1)>0",
                bookId
            );

            if (rows == 0) {
                return ResponseEntity.badRequest()
                    .body("Book unavailable or not found.");
            }

            jdbc.update(
                "INSERT INTO SYSTEM.ISSUEDBOOKS " +
                "(ISSUEID,BOOKID,ISSUEDDATE,DUEDATE,USER_ID," +
                "FINEAMOUNT,STATUS,FINE_PAID) " +
                "VALUES (SYSTEM.ISSUEDBOOKS_SEQ.NEXTVAL,?,?,?,?,0,'ISSUED','NO')",
                bookId,
                Date.valueOf(issueDate),
                Date.valueOf(dueDate),
                userId
            );

            return ResponseEntity.ok(
                "Book issued successfully! Due date: " + dueDate
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Issue failed: " + e.getMessage());
        }
    }

    // 8. RETURN BOOK AND CALCULATE FINE
    @PostMapping("/library/return/{issueId}")
    public ResponseEntity<?> returnBook(
            @PathVariable int issueId,
            @RequestBody Map<String, Object> data) {
        try {
            int userId = Integer.parseInt(data.get("userId").toString());

            Map<String, Object> issue = jdbc.queryForMap(
                "SELECT BOOKID,DUEDATE,STATUS FROM SYSTEM.ISSUEDBOOKS " +
                "WHERE ISSUEID=? AND USER_ID=?",
                issueId, userId
            );

            if (!"ISSUED".equalsIgnoreCase(
                    String.valueOf(issue.get("STATUS")))) {
                return ResponseEntity.badRequest()
                    .body("This book has already been returned.");
            }

            Date dueSqlDate = (Date) issue.get("DUEDATE");

            if (dueSqlDate == null) {
                return ResponseEntity.badRequest()
                    .body("Due date is missing.");
            }

            LocalDate dueDate = dueSqlDate.toLocalDate();
            LocalDate today = LocalDate.now();

            long overdueDays = Math.max(
                0, ChronoUnit.DAYS.between(dueDate, today)
            );

            double fine = overdueDays * 5.0;
            int bookId = ((Number) issue.get("BOOKID")).intValue();

            jdbc.update(
                "UPDATE SYSTEM.ISSUEDBOOKS SET RETURNDATE=?, " +
                "FINEAMOUNT=?, STATUS='RETURNED' WHERE ISSUEID=?",
                Date.valueOf(today), fine, issueId
            );

            jdbc.update(
                "UPDATE SYSTEM.BOOKS SET QUANTITY=NVL(QUANTITY,0)+1 " +
                "WHERE BOOKID=?",
                bookId
            );

            return ResponseEntity.ok(
                "Book returned successfully. Overdue days: " +
                overdueDays + ". Fine: Rs. " + fine
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Return failed: " + e.getMessage());
        }
    }

    // 9. ADMIN VIEW ISSUED BOOKS
    @GetMapping("/admin/issued-books")
    public List<Map<String, Object>> adminIssuedBooks() {
        return jdbc.queryForList(
            "SELECT I.ISSUEID,I.BOOKID,B.BOOKNAME," +
            "U.FULL_NAME,U.EMAIL,I.ISSUEDDATE,I.DUEDATE," +
            "I.RETURNDATE,I.FINEAMOUNT,I.STATUS,I.FINE_PAID " +
            "FROM SYSTEM.ISSUEDBOOKS I " +
            "JOIN SYSTEM.BOOKS B ON I.BOOKID=B.BOOKID " +
            "LEFT JOIN SYSTEM.LIBRARY_USERS U ON I.USER_ID=U.USER_ID " +
            "ORDER BY I.ISSUEID DESC"
        );
    }

    // 10. ADMIN VIEW MEMBERS
    // IMPORTANT: Only ONE /admin/members mapping exists.
    @GetMapping("/admin/members")
    public List<Map<String, Object>> members() {
        return jdbc.queryForList(
            "SELECT USER_ID,FULL_NAME,EMAIL,ROLE " +
            "FROM SYSTEM.LIBRARY_USERS ORDER BY USER_ID"
        );
    }

    // 11. MARK FINE AS PAID
    @PutMapping("/admin/fines/{issueId}/paid")
    public ResponseEntity<?> markFinePaid(
            @PathVariable int issueId) {

        int rows = jdbc.update(
            "UPDATE SYSTEM.ISSUEDBOOKS SET FINE_PAID='YES' " +
            "WHERE ISSUEID=? AND STATUS='RETURNED'",
            issueId
        );

        if (rows == 0) {
            return ResponseEntity.badRequest()
                .body("Returned issue not found.");
        }

        return ResponseEntity.ok("Fine marked as paid!");
    }

    // 12. ADVANCE BOOKING
    @PostMapping("/reservations")
    public ResponseEntity<?> reserveBook(
            @RequestBody Map<String, Object> data) {
        try {
            int userId = Integer.parseInt(data.get("userId").toString());
            int bookId = Integer.parseInt(data.get("bookId").toString());

            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SYSTEM.BOOKS WHERE BOOKID=?",
                Integer.class, bookId
            );

            if (count == null || count == 0) {
                return ResponseEntity.badRequest()
                    .body("Book not found.");
            }

            jdbc.update(
                "INSERT INTO SYSTEM.BOOK_RESERVATIONS " +
                "(RESERVATION_ID,USER_ID,BOOK_ID,RESERVATION_DATE,STATUS) " +
                "VALUES (SYSTEM.RESERVATIONS_SEQ.NEXTVAL,?,?,SYSDATE,'PENDING')",
                userId, bookId
            );

            return ResponseEntity.ok("Book reserved successfully!");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Reservation failed: " + e.getMessage());
        }
    }

    // 13. CONTACT / QUERY FORM
    @PostMapping("/contact")
    public ResponseEntity<?> contact(
            @RequestBody Map<String, Object> data) {
        try {
            jdbc.update(
                "INSERT INTO SYSTEM.CONTACT_MESSAGES " +
                "(MESSAGE_ID,NAME,EMAIL,MESSAGE,CREATED_DATE,STATUS) " +
                "VALUES (SYSTEM.CONTACT_MESSAGES_SEQ.NEXTVAL,?,?,?,SYSDATE,'NEW')",
                data.get("name"),
                data.get("email"),
                data.get("message")
            );

            return ResponseEntity.ok("Message submitted successfully!");

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Message failed: " + e.getMessage());
        }
    }

    // 14. ADMIN VIEW CONTACT MESSAGES
    @GetMapping("/admin/contact-messages")
    public List<Map<String, Object>> contactMessages() {
        return jdbc.queryForList(
            "SELECT * FROM SYSTEM.CONTACT_MESSAGES " +
            "ORDER BY MESSAGE_ID DESC"
        );
    }
}
