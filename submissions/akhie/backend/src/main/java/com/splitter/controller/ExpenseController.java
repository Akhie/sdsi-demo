package com.splitter.controller;

import com.splitter.dto.GroupState;
import com.splitter.model.Expense;
import com.splitter.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @PostMapping("/expenses/single")
    public ResponseEntity<GroupState> addExpense(@Valid @RequestBody Expense expense) {
        return ResponseEntity.ok(service.addExpense(expense));
    }

    @PostMapping("/expenses/batch")
    public ResponseEntity<GroupState> addExpenses(@RequestBody List<Expense> batch) {
        return ResponseEntity.ok(service.addExpenses(batch));
    }

    @GetMapping("/state")
    public ResponseEntity<GroupState> getState() {
        return ResponseEntity.ok(service.getState());
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clear() {
        service.clear();
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}