<script setup>
import { ref, reactive, computed } from 'vue'
import api from '../api/client.js'

// --- State ---
const balances = ref({})
const settlements = ref([])
const expenses = ref([])
const error = ref('')
const loading = ref(false)

const form = reactive({
  description: '',
  amount: '',
  paidBy: '',
  splitType: 'EQUAL',
  members: ['', ''],
})

// --- Computed ---
const memberList = computed(() => {
  const s = new Set()
  expenses.value.forEach(e => {
    s.add(e.paidBy)
    if (e.splits) Object.keys(e.splits).forEach(k => s.add(k))
  })
  Object.keys(balances.value).forEach(k => s.add(k))
  return [...s]
})

// --- Methods ---
function addMemberField() { form.members.push('') }
function removeMemberField(i) { if (form.members.length > 1) form.members.splice(i, 1) }

function buildExpense() {
  const desc = form.description.trim()
  const amt = parseFloat(form.amount)
  const payer = form.paidBy.trim()

  if (!desc) { error.value = 'Description is required'; return null }
  if (!amt || amt <= 0) { error.value = 'Amount must be positive'; return null }
  if (!payer) { error.value = 'PaidBy is required'; return null }

  const members = form.members.map(m => m.trim()).filter(Boolean)
  if (members.length === 0) { error.value = 'At least one member required'; return null }

  const splits = {}
  if (form.splitType === 'EQUAL') {
    members.forEach(m => splits[m] = 0)
  } else if (form.splitType === 'EXACT') {
    let sum = 0
    for (let i = 0; i < members.length; i++) {
      const val = parseFloat(prompt(`Exact amount for ${members[i]}:`) || '0')
      if (isNaN(val) || val < 0) { error.value = `Invalid amount for ${members[i]}`; return null }
      splits[members[i]] = val
      sum += val
    }
    if (Math.abs(sum - amt) > 0.01) { error.value = `Exact amounts sum (${sum}) ≠ total (${amt})`; return null }
  } else if (form.splitType === 'PERCENTAGE') {
    let sum = 0
    for (let i = 0; i < members.length; i++) {
      const val = parseFloat(prompt(`Percentage for ${members[i]}:`) || '0')
      if (isNaN(val) || val < 0) { error.value = `Invalid percentage for ${members[i]}`; return null }
      splits[members[i]] = val
      sum += val
    }
    if (Math.abs(sum - 100) > 0.01) { error.value = `Percentages sum to ${sum}, must be 100`; return null }
  }

  return { description: desc, amount: amt, paidBy: payer, splitType: form.splitType, splits }
}

async function submitExpense() {
  error.value = ''
  const expense = buildExpense()
  if (!expense) return

  try {
    loading.value = true
    const { data } = await api.addExpense(expense)
    updateState(data)
    resetForm()
  } catch (e) {
    error.value = e.response?.data?.error || e.message
  } finally {
    loading.value = false
  }
}

async function loadSample() {
  try {
    loading.value = true
    error.value = ''
    const resp = await fetch('/sample/expenses.json')
    const batch = await resp.json()
    const { data } = await api.addBatch(batch)
    updateState(data)
  } catch (e) {
    error.value = e.response?.data?.error || e.message
  } finally {
    loading.value = false
  }
}

async function handleFileUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    error.value = ''
    loading.value = true
    const text = await file.text()
    const batch = JSON.parse(text)
    if (!Array.isArray(batch)) throw new Error('JSON must be an array of expenses')
    const { data } = await api.addBatch(batch)
    updateState(data)
  } catch (e) {
    error.value = e.response?.data?.error || e.message
  } finally {
    loading.value = false
    e.target.value = ''
  }
}

async function clearAll() {
  await api.clear()
  balances.value = {}
  settlements.value = []
  expenses.value = []
  error.value = ''
}

function updateState(data) {
  balances.value = data.balances || {}
  settlements.value = data.settlements || []
  expenses.value = data.expenses || []
}

function resetForm() {
  form.description = ''
  form.amount = ''
  form.paidBy = ''
  form.splitType = 'EQUAL'
  form.members = ['', '']
}

function fmt(n) { return '₹' + Math.abs(n).toFixed(2) }
function signClass(n) { return n > 0 ? 'positive' : n < 0 ? 'negative' : 'zero' }
</script>

<template>
  <div class="home">
    <!-- Error banner -->
    <div v-if="error" class="error-banner">{{ error }}</div>

    <!-- Add Expense Card -->
    <section class="card">
      <h2>➕ Add Expense</h2>
      <div class="form-grid">
        <div class="field">
          <label>Description</label>
          <input v-model="form.description" placeholder="e.g. Team Dinner" />
        </div>
        <div class="field">
          <label>Amount (₹)</label>
          <input v-model="form.amount" type="number" min="0" step="0.01" placeholder="0.00" />
        </div>
        <div class="field">
          <label>Paid By</label>
          <input v-model="form.paidBy" placeholder="e.g. Alice" />
        </div>
        <div class="field">
          <label>Split Type</label>
          <select v-model="form.splitType">
            <option>EQUAL</option>
            <option>EXACT</option>
            <option>PERCENTAGE</option>
          </select>
        </div>
      </div>

      <div class="members-section">
        <label>Members</label>
        <div v-for="(m, i) in form.members" :key="i" class="member-row">
          <input v-model="form.members[i]" placeholder="Member name" />
          <button type="button" class="btn-sm" @click="removeMemberField(i)" :disabled="form.members.length <= 1">✕</button>
        </div>
        <button type="button" class="btn-sm btn-link" @click="addMemberField">+ Add member</button>
      </div>

      <button class="btn btn-primary" @click="submitExpense" :disabled="loading">
        {{ loading ? 'Adding...' : 'Add Expense' }}
      </button>
    </section>

    <!-- Import Card -->
    <section class="card">
      <h2>📥 Import Expenses</h2>
      <div class="import-row">
        <button class="btn btn-secondary" @click="loadSample" :disabled="loading">Load Sample</button>
        <label class="btn btn-secondary file-label">
          Upload JSON
          <input type="file" accept=".json" @change="handleFileUpload" hidden />
        </label>
        <button class="btn btn-outline" @click="clearAll">Clear All</button>
      </div>
    </section>

    <!-- Dashboard: Balances + Settlements side by side -->
    <div class="dashboard" v-if="Object.keys(balances).length > 0 || settlements.length > 0">
      <!-- Balances -->
      <section class="card">
        <h2>💰 Net Balances</h2>
        <p class="subtitle">Positive = owed money, Negative = owes money</p>
        <div v-for="(bal, name) in balances" :key="name" class="balance-row">
          <span class="member-name">{{ name }}</span>
          <span :class="['balance-amount', signClass(bal)]">
            {{ bal > 0 ? '+' : '' }}{{ fmt(bal) }}
          </span>
        </div>
        <p v-if="Object.keys(balances).length === 0" class="empty">No balances yet.</p>
      </section>

      <!-- Settlements -->
      <section class="card">
        <h2>🤝 Simplified Settlements</h2>
        <p class="subtitle">{{ settlements.length }} transaction{{ settlements.length !== 1 ? 's' : '' }} to settle up</p>
        <div v-for="(s, i) in settlements" :key="i" class="settlement-row">
          <strong>{{ s.from }}</strong>
          <span class="arrow">pays</span>
          <strong>{{ s.to }}</strong>
          <span class="settle-amount">{{ fmt(s.amount) }}</span>
        </div>
        <p v-if="settlements.length === 0" class="empty">All settled up! 🎉</p>
      </section>
    </div>

    <!-- Expense History -->
    <section class="card" v-if="expenses.length > 0">
      <h2>📋 Expense History ({{ expenses.length }})</h2>
      <div v-for="(e, i) in expenses" :key="i" class="expense-row">
        <div class="expense-info">
          <strong>{{ e.description }}</strong>
          <span class="tag">{{ e.splitType }}</span>
        </div>
        <div class="expense-meta">
          <span>{{ fmt(e.amount) }} paid by <strong>{{ e.paidBy }}</strong></span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1.25rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}
.card h2 { font-size: 1.1rem; margin-bottom: 0.75rem; color: var(--text); }
.subtitle { color: var(--muted); font-size: 0.8rem; margin-bottom: 0.75rem; }
.empty { color: var(--muted); font-style: italic; font-size: 0.9rem; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.25rem; }
.field label { font-size: 0.8rem; font-weight: 600; color: var(--muted); }
.field input, .field select {
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.2s;
}
.field input:focus, .field select:focus { border-color: var(--primary); }

.members-section { margin-bottom: 1rem; }
.members-section > label { font-size: 0.8rem; font-weight: 600; color: var(--muted); display: block; margin-bottom: 0.5rem; }
.member-row { display: flex; gap: 0.5rem; margin-bottom: 0.4rem; }
.member-row input {
  flex: 1;
  padding: 0.4rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.85rem;
  outline: none;
}
.member-row input:focus { border-color: var(--primary); }

.import-row { display: flex; gap: 0.75rem; flex-wrap: wrap; }
.file-label { cursor: pointer; display: inline-block; }

.dashboard { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }
@media (max-width: 640px) { .dashboard { grid-template-columns: 1fr; } .form-grid { grid-template-columns: 1fr; } }

.balance-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--border);
}
.balance-row:last-child { border-bottom: none; }
.member-name { font-weight: 600; }
.balance-amount { font-weight: 700; font-size: 0.95rem; font-family: monospace; }
.positive { color: var(--green); }
.negative { color: var(--red); }
.zero { color: var(--muted); }

.settlement-row {
  display: flex; align-items: center; gap: 0.5rem;
  padding: 0.6rem 0; border-bottom: 1px solid var(--border);
  font-size: 0.9rem;
}
.settlement-row:last-child { border-bottom: none; }
.arrow { color: var(--muted); font-size: 0.8rem; }
.settle-amount { margin-left: auto; font-weight: 700; color: var(--primary); font-family: monospace; }

.expense-row { padding: 0.5rem 0; border-bottom: 1px solid var(--border); font-size: 0.85rem; }
.expense-row:last-child { border-bottom: none; }
.expense-info { display: flex; align-items: center; gap: 0.5rem; }
.tag {
  font-size: 0.7rem; font-weight: 700; text-transform: uppercase;
  background: #eef2ff; color: var(--primary); padding: 0.15rem 0.5rem; border-radius: 100px;
}
.expense-meta { color: var(--muted); font-size: 0.8rem; }

.error-banner {
  background: #fef2f2; color: var(--red); border: 1px solid #fecaca;
  padding: 0.75rem 1rem; border-radius: 8px; margin-bottom: 1rem; font-size: 0.85rem;
}

.btn { padding: 0.6rem 1.25rem; border: none; border-radius: 8px; font-size: 0.85rem; font-weight: 600; cursor: pointer; transition: opacity 0.2s; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: var(--primary); color: #fff; width: 100%; margin-top: 0.5rem; }
.btn-secondary { background: #eef2ff; color: var(--primary); }
.btn-outline { background: transparent; border: 1px solid var(--border); color: var(--muted); }
.btn-sm { padding: 0.25rem 0.5rem; border: 1px solid var(--border); border-radius: 6px; background: var(--bg); cursor: pointer; font-size: 0.8rem; }
.btn-link { border: none; color: var(--primary); background: none; font-weight: 600; }
</style>