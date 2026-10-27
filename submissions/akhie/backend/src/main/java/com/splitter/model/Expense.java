package com.splitter.model;

import java.util.Map;

public class Expense {
    private String description;
    private double amount;
    private String paidBy;
    private SplitType splitType;
    private Map<String, Double> splits; // member -> amount/percentage

    public Expense() {}

    public Expense(String description, double amount, String paidBy, SplitType splitType, Map<String, Double> splits) {
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.splits = splits;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
    public Map<String, Double> getSplits() { return splits; }
    public void setSplits(Map<String, Double> splits) { this.splits = splits; }
}