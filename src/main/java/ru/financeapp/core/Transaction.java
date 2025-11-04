package ru.financeapp.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.google.gson.annotations.Expose;

public class Transaction {
    public enum Type {
        INCOME,
        EXPENSE
    }

    @Expose private Type type;
    @Expose private String category;
    @Expose private double amount;
    @Expose private String date;

    public Transaction(Type type, String category, double amount) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public Type getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Double.compare(that.amount, amount) == 0
                && type == that.type
                && Objects.equals(category, that.category)
                && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, category, amount, date);
    }
}
