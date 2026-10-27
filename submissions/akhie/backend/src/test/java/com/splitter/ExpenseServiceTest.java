package com.splitter;

import com.splitter.exception.ValidationException;
import com.splitter.model.Expense;
import com.splitter.model.Settlement;
import com.splitter.model.SplitType;
import com.splitter.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseServiceTest {

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService();
    }

    // --- Equal split ---
    @Test
    void equalSplitShouldWork() {
        var e = new Expense("Dinner", 300, "Alice", SplitType.EQUAL,
            Map.of("Alice", 0.0, "Bob", 0.0, "Charlie", 0.0));
        var state = service.addExpense(e);
        assertEquals(0.0, state.getBalances().get("Alice"), 0.01);
        assertEquals(-100.0, state.getBalances().get("Bob"), 0.01);
        assertEquals(-100.0, state.getBalances().get("Charlie"), 0.01);
    }

    // --- Exact split ---
    @Test
    void exactSplitShouldWork() {
        var e = new Expense("Groceries", 500, "Bob", SplitType.EXACT,
            Map.of("Alice", 300.0, "Bob", 200.0));
        var state = service.addExpense(e);
        assertEquals(-300.0, state.getBalances().get("Alice"), 0.01);
        assertEquals(300.0, state.getBalances().get("Bob"), 0.01); // paid 500, owes 200
    }

    // --- Percentage split ---
    @Test
    void percentageSplitShouldWork() {
        var e = new Expense("Trip", 1000, "Charlie", SplitType.PERCENTAGE,
            Map.of("Alice", 50.0, "Bob", 30.0, "Charlie", 20.0));
        var state = service.addExpense(e);
        assertEquals(-500.0, state.getBalances().get("Alice"), 0.01);
        assertEquals(-300.0, state.getBalances().get("Bob"), 0.01);
        assertEquals(800.0, state.getBalances().get("Charlie"), 0.01);
    }

    // --- Validation: percentages not summing to 100 ---
    @Test
    void percentageMustSumTo100() {
        var e = new Expense("Bad", 100, "A", SplitType.PERCENTAGE,
            Map.of("A", 40.0, "B", 30.0));
        assertThrows(ValidationException.class, () -> service.addExpense(e));
    }

    // --- Validation: exact amounts not summing to total ---
    @Test
    void exactMustSumToTotal() {
        var e = new Expense("Bad", 100, "A", SplitType.EXACT,
            Map.of("A", 40.0, "B", 30.0));
        assertThrows(ValidationException.class, () -> service.addExpense(e));
    }

    // --- Validation: empty description ---
    @Test
    void emptyDescriptionShouldFail() {
        var e = new Expense("   ", 100, "A", SplitType.EQUAL,
            Map.of("A", 0.0));
        assertThrows(ValidationException.class, () -> service.addExpense(e));
    }

    // --- Validation: zero/negative amount ---
    @Test
    void zeroAmountShouldFail() {
        var e = new Expense("Bad", 0, "A", SplitType.EQUAL,
            Map.of("A", 0.0));
        assertThrows(ValidationException.class, () -> service.addExpense(e));
    }

    // --- Debt simplification ---
    @Test
    void simplifyDebtsShouldMinimizeSettlements() {
        service.addExpense(new Expense("A", 100, "Alice", SplitType.EQUAL,
            Map.of("Alice", 0.0, "Bob", 0.0)));
        service.addExpense(new Expense("B", 200, "Bob", SplitType.EQUAL,
            Map.of("Alice", 0.0, "Bob", 0.0)));

        var state = service.getState();
        var settlements = state.getSettlements();
        assertEquals(1, settlements.size());
        assertEquals("Alice", settlements.get(0).getFrom());
        assertEquals("Bob", settlements.get(0).getTo());
    }

    // --- Batch ---
    @Test
    void batchShouldProcessMultipleExpenses() {
        var batch = List.of(
            new Expense("Lunch", 400, "Alice", SplitType.EQUAL,
                Map.of("Alice", 0.0, "Bob", 0.0)),
            new Expense("Cab", 200, "Bob", SplitType.EXACT,
                Map.of("Alice", 200.0))
        );
        var state = service.addExpenses(batch);
        assertEquals(0.0, state.getBalances().get("Alice"), 0.01);
        assertEquals(0.0, state.getBalances().get("Bob"), 0.01);
    }

    // --- Validation: payer must be a group member (M5) ---
    @Test
    void payerMustBeInSplits() {
        var e = new Expense("Ghost", 100, "Zoe", SplitType.EQUAL,
            Map.of("Alice", 0.0, "Bob", 0.0));
        assertThrows(ValidationException.class, () -> service.addExpense(e));
    }

    // --- Validation: payer not in batch splits ---
    @Test
    void payerMustBeInSplitsBatch() {
        var batch = List.of(
            new Expense("Ghost batch", 200, "Zoe", SplitType.EXACT,
                Map.of("Alice", 100.0, "Bob", 100.0))
        );
        var ex = assertThrows(ValidationException.class, () -> service.addExpenses(batch));
        assertTrue(ex.getMessage().contains("Payer"));
    }

    // --- Edge: single person ---
    @Test
    void singlePersonShouldWork() {
        var e = new Expense("Coffee", 50, "Alice", SplitType.EQUAL,
            Map.of("Alice", 0.0));
        var state = service.addExpense(e);
        assertEquals(0.0, state.getBalances().get("Alice"), 0.01);
    }
}