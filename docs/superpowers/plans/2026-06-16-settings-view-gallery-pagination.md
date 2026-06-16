# Settings View and Gallery Pagination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move low-frequency configuration into an in-app settings view and add paged gallery loading.

**Architecture:** Keep a single Tauri web entry point with hash-based `home`/`settings` views. Split JavaScript into small modules: config persistence, gallery pagination helpers, and the main DOM controller.

**Tech Stack:** Plain HTML/CSS/JavaScript modules, Node built-in test runner, existing Tauri commands.

---

### Task 1: Pagination Helper

**Files:**
- Create: `src/galleryPager.js`
- Test: `src/galleryPager.test.js`
- Modify: `package.json`

- [ ] Add a pure helper that clamps Bing page size to 1..8 and computes the next `idx` offset from returned item count.
- [ ] Cover first page, append page, partial page, and invalid count with `node --test`.

### Task 2: Config Module

**Files:**
- Create: `src/configStore.js`
- Modify: `src/main.js`

- [ ] Move localStorage read/write and form/config mapping into a focused module.
- [ ] Keep existing config shape unchanged for Tauri commands.

### Task 3: HTML View Split

**Files:**
- Modify: `src/index.html`
- Modify: `src/styles.css`

- [ ] Keep one HTML file but split it into commented regions: app toolbar, home view, settings view.
- [ ] Move all config controls into settings view.
- [ ] Add `设置`, `返回主页`, and `加载更多` controls.

### Task 4: Main Controller

**Files:**
- Modify: `src/main.js`

- [ ] Add hash-based view switching.
- [ ] Make refresh reset the gallery and load `idx=0`.
- [ ] Make load more append the next page and disable itself when Bing returns fewer than requested.
- [ ] Save settings changes and refresh the first page when Bing source settings change.

### Task 5: Verification

**Commands:**
- `npm run test:js`
- `cargo test`
- Optional visual check with the app dev server if dependencies are available.
