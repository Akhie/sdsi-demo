# PROMPTS.md — AI Collaboration Effectiveness

## Summary of Prompts Used

### 1. Initial Project Scaffolding
```
Build an Expense Splitter with Debt Simplification in Vue + Java.
Create everything inside submissions/akhie/.
```

**Response**: Claude created the full project structure: Spring Boot backend with models (Expense, Settlement, SplitType), service layer with validation and debt simplification, REST controller with CORS, and Vue 3 frontend with Vite, router, API client, and a single-page UI.

### 2. Design Decisions Requested
```
What split types? → EQUAL, EXACT, PERCENTAGE
How to simplify debts? → Greedy creditor-debtor matching
How to validate? → Server-side with specific error messages
```

**Response**: Claude chose:
- Enum-based split types for type safety
- Greedy algorithm for debt simplification (O(n log n))
- Spring's `@RestControllerAdvice` for consistent error handling
- In-memory state (no database — keeps it simple for a demo)

### 3. UI Layout Request
```
Create a clean single-page UI with:
- Expense form (description, amount, payer, split type, members)
- Import section (sample data button + file upload)
- Dashboard with balances card + settlements card
- Expense history
```

**Response**: Built a responsive grid layout with cards, color-coded balances (green/red), and clean typography using CSS custom properties.

### 4. Testing Strategy
```
What edge cases should we test?
```

**Response**: Created 8 unit tests covering:
- Normal flows: equal, exact, percentage splits
- Validation: percentages ≠ 100, exact amounts ≠ total, blank description, zero amount
- Business logic: debt simplification produces minimal settlements
- Integration: batch processing
- Edge case: single-person group

### 5. Sample Data
```
Create sample expenses.json with 4 expenses covering all 3 split types
```

**Response**: Generated sample data with Team Dinner (equal), Cab (exact), Projector Rental (percentage), Coffee Run (equal) totaling ₹8400 across 3 members.

## Why These Prompts Were Effective

1. **Specific constraints upfront** — Mentioning "Vue + Java" and "inside submissions/akhie/" eliminated ambiguity immediately.
2. **Iterative refinement** — Each follow-up addressed a specific layer (backend, frontend, tests, docs) rather than trying to specify everything at once.
3. **Letting the AI choose defaults** — For algorithm choice (greedy vs. linear programming), we asked for a recommendation rather than prescribing one. Claude picked the pragmatic O(n log n) greedy approach since exact minimization is NP-hard.
4. **Providing concrete examples** — When asking for sample data, we specified "4 expenses covering all 3 split types" which gave clear scope.

## Lessons Learned

- Starting with the backend model layer first (entities → service → controller) gave a solid foundation before the UI
- The greedy debt simplification algorithm produces optimal or near-optimal results without the complexity of min-cost flow
- Using Vite's dev proxy eliminated CORS issues entirely in development
- In-memory state is sufficient for this scope; a real app would add persistence