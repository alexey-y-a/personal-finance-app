package ru.financeapp.cli;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.financeapp.core.FinanceService;
import ru.financeapp.core.User;
import ru.financeapp.core.UserService;
import ru.financeapp.core.Wallet;
import ru.financeapp.infra.Storage;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsoleAppTest {
    private ConsoleApp app;
    @Mock private UserService userService;
    @Mock private FinanceService financeService;
    @Mock private Storage storage;
    @Mock private User mockUser;
    @Mock private Wallet mockWallet;

    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        app = new ConsoleApp(userService, financeService, storage);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void handleRegister_Valid_PrintsSuccess() {
        CommandParser.Command cmd = new CommandParser.Command("register test pass");
        app.handleCommand(cmd);
        assertTrue(outContent.toString().contains("Registered successfully!"));
        verify(userService).register(eq("test"), eq("pass"));
    }

    @Test
    void handleLogin_Valid_PrintsLoggedIn() {
        CommandParser.Command cmd = new CommandParser.Command("login test pass");
        app.handleCommand(cmd);
        assertTrue(outContent.toString().contains("Logged in as test"));
        verify(userService).login(eq("test"), eq("pass"));
    }

    @Test
    void handleAddIncome_Valid_PrintsAdded() {
        when(userService.isLoggedIn()).thenReturn(true);
        CommandParser.Command cmd = new CommandParser.Command("add income salary 50000");
        app.handleCommand(cmd);
        assertTrue(outContent.toString().contains("Added income successfully!"));
        verify(financeService).addIncome(eq("salary"), eq(50000.0));
    }

    @Test
    void handleAddExpense_InvalidType_ThrowsWithUsage() {
        when(userService.isLoggedIn()).thenReturn(true);
        CommandParser.Command cmd = new CommandParser.Command("add invalid food 1000");
        app.handleCommand(cmd);
        assertTrue(outContent.toString().contains("Type must be 'income' or 'expense'"));
    }

    @Test
    void handleSetBudget_Valid_PrintsSet() {
        when(userService.isLoggedIn()).thenReturn(true);
        CommandParser.Command cmd = new CommandParser.Command("set budget food 4000");
        app.handleCommand(cmd);
        assertTrue(outContent.toString().contains("Budget set!"));
        verify(financeService).setBudget(eq("food"), eq(4000.0));
    }

    @Test
    void handleSetBudget_InvalidArgs_ThrowsWithUsage() {
        CommandParser.Command cmd = new CommandParser.Command("set invalid cat 1000");
        app.handleCommand(cmd);
        assertTrue(outContent.toString().contains("Usage: set budget <category> <amount>"));
    }

    @Test
    void handleStats_Valid_PrintsTable() {
        when(userService.isLoggedIn()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getWallet()).thenReturn(mockWallet);
        when(mockWallet.getBudgets()).thenReturn(new HashMap<>());
        when(mockWallet.getBudget(anyString())).thenReturn(0.0);
        when(financeService.getTotalIncome()).thenReturn(63000.0);
        when(financeService.getTotalExpenses()).thenReturn(8300.0);
        when(financeService.getIncomeByCategories(anyList())).thenReturn(Map.of("salary", 63000.0));
        when(financeService.getExpensesByCategories(anyList())).thenReturn(Map.of());
        when(financeService.getBudgetRemaining(anyString())).thenReturn(0.0);

        CommandParser.Command cmd = new CommandParser.Command("stats salary");
        app.handleCommand(cmd);

        String output = outContent.toString();
        assertTrue(
                output.contains("Category")
                        && output.contains("Income")
                        && output.contains("Expense"));
        assertTrue(output.contains("63000.0"));
        assertTrue(output.contains("8300.0"));
    }

    @Test
    void handleExport_Valid_PrintsExported() {
        when(userService.isLoggedIn()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getWallet()).thenReturn(mockWallet);
        when(mockWallet.getBudgets()).thenReturn(Map.of("food", 4000.0));
        when(mockWallet.getTransactions()).thenReturn(List.of());
        CommandParser.Command cmd = new CommandParser.Command("export test.json");
        app.handleCommand(cmd);
        assertTrue(outContent.toString().contains("Exported to test.json"));
    }

    @Test
    void handleUnknownCommand_PrintsErrorWithHelp() {
        CommandParser.Command cmd = new CommandParser.Command("unknown");
        app.handleCommand(cmd);
        assertTrue(outContent.toString().contains("Unknown command. Type 'help' for usage."));
    }

    @Test
    void handleError_Generic_PrintsErrorWithHelp() {
        when(userService.isLoggedIn()).thenReturn(true);
        doThrow(new RuntimeException("Test error"))
                .when(financeService)
                .addIncome(anyString(), anyDouble());
        CommandParser.Command cmd = new CommandParser.Command("add income salary 1000");
        app.handleCommand(cmd);
        String output = outContent.toString();
        assertTrue(output.contains("Error: Test error"));
        assertTrue(output.contains("Type 'help' for usage."));
    }

    @Test
    void exit_SetsRunningFalse_PrintsGoodbye() {
        app.exit();
        assertFalse(app.running);
        assertTrue(outContent.toString().contains("Goodbye!"));
    }

    @Test
    void saveAll_CallsSaveOnAllUsers() {
        List<User> mockUsers = Arrays.asList(mock(User.class), mock(User.class));
        when(storage.getAllUsers()).thenReturn(mockUsers);
        app.saveAll();
        verify(storage, times(2)).saveWallet(any(User.class));
    }

    @Test
    void printHelp_PrintsCommandsAndExamples() {
        app.printHelp();
        String output = outContent.toString();
        assertTrue(output.contains("Commands: register/login"));
        assertTrue(output.contains("Examples: 'add expense food 1000'"));
    }
}
