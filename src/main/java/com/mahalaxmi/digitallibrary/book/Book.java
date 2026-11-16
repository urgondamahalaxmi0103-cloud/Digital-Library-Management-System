package com.mahalaxmi.digitallibrary.book;

public class Book {

    private int bookid;
    private String bookname;
    private double price;

    public Book() {
    }

    public Book(int bookid, String bookname, double price) {
        this.bookid = bookid;
        this.bookname = bookname;
        this.price = price;
    }

    public int getBookid() {
        return bookid;
    }

    public void setBookid(int bookid) {
        this.bookid = bookid;
    }

    public String getBookname() {
        return bookname;
    }

    public void setBookname(String bookname) {
        this.bookname = bookname;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}