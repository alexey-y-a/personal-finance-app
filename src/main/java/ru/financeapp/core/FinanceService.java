package ru.financeapp.core;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.financeapp.exceptions.InvalidInputException;
import ru.financeapp.exceptions.UserNotFoundException;
import ru.financeapp.infra.Storage;

public class FinanceService {
    private final UserService userService;

    public FinanceService(UserService userService) {
        this.userService = userService;
    }

    public void addIncome(String category, double amount) {
        validateAmount(amount);
        validateCategory(category);
        Transaction t = new Transaction(Transaction.Type.INCOME, category, amount);
        userService.getCurrentUser().getWallet().addTransaction(t);
        checkNotifications(t);
    }

    public void addExpense(String category, double amount) {
        validateAmount(amount);
        validateCategory(category);
        Transaction t = new Transaction(Transaction.Type.EXPENSE, category, amount);
        userService.getCurrentUser().getWallet().addTransaction(t);
        checkNotifications(t);
    }

    public void setBudget(String category, double amount) {
        validateAmount(amount);
        if (amount < 0) throw new InvalidInputException("Budget cannot be negative");
        userService.getCurrentUser().getWallet().setBudget(category, amount);
    }

    public double getTotalIncome() {
        return userService.getCurrentUser().getWallet().getTransactions().stream()
                .filter(t -> t.getType() == Transaction.Type.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public double getTotalExpenses() {
        return userService.getCurrentUser().getWallet().getTransactions().stream()
                .filter(t -> t.getType() == Transaction.Type.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Map<String, Double> getIncomeByCategories(List<String> categories) {
        return filterByCategories(Transaction.Type.INCOME, categories);
    }

    public Map<String, Double> getExpensesByCategories(List<String> categories) {
        return filterByCategories(Transaction.Type.EXPENSE, categories);
    }

    public double getBudgetRemaining(String category) {
        double budget = userService.getCurrentUser().getWallet().getBudget(category);
        double spent = getExpensesByCategories(List.of(category)).getOrDefault(category, 0.0);
        return budget - spent;
    }

    public boolean isBudgetExceeded(String category) {
        return getBudgetRemaining(category) < 0;
    }

    public void transfer(String toUsername, double amount) {
        User current = userService.getCurrentUser();
        Storage st = userService.getStorage();
        User toUser = st.findUser(toUsername);
        if (toUser == null) throw new UserNotFoundException("Recipient not found: " + toUsername);
        addExpense("Transfer", amount);
        toUser.getWallet()
                .addTransaction(new Transaction(Transaction.Type.INCOME, "Transfer", amount));
        st.saveWallet(toUser);
    }

    private Map<String, Double> filterByCategories(Transaction.Type type, List<String> categories) {
        if (categories.isEmpty()) {
            return getAllByType(type);
        }
        Map<String, Double> result = getAllByType(type);
        Map<String, Double> budgets = userService.getCurrentUser().getWallet().getBudgets();
        categories.forEach(
                cat -> {
                    if (!result.containsKey(cat) && !budgets.containsKey(cat)) {
                        System.out.println(
                                "Warning: Category '"
                                        + cat
                                        + "' not found in transactions or budgets.");
                    }
                });
        return result.entrySet().stream()
                .filter(e -> categories.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, Double> getAllByType(Transaction.Type type) {
        return userService.getCurrentUser().getWallet().getTransactions().stream()
                .filter(t -> t.getType() == type)
                .collect(
                        Collectors.groupingBy(
                                Transaction::getCategory,
                                Collectors.summingDouble(Transaction::getAmount)));
    }

    private void validateAmount(double amount) {
        if (amount <= 0) throw new InvalidInputException("Amount must be positive");
    }

    private void validateCategory(String category) {
        if (category.isEmpty()) throw new InvalidInputException("Category cannot be empty");
    }

    private void checkNotifications(Transaction t) {
        if (t.getType() == Transaction.Type.EXPENSE) {
            double remaining = getBudgetRemaining(t.getCategory());
            double budget = userService.getCurrentUser().getWallet().getBudget(t.getCategory());
            if (budget > 0 && remaining < 0.8 * budget) {
                System.out.println("Warning: 80% of budget for '" + t.getCategory() + "' used!");
            }
            if (remaining < 0) {
                System.out.println("Alert: Budget exceeded for '" + t.getCategory() + "'!");
            }
        }
        double balance = getTotalIncome() - getTotalExpenses();
        if (balance == 0) {
            System.out.println("Warning: Balance is zero!");
        }
        if (getTotalExpenses() > getTotalIncome()) {
            System.out.println("Alert: Expenses exceed income!");
        }
    }
}
