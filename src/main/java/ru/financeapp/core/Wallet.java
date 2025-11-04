package ru.financeapp.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;

public class Wallet {
    @Expose private List<Transaction> transactions = new ArrayList<>();
    @Expose private Map<String, Double> budgets = new HashMap<>();

    public void addTransaction(Transaction t) {
        transactions.add(t);
    }

    public void setBudget(String category, double amount) {
        budgets.put(category, amount);
    }

    public Double getBudget(String category) {
        return budgets.getOrDefault(category, 0.0);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }

    public Map<String, Double> getBudgets() {
        return budgets;
    }

    public void setBudgets(Map<String, Double> budgets) {
        this.budgets = budgets != null ? budgets : new HashMap<>();
    }
}
