<script setup>
</script>

<template>
  <div class="app">
    <header class="app-header">
      <h1>💸 Expense Splitter</h1>
      <p>Split team outing expenses, simplify debts</p>
    </header>
    <main>
      <router-view />
    </main>
  </div>
</template>

<style>
:root {
  --primary: #4f46e5;
  --green: #059669;
  --red: #dc2626;
  --bg: #f8fafc;
  --card: #fff;
  --border: #e2e8f0;
  --text: #1e293b;
  --muted: #64748b;
}
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  background: var(--bg);
  color: var(--text);
  line-height: 1.6;
}
.app { max-width: 960px; margin: 0 auto; padding: 1.5rem; }
.app-header {
  text-align: center;
  margin-bottom: 2rem;
  padding: 1.5rem 0 1rem;
  border-bottom: 2px solid var(--border);
}
.app-header h1 { font-size: 1.75rem; color: var(--primary); }
.app-header p { color: var(--muted); font-size: 0.9rem; margin-top: 0.25rem; }
</style>