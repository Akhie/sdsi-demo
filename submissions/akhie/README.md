# Expense Splitter with Debt Simplification

A full-stack team-outing expense settler (Splitwise-lite). Built with **Vue 3** (frontend) and **Java Spring Boot** (backend).

## Features

| Requirement | Status |
|---|---|
| **M1** Three split types: Equal, Exact, Percentage | ✅ |
| **M2** Net balance per member | ✅ |
| **M3** Minimum settlement list ("A pays B ₹X") | ✅ |
| **M4** Single + batch import from JSON | ✅ |
| **M5** Input validation (percentages, amounts, payer) | ✅ |
| **Stretch** File upload, clear state, responsive UI | ✅ |

## Architecture

```
submissions/akhie/
├── backend/                    # Java Spring Boot API
│   ├── src/main/java/com/splitter/
│   │   ├── ExpenseSplitterApplication.java
│   │   ├── controller/ExpenseController.java
│   │   ├── dto/GroupState.java
│   │   ├── exception/
│   │   │   ├── ValidationException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── model/
│   │   │   ├── Expense.java, Settlement.java, SplitType.java
│   │   └── service/ExpenseService.java
│   ├── src/test/java/com/splitter/ExpenseServiceTest.java
│   └── pom.xml
├── frontend/                   # Vue 3 + Vite
│   ├── src/
│   │   ├── api/client.js
│   │   ├── router/index.js
│   │   ├── views/HomeView.vue
│   │   ├── App.vue, main.js
│   ├── index.html, vite.config.js
│   └── package.json
├── sample/expenses.json        # Sample data
├── README.md
└── PROMPTS.md
```

## Quick Start

### Backend (Java 17+ & Maven required)

```bash
cd backend
mvn spring-boot:run
```
Starts on **http://localhost:8080**

### Frontend (Node.js 16+ required)

```bash
cd frontend
npm install
npm run dev
```
Starts on **http://localhost:3000** (proxies `/api` to backend)

### Run Tests

```bash
cd backend
mvn test
```

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/expenses/single` | Add one expense |
| `POST` | `/api/expenses/batch` | Add multiple expenses |
| `GET` | `/api/state` | Get current state (balances + settlements) |
| `POST` | `/api/clear` | Reset all state |

### Expense JSON format

```json
{
  "description": "Team Dinner",
  "amount": 4500,
  "paidBy": "Alice",
  "splitType": "EQUAL",
  "splits": { "Alice": 0, "Bob": 0, "Charlie": 0 }
}
```

Split types:
- **EQUAL** — splits keys are participants (values ignored)
- **EXACT** — splits map member -> exact amount (must sum to total)
- **PERCENTAGE** — splits map member -> percentage (must sum to 100)

## Debt Simplification Algorithm

A greedy algorithm: sort creditors (positive balance) descending and debtors (negative balance) ascending. At each step, settle the smaller amount between the top creditor and top debtor. This produces **at most N-1 settlements** for N people.

## Edge Cases Handled

- Zero/negative amounts rejected
- Blank descriptions rejected
- Single-person groups (everyone settled)
- All amounts already balanced (no settlements needed)
- Exact sum matching with 0.001 tolerance for floating point
- Batch import stops on first invalid expense with clear error message
- Empty batch rejected