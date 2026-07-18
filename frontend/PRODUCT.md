# Product

## Register

product

## Users

Two audiences share this dashboard:

- **Internal operations and admin staff**, managing access structures: creating and editing roles, assigning endpoint and page permissions, and managing team spaces. Their context is operational — they're in a workflow, not browsing. Speed, clarity, and predictable behavior matter more than visual delight.
- **End users tracking their own or their family's finances** inside a Space: registering bank accounts, categorizing income/expenses, logging transactions and transfers, and reading reports. Their context is personal and recurring — they come back weekly or monthly to stay on top of money, not to configure a system. Trust and clarity about *their own numbers* matter more than admin-style density.

Both groups use the same visual language and the same underlying permission model (a regular end user is simply a Space member with a non-admin Role) — the interface doesn't fork into two different products, but the financial pages (Bank Accounts, Categories, Payment Methods, Transactions, Reports) are read by people who are not IT professionals and should never feel like an ops console.

## Product Purpose

Two things, on the same platform: (1) an admin control plane letting ops teams manage roles, permissions (API + front-page), and workspace membership; and (2) a personal/family financial control tool letting Space members track bank account balances, categorize and record transactions (including transfers between accounts), and generate reports to understand where their money is going. Success means an admin can understand the permission structure at a glance and make changes confidently without fear of breaking access for real users, **and** an end user can look at a report and immediately trust the numbers reflect their real financial situation.

## Brand Personality

Professional · Reliable · Clean. The interface should communicate that it's in control of complexity. No flair for flair's sake. Structured, scannable, well-labeled. When something is destructive or irreversible, it says so clearly. On the financial pages this same personality reads as *trustworthy*, not *cold*: numbers are always presented plainly and predictably (never dramatized, never guilt-tripping about spending), because the person reading a balance or a report is trusting the app with their real money.

## Anti-references

- Overly playful consumer apps (Notion's whimsy, Linear's microanimations for their own sake)
- Dark-mode-by-default "hacker" tooling aesthetics
- Cluttered enterprise dashboards that expose everything at once (SAP, older Oracle UIs)

## Design Principles

1. **Clarity over cleverness** — admins and end users alike need to read state instantly (a permission list, an account balance, a report total); never sacrifice scannability for visual interest.
2. **Progressive disclosure** — surface the most common operations first; reveal complexity only when needed.
3. **Consistent weight** — same conceptual actions get the same visual weight throughout (primary/secondary/destructive never swap roles).
4. **Trust through predictability** — interactions behave exactly as expected; no surprise side effects.
5. **Errors are informative, not alarming** — problem + cause + recovery path, never just a red bar.

## Accessibility & Inclusion

WCAG 2.1 AA. All text at 4.5:1 contrast minimum (3:1 for large text). Keyboard navigation throughout. Focus indicators visible. Supports `prefers-reduced-motion`.
