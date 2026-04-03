# App Flow — Label Manager

## Overview

The Label Manager is a multi-label workspace. A user may own or manage several labels. All screens
operate in the context of the **active label**, selected via a header dropdown. Switching labels
reloads the workspace to that label's data.

---

## Domain Model

```
USER
 └── owns/manages → LABEL (one or many)
                      ├── has → LABEL EXPENSE (one or many)
                      ├── has → RELEASE (one or many)
                      │          ├── has → TRACK (one or many)
                      │          │          └── has → ARTIST (one or many)
                      │          ├── has → ARTIST (one or many, at release level)
                      │          ├── has → RELEASE EXPENSE (one or many)
                      │          ├── has → SALES
                      │          └── distributed by → DISTRIBUTOR (one or many)
                      └── works with → DISTRIBUTOR (one or many)
```

A label sells music both through distributors (physical, e.g. vinyl) and directly.

### Two levels of expenses

| Type | Scope | Examples |
|---|---|---|
| **Label expense** | Label-wide overhead, not tied to any release | Website hosting, office rent, legal fees |
| **Release expense** | Cost tied to producing a specific release | Mastering, vinyl pressing, artwork |

---

## Navigation Structure

### Primary sidebar

| Section | Route | Purpose |
|---|---|---|
| Dashboard | `/` | Label-level overview: financials, inventory, recent activity |
| Releases | `/releases` | Browse and manage all releases for the active label |
| Distributors | `/distributors` | Manage distributor relationships and inventory |
| Sales & Financials | `/sales` | Register sales, manage expenses, view financial performance |

### Secondary sidebar (visually separated — label utility)

| Section | Route | Purpose |
|---|---|---|
| Artists | `/artists` | Browse and manage the full artist roster for the active label |

---

## Screens

### Dashboard (`/`)
- Key label metrics: total revenue, total expenses (label + release), net profit
- Inventory snapshot across releases
- Recent sales activity

### Releases (`/releases`)
- List of all releases for the active label
- Actions: add release, filter/search

### Release Detail (`/releases/:id`)

Four sub-sections:

1. **Artists & Tracks** — tracklist with linked artists (each artist name links to `/artists/:id`)
2. **Distributor Allocations** — per-distributor unit counts + total inventory
3. **Expenses** — release-specific costs (mastering, pressing, artwork, etc.); add/edit here
4. **Sales & Financials** — sales figures and financial performance for this release

### Distributors (`/distributors`)
- List of distributors for the active label
- → Distributor Detail (`/distributors/:id`): relationship info, releases carried with per-release inventory

### Sales & Financials (`/sales`)
- **Register a sale** — pick release, enter quantity / price / date / channel (direct or distributor)
- **Transaction list** — all sales for the active label, filterable by release, date, channel
- **Label Expenses** — add and manage label-wide overhead costs not tied to any release
- **Full expense overview** — all expenses in one place: label expenses + release expenses, filterable
- **Label aggregate** — total revenue, total expenses (label + release combined), net profit
- **Per-release drill-down** — click a release row to navigate to its Release Detail financials

### Artists (`/artists`)
- Full artist roster scoped to the active label
- → Artist Detail (`/artists/:id`): contact info, releases they appear on, royalties owed and paid
- *Also accessible contextually: each artist name in a Release Detail view links here*

---

## Primary User Flows

### 1. Add a release and set it up
1. Navigate to Releases → click Add Release
2. Enter release info (title, date, format)
3. Add tracks; link artists to each track and to the release
4. Set distributor allocations (units per distributor)
5. Add release expenses (mastering, pressing, etc.)
6. Release is saved and visible in the Releases list

### 2. Register a sale
1. Navigate to Sales & Financials → click Register Sale
2. Select the release
3. Enter: quantity, price, date, channel (direct or distributor)
4. Save → transaction appears in the list; release and label financials update

### 3. Track inventory
- **Per release**: Release Detail → Distributor Allocations (per-distributor stock + total)
- **Across the label**: Dashboard inventory snapshot; or Distributors → Distributor Detail

### 4. View financial performance
- **Label level**: Sales & Financials → label aggregate view (all revenue minus all expenses)
- **Full expense breakdown**: Sales & Financials → Full expense overview (label + release expenses)
- **Per release**: Release Detail → Expenses + Sales & Financials sub-section
- **Drill-down path**: Sales & Financials → click a release row → Release Detail financials

### 5. Add a label expense
1. Navigate to Sales & Financials → Label Expenses
2. Click Add Expense
3. Enter: description, amount, date, category
4. Save → expense appears in label expenses list and in the full expense overview

### 6. Manage an artist
- From Release Detail: click artist name → Artist Detail
- From sidebar: Artists → browse roster → Artist Detail
- On Artist Detail: edit contact info, view all releases, review royalties
