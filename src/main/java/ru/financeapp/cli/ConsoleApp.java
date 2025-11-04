package ru.financeapp.cli;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.financeapp.core.FinanceService;
import ru.financeapp.core.Transaction;
import ru.financeapp.core.UserService;
import ru.financeapp.core.Wallet;
import ru.financeapp.exceptions.InvalidInputException;
import ru.financeapp.infra.FileJsonStorage;
import ru.financeapp.infra.Storage;

public class ConsoleApp {
    protected final UserService userService;
    protected final FinanceService financeService;
    protected final Storage storage;
    boolean running = true;

    public ConsoleApp() {
        this.storage = new FileJsonStorage();
        this.userService = new UserService(storage);
        this.financeService = new FinanceService(userService);
    }

    protected ConsoleApp(UserService userService, FinanceService financeService, Storage storage) {
        this.userService = userService;
        this.financeService = financeService;
        this.storage = storage;
    }

    public void run() {
        System.out.println("=== Personal Finance App: Управление финансами ===");
        printHelp();
        Scanner scanner = new Scanner(System.in);
        while (running) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }
            try {
                handleCommand(new CommandParser.Command(input));
            } catch (InvalidInputException e) {
                System.out.println("Error: " + e.getMessage() + ". Type 'help' for usage.");
            }
        }
        saveAll();
    }

    void handleCommand(CommandParser.Command cmd) {
        try {
            switch (cmd.action) {
                case "register" -> register(cmd.args);
                case "login" -> login(cmd.args);
                case "add" -> add(cmd.args);
                case "set" -> {
                    if (cmd.args.isEmpty() || !cmd.args.get(0).equals("budget")) {
                        throw new InvalidInputException("Usage: set budget <category> <amount>");
                    }
                    setBudget(cmd.args.subList(1, cmd.args.size()));
                }
                case "edit" -> {
                    if (cmd.args.isEmpty() || !cmd.args.get(0).equals("budget")) {
                        throw new InvalidInputException("Usage: edit budget <category> <amount>");
                    }
                    setBudget(cmd.args.subList(1, cmd.args.size()));
                    System.out.println("Budget updated!");
                }
                case "stats" -> stats(cmd.args);
                case "transfer" -> transfer(cmd.args);
                case "export" -> export(cmd.args);
                case "import" -> imprt(cmd.args);
                case "list" -> listCategories();
                case "logout" -> logout();
                case "help" -> printHelp();
                case "exit" -> exit();
                default -> System.out.println("Unknown command. Type 'help' for usage.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage() + ". Type 'help' for usage.");
        }
    }

    private void register(List<String> args) {
        if (args.size() != 2)
            throw new InvalidInputException("Usage: register <username> <password>");
        userService.register(args.get(0), args.get(1));
        System.out.println("Registered successfully!");
    }

    private void login(List<String> args) {
        if (args.size() != 2) throw new InvalidInputException("Usage: login <username> <password>");
        userService.login(args.get(0), args.get(1));
        System.out.println("Logged in as " + args.get(0));
    }

    private void add(List<String> args) {
        if (!userService.isLoggedIn())
            throw new InvalidInputException("Login first. Usage: login <username> <password>");
        if (args.size() != 3)
            throw new InvalidInputException("Usage: add <income|expense> <category> <amount>");
        String type = args.get(0);
        String cat = args.get(1);
        double amt = Double.parseDouble(args.get(2));
        if ("income".equals(type)) {
            financeService.addIncome(cat, amt);
        } else if ("expense".equals(type)) {
            financeService.addExpense(cat, amt);
        } else {
            throw new InvalidInputException(
                    "Type must be 'income' or 'expense'. Usage: add <income|expense> <category> <amount>");
        }
        System.out.println("Added " + type + " successfully!");
    }

    private void setBudget(List<String> args) {
        if (!userService.isLoggedIn())
            throw new InvalidInputException("Login first. Usage: login <username> <password>");
        if (args.size() != 2)
            throw new InvalidInputException("Usage: set budget <category> <amount>");
        double amt = Double.parseDouble(args.get(1));
        financeService.setBudget(args.get(0), amt);
        System.out.println("Budget set!");
    }

    private void stats(List<String> args) {
        if (!userService.isLoggedIn())
            throw new InvalidInputException("Login first. Usage: login <username> <password>");
        List<String> cats;
        if (!args.isEmpty() && "categories".equals(args.get(0))) {
            cats = args.subList(1, args.size());
        } else {
            cats = args;
        }
        double income = financeService.getTotalIncome();
        double expense = financeService.getTotalExpenses();
        System.out.printf(Locale.US, "Общий доход: %.1f%n", income);
        System.out.printf(Locale.US, "Общие расходы: %.1f%n", expense);
        printTable(
                financeService.getIncomeByCategories(cats),
                financeService.getExpensesByCategories(cats));
        if (financeService.getTotalExpenses() > financeService.getTotalIncome()) {
            System.out.println("Alert: Expenses exceed income!");
        }
    }

    private void transfer(List<String> args) {
        if (!userService.isLoggedIn())
            throw new InvalidInputException("Login first. Usage: login <username> <password>");
        if (args.size() != 2) throw new InvalidInputException("Usage: transfer <to_user> <amount>");
        double amt = Double.parseDouble(args.get(1));
        financeService.transfer(args.get(0), amt);
        System.out.println("Transfer sent!");
    }

    private void export(List<String> args) {
        if (!userService.isLoggedIn())
            throw new InvalidInputException("Login first. Usage: login <username> <password>");
        if (args.isEmpty()) throw new InvalidInputException("Usage: export <file.json>");
        String file = args.get(0);
        Map<String, Object> data =
                Map.of(
                        "transactions", userService.getCurrentUser().getWallet().getTransactions(),
                        "budgets", userService.getCurrentUser().getWallet().getBudgets());
        try (FileWriter w = new FileWriter(file)) {
            new Gson().toJson(data, w);
            System.out.println("Exported to " + file);
        } catch (IOException e) {
            throw new RuntimeException("Export failed", e);
        }
    }

    private void imprt(List<String> args) {
        if (!userService.isLoggedIn())
            throw new InvalidInputException("Login first. Usage: login <username> <password>");
        if (args.isEmpty()) throw new InvalidInputException("Usage: import <file.json>");
        String file = args.get(0);
        Gson gson = new Gson();
        try (FileReader r = new FileReader(file)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = gson.fromJson(r, Map.class);
            @SuppressWarnings("unchecked")
            List<Transaction> trans =
                    gson.fromJson(
                            gson.toJsonTree(data.get("transactions")),
                            new TypeToken<List<Transaction>>() {}.getType());
            @SuppressWarnings("unchecked")
            Map<String, Double> buds =
                    gson.fromJson(
                            gson.toJsonTree(data.get("budgets")),
                            new TypeToken<Map<String, Double>>() {}.getType());
            Wallet w = userService.getCurrentUser().getWallet();
            w.setTransactions(trans != null ? trans : List.of());
            w.setBudgets(buds != null ? buds : new HashMap<>());
            System.out.println("Imported successfully!");
        } catch (Exception e) {
            throw new InvalidInputException("Import failed: " + e.getMessage());
        }
    }

    private void listCategories() {
        if (!userService.isLoggedIn())
            throw new InvalidInputException("Login first. Usage: login <username> <password>");
        System.out.println(
                "Categories: " + userService.getCurrentUser().getWallet().getBudgets().keySet());
    }

    private void logout() {
        userService.logout();
        System.out.println("Logged out.");
    }

    void exit() {
        running = false;
        System.out.println("Goodbye!");
    }

    void printHelp() {
        System.out.println(
                "Commands: register/login <user> <pass>, "
                        + "add <income|expense> <cat> <amt>, "
                        + "set/edit budget <cat> <amt>, "
                        + "stats [categories <cats>], transfer <to> <amt>, "
                        + "export/import <file>, list categories, "
                        + "logout, help, exit.");
        System.out.println(
                "Examples: 'add expense food 1000', "
                        + "'stats categories food transport', 'set budget еда 4000'.");
    }

    private void printTable(Map<String, Double> incomes, Map<String, Double> expenses) {
        System.out.println("| Category | Income | Expense | Budget | Remaining |");
        System.out.println("|----------|--------|---------|--------|-----------|");
        Set<String> allCats = new HashSet<>();
        allCats.addAll(incomes.keySet());
        allCats.addAll(expenses.keySet());
        allCats.forEach(
                cat -> {
                    double inc = incomes.getOrDefault(cat, 0.0);
                    double exp = expenses.getOrDefault(cat, 0.0);
                    double bud = userService.getCurrentUser().getWallet().getBudget(cat);
                    double rem = financeService.getBudgetRemaining(cat);
                    String budStr = (bud > 0) ? String.format(Locale.US, "%.1f", bud) : "N/A";
                    String remStr = (bud > 0) ? String.format(Locale.US, "%.1f", rem) : "N/A";
                    System.out.printf(
                            Locale.US,
                            "| %-8s | %-6.1f | %-7.1f | %-6s | %-9s |%n",
                            cat,
                            inc,
                            exp,
                            budStr,
                            remStr);
                });
    }

    void saveAll() {
        storage.getAllUsers().forEach(storage::saveWallet);
    }
}
