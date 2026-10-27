package com.splitter.dto;

import com.splitter.model.Expense;
import com.splitter.model.Settlement;
import java.util.*;

public class GroupState {
    private List<Expense> expenses;
    private Map<String, Double> balances;     // member -> net balance (positive = owed money)
    private List<Settlement> settlements;
    private int expenseCount;
    private int settlementCount;

    public GroupState() {}

    public GroupState(List<Expense> expenses, Map<String, Double> balances, List<Settlement> settlements) {
        this.expenses = expenses;
        this.balances = balances;
        this.settlements = settlements;
        this.expenseCount = expenses.size();
        this.settlementCount = settlements.size();
    }

    public List<Expense> getExpenses() { return expenses; }
    public Map<String, Double> getBalances() { return balances; }
    public List<Settlement> getSettlements() { return settlements; }
    public int getExpenseCount() { return expenseCount; }
    public int getSettlementCount() { return settlementCount; }
}