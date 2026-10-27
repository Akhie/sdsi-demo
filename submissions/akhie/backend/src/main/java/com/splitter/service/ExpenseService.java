package com.splitter.service;

import com.splitter.dto.GroupState;
import com.splitter.exception.ValidationException;
import com.splitter.model.Expense;
import com.splitter.model.Settlement;
import com.splitter.model.SplitType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final List<Expense> expenses = new ArrayList<>();
    private final Set<String> members = new LinkedHashSet<>();

    /**
     * Add an expense after full validation.
     * @throws ValidationException if any rule is violated
     */
    public GroupState addExpense(Expense expense) {
        validateExpense(expense);
        expenses.add(expense);
        members.add(expense.getPaidBy());
        if (expense.getSplits() != null) {
            members.addAll(expense.getSplits().keySet());
        }
        return computeState();
    }

    /**
     * Batch-import expenses. Stops on first invalid expense and returns the error.
     */
    public GroupState addExpenses(List<Expense> batch) {
        if (batch == null || batch.isEmpty()) {
            throw new ValidationException("Batch must contain at least one expense");
        }
        for (Expense e : batch) {
            validateExpense(e);
        }
        for (Expense e : batch) {
            expenses.add(e);
            members.add(e.getPaidBy());
            if (e.getSplits() != null) members.addAll(e.getSplits().keySet());
        }
        return computeState();
    }

    public GroupState getState() {
        return computeState();
    }

    public void clear() {
        expenses.clear();
        members.clear();
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    void validateExpense(Expense e) {
        if (e.getDescription() == null || e.getDescription().isBlank()) {
            throw new ValidationException("Description must not be blank");
        }
        if (e.getAmount() <= 0) {
            throw new ValidationException("Amount must be positive");
        }
        if (e.getPaidBy() == null || e.getPaidBy().isBlank()) {
            throw new ValidationException("PaidBy must not be blank");
        }
        if (e.getSplitType() == null) {
            throw new ValidationException("Split type is required (EQUAL, EXACT, PERCENTAGE)");
        }
        Map<String, Double> splits = e.getSplits();
        if (splits == null || splits.isEmpty()) {
            throw new ValidationException("Splits map must not be empty");
        }
        // Payer must be listed in the splits (i.e., be a group member)
        if (!splits.containsKey(e.getPaidBy())) {
            throw new ValidationException("Payer '" + e.getPaidBy() + "' must be listed in the splits");
        }

        switch (e.getSplitType()) {
            case PERCENTAGE -> {
                double sum = splits.values().stream().mapToDouble(Double::doubleValue).sum();
                if (Math.abs(sum - 100.0) > 0.001) {
                    throw new ValidationException("Percentages must sum to 100. Got: " + sum);
                }
                for (Map.Entry<String, Double> entry : splits.entrySet()) {
                    if (entry.getValue() < 0) {
                        throw new ValidationException("Percentage for " + entry.getKey() + " is negative");
                    }
                }
            }
            case EXACT -> {
                double sum = splits.values().stream().mapToDouble(Double::doubleValue).sum();
                if (Math.abs(sum - e.getAmount()) > 0.001) {
                    throw new ValidationException(
                        String.format("Exact amounts sum (%.2f) must equal total (%.2f)", sum, e.getAmount()));
                }
            }
            case EQUAL -> {
                // EQUAL: splits map keys are the participants; values are ignored
                if (splits.isEmpty()) {
                    throw new ValidationException("Equal split requires at least one participant");
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Balance computation
    // ---------------------------------------------------------------

    Map<String, Double> computeBalances() {
        Map<String, Double> bal = new LinkedHashMap<>();
        for (String m : members) bal.put(m, 0.0);

        for (Expense e : expenses) {
            Map<String, Double> shares = computeShares(e);
            String payer = e.getPaidBy();

            // The payer is credited the full amount
            bal.put(payer, bal.getOrDefault(payer, 0.0) + e.getAmount());

            // Each participant is debited their share
            for (Map.Entry<String, Double> entry : shares.entrySet()) {
                String member = entry.getKey();
                double share = entry.getValue();
                members.add(member);
                bal.putIfAbsent(member, 0.0);
                bal.put(member, bal.get(member) - share);
            }
        }

        // Remove entries that are ~0 to keep output clean
        bal.entrySet().removeIf(e -> Math.abs(e.getValue()) < 0.001);
        return bal;
    }

    Map<String, Double> computeShares(Expense e) {
        Map<String, Double> shares = new LinkedHashMap<>();
        switch (e.getSplitType()) {
            case EQUAL -> {
                int n = e.getSplits().size();
                double each = e.getAmount() / n;
                for (String member : e.getSplits().keySet()) {
                    shares.put(member, each);
                }
            }
            case EXACT -> shares.putAll(e.getSplits());
            case PERCENTAGE -> {
                for (Map.Entry<String, Double> entry : e.getSplits().entrySet()) {
                    shares.put(entry.getKey(), e.getAmount() * entry.getValue() / 100.0);
                }
            }
        }
        return shares;
    }

    // ---------------------------------------------------------------
    // Debt simplification — greedy algorithm
    // ---------------------------------------------------------------

    List<Settlement> simplifyDebts(Map<String, Double> balances) {
        // Split into creditors (positive) and debtors (negative)
        List<Map.Entry<String, Double>> creditors = balances.entrySet().stream()
            .filter(e -> e.getValue() > 0.001)
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toCollection(ArrayList::new));

        List<Map.Entry<String, Double>> debtors = balances.entrySet().stream()
            .filter(e -> e.getValue() < -0.001)
            .sorted(Map.Entry.comparingByValue()) // most negative first
            .collect(Collectors.toCollection(ArrayList::new));

        List<Settlement> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < creditors.size() && j < debtors.size()) {
            double credit = creditors.get(i).getValue();
            double debt = -debtors.get(j).getValue();
            double settle = Math.min(credit, debt);

            result.add(new Settlement(
                debtors.get(j).getKey(),
                creditors.get(i).getKey(),
                Math.round(settle * 100.0) / 100.0
            ));

            creditors.get(i).setValue(credit - settle);
            debtors.get(j).setValue(-(debt - settle));

            if (Math.abs(creditors.get(i).getValue()) < 0.001) i++;
            if (Math.abs(debtors.get(j).getValue()) < 0.001) j++;
        }
        return result;
    }

    // ---------------------------------------------------------------
    // State snapshot
    // ---------------------------------------------------------------

    private GroupState computeState() {
        Map<String, Double> balances = computeBalances();

        // Round to 2 decimal places
        for (Map.Entry<String, Double> e : balances.entrySet()) {
            e.setValue(Math.round(e.getValue() * 100.0) / 100.0);
        }

        List<Settlement> settlements = simplifyDebts(balances);

        return new GroupState(
            new ArrayList<>(expenses),
            balances,
            settlements
        );
    }
}