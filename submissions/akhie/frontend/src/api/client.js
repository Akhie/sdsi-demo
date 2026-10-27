import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

export default {
  addExpense(expense) {
    return api.post('/expenses/single', expense)
  },
  addBatch(expenses) {
    return api.post('/expenses/batch', expenses)
  },
  getState() {
    return api.get('/state')
  },
  clear() {
    return api.post('/clear')
  }
}