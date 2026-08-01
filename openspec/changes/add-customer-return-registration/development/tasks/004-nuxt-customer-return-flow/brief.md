# Task Brief: 004-nuxt-customer-return-flow

## Goal

Promote the approved prototype into a production SSR-safe Nuxt customer page.

## Vertical Slice

A customer can open `/return/[token]` in WeChat, complete the five steps, retry
photo failures and receive a localized accepted or review-required receipt.

## In Scope

- Nuxt API types, route, state hooks, step components, status pages and styles.
- Light/dark and `zh-CN`/`en` site preference support.
- Responsive, accessibility and browser behavior tests.
- Extract photo uploads and draft flow hooks; reuse the approved visual language and Nuxt runtime.

## Files Allowed

- `camera-rental-web`
- `openspec/changes/add-customer-return-registration`

## Verification Commands

- `bun run build`
- `bunx nuxi typecheck`
- Browser tests at mobile and desktop viewports.

## Stop Conditions

- Stop if code requires browser globals during server rendering.
- Stop if a frontend rule becomes authoritative for device matching or submission state.
- Stop if implementation cannot preserve draft data across retryable failures.
