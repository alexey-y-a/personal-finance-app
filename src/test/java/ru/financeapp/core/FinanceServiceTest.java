package ru.financeapp.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.financeapp.exceptions.InvalidInputException;
import ru.financeapp.infra.Storage;

class FinanceServiceTest {
    private FinanceService service;
    @Mock private UserService userService;
    @Mock private User mockUser;
    @Mock private Wallet mockWallet;
    @Mock private Storage mockStorage;
    @Mock private User mockToUser;
    @Mock private Wallet mockToWallet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(userService.getCurrentUser()).thenReturn(mockUser);
        when(mockUser.getWallet()).thenReturn(mockWallet);
        when(userService.getStorage()).thenReturn(mockStorage);
        when(mockStorage.findUser(anyString())).thenReturn(mockToUser);
        when(mockToUser.getWallet()).thenReturn(mockToWallet);
        when(mockWallet.getBudgets()).thenReturn(new HashMap<>());
        service = new FinanceService(userService);
    }

    @Test
    void addIncome_Valid_AddsTransaction() {
        assertDoesNotThrow(() -> service.addIncome("salary", 50000.0));
        verify(mockWallet).addTransaction(any(Transaction.class));
    }

    @Test
    void addIncome_NegativeAmount_Throws() {
        assertThrows(InvalidInputException.class, () -> service.addIncome("salary", -50000.0));
    }

    @Test
    void addIncome_EmptyCategory_Throws() {
        assertThrows(InvalidInputException.class, () -> service.addIncome("", 50000.0));
    }

    @Test
    void addExpense_Valid_AddsTransaction() {
        assertDoesNotThrow(() -> service.addExpense("food", 1000.0));
        verify(mockWallet).addTransaction(any(Transaction.class));
    }

    @Test
    void addExpense_NegativeAmount_Throws() {
        assertThrows(InvalidInputException.class, () -> service.addExpense("food", -1000.0));
    }

    @Test
    void setBudget_Valid_SetsBudget() {
        assertDoesNotThrow(() -> service.setBudget("food", 4000.0));
        verify(mockWallet).setBudget(eq("food"), eq(4000.0));
    }

    @Test
    void setBudget_NegativeAmount_Throws() {
        assertThrows(InvalidInputException.class, () -> service.setBudget("food", -4000.0));
    }

    @Test
    void getTotalIncome_WithData_ReturnsSum() {
        Transaction t1 = new Transaction(Transaction.Type.INCOME, "salary", 50000);
        Transaction t2 = new Transaction(Transaction.Type.INCOME, "bonus", 10000);
        when(mockWallet.getTransactions()).thenReturn(List.of(t1, t2));
        assertEquals(60000.0, service.getTotalIncome(), 0.01);
    }

    @Test
    void getTotalIncome_NoTransactions_ReturnsZero() {
        when(mockWallet.getTransactions()).thenReturn(List.of());
        assertEquals(0.0, service.getTotalIncome(), 0.01);
    }

    @Test
    void getTotalExpenses_WithData_ReturnsSum() {
        Transaction t1 = new Transaction(Transaction.Type.EXPENSE, "food", 1000);
        Transaction t2 = new Transaction(Transaction.Type.EXPENSE, "transport", 500);
        when(mockWallet.getTransactions()).thenReturn(List.of(t1, t2));
        assertEquals(1500.0, service.getTotalExpenses(), 0.01);
    }

    @Test
    void getExpensesByCategories_Filtered_ReturnsFiltered() {
        Transaction t1 = new Transaction(Transaction.Type.EXPENSE, "food", 1000);
        Transaction t2 = new Transaction(Transaction.Type.EXPENSE, "transport", 500);
        when(mockWallet.getTransactions()).thenReturn(List.of(t1, t2));
        Map<String, Double> result = service.getExpensesByCategories(List.of("food"));
        assertEquals(1, result.size());
        assertEquals(1000.0, result.get("food"), 0.01);
    }

    @Test
    void getExpensesByCategories_NotFound_Warns() {
        when(mockWallet.getTransactions()).thenReturn(List.of());
        Map<String, Double> result = service.getExpensesByCategories(List.of("unknown"));
        assertTrue(result.isEmpty());
    }

    @Test
    void getBudgetRemaining_WithSpend_ReturnsRemaining() {
        when(mockWallet.getBudget("food")).thenReturn(4000.0);
        Transaction t = new Transaction(Transaction.Type.EXPENSE, "food", 800);
        when(mockWallet.getTransactions()).thenReturn(List.of(t));
        assertEquals(3200.0, service.getBudgetRemaining("food"), 0.01);
    }

    @Test
    void isBudgetExceeded_Exceeded_ReturnsTrue() {
        when(mockWallet.getBudget("food")).thenReturn(1000.0);
        Transaction t = new Transaction(Transaction.Type.EXPENSE, "food", 1500);
        when(mockWallet.getTransactions()).thenReturn(List.of(t));
        assertTrue(service.isBudgetExceeded("food"));
    }

    @Test
    void transfer_Valid_Executes() {
        when(mockStorage.findUser(eq("to"))).thenReturn(mockToUser);
        when(mockToUser.getWallet()).thenReturn(mockToWallet);
        assertDoesNotThrow(() -> service.transfer("to", 1000.0));
        verify(mockWallet).addTransaction(any(Transaction.class));
        verify(mockToWallet).addTransaction(any(Transaction.class));
        verify(mockStorage).saveWallet(eq(mockToUser));
    }
}
