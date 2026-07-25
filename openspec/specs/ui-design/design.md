---
version: alpha
name: Camera Rental Platform Design System
description: Shared semantic UI rules for the camera-rental admin, customer, staff, and web clients.
colors:
  primary: "#171717"
  secondary: "#4d4d4d"
  tertiary: "#006bff"
  neutral: "#f2f2f2"
  background-100: "#ffffff"
  background-200: "#fafafa"
  gray-100: "#f2f2f2"
  gray-200: "#ebebeb"
  gray-300: "#e6e6e6"
  gray-400: "#eaeaea"
  gray-500: "#c9c9c9"
  gray-600: "#a8a8a8"
  gray-700: "#8f8f8f"
  gray-800: "#7d7d7d"
  gray-900: "#4d4d4d"
  gray-1000: "#171717"
  blue-700: "#006bff"
  red-800: "#ea001d"
  amber-700: "#ffae00"
  green-700: "#28a948"
typography:
  heading-32:
    fontFamily: Geist Sans
    fontSize: 32px
    fontWeight: 600
    lineHeight: 40px
    letterSpacing: -1.28px
  heading-24:
    fontFamily: Geist Sans
    fontSize: 24px
    fontWeight: 600
    lineHeight: 32px
    letterSpacing: -0.96px
  label-14:
    fontFamily: Geist Sans
    fontSize: 14px
    fontWeight: 500
    lineHeight: 20px
    letterSpacing: 0
  copy-14:
    fontFamily: Geist Sans
    fontSize: 14px
    fontWeight: 400
    lineHeight: 22px
    letterSpacing: 0
  button-14:
    fontFamily: Geist Sans
    fontSize: 14px
    fontWeight: 500
    lineHeight: 20px
    letterSpacing: 0
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 40px
rounded:
  sm: 6px
  md: 12px
  lg: 16px
components:
  button-primary:
    backgroundColor: "{colors.gray-1000}"
    textColor: "{colors.background-100}"
    typography: "{typography.button-14}"
    rounded: "{rounded.sm}"
    height: 40px
  button-secondary:
    backgroundColor: "{colors.background-100}"
    textColor: "{colors.primary}"
    typography: "{typography.button-14}"
    rounded: "{rounded.sm}"
    height: 40px
  input:
    backgroundColor: "{colors.background-100}"
    textColor: "{colors.primary}"
    typography: "{typography.label-14}"
    rounded: "{rounded.sm}"
    height: 40px
---

# Camera Rental Platform Design System

## Overview

This file defines shared semantic behavior across four client runtimes without
forcing them to use one component library. Admin retains Element Plus, staff
retains Wot UI and uni-app, the customer client retains its existing uni-app
components, and the customer website retains Nuxt/Vue components.

Rental interfaces must make the distinction between billable dates and occupied
dates visible wherever that distinction affects allocation, fulfillment, or
availability. Server-returned amount, status, and availability are authoritative.

## Colors

Use the frontmatter tokens as the semantic baseline for new rental surfaces.
`primary` is primary text, `secondary` is supporting text, `tertiary` is the
interactive accent, neutral/background tokens define surfaces and borders,
green indicates completed/available, amber indicates review or operational
attention, and red indicates destructive action, blocking conflict, or failure.

Status must never rely on color alone. Pair it with text and, where useful, an
icon. Existing application variables may map these semantic tokens to the local
component library.

## Typography

Use typography tokens instead of ad hoc sizes. Existing clients may map Geist
Sans to their established sans-serif family until font delivery is explicitly
approved. Device numbers, serial numbers, order numbers, tracking numbers, and
sync run identifiers use a local monospace/data style for scanning accuracy.

## Layout

- Admin: dense operational pages use filter area, primary table/calendar, and
  detail drawer/dialog. Keep primary actions visible without crowding the header.
- Customer web: SSR pages use a centered content width and responsive product
  grids; the checkout path becomes single-column on narrow screens.
- Customer and staff uni-app: use one primary task per screen, safe-area-aware
  bottom actions, and touch targets of at least 44 CSS pixels or platform
  equivalent.
- Use the spacing scale for section rhythm. Dense data tables may use `sm`;
  cards and major sections use `md` to `lg`.
- Schedule views must label both billable and occupied ranges rather than
  encoding the difference only through color.

## Elevation & Depth

Prefer surface and border hierarchy before shadows. Tables, schedule lanes, and
operational cards use subtle borders; floating drawers, dialogs, menus, and
popovers may use one restrained shadow level. Modal overlays must preserve
visible focus and prevent background actions.

## Motion

Motion is limited to meaningful page/section entry, drawer/dialog transition,
and state change feedback. Use approximately 160-240 ms easing and honor reduced
motion. Do not animate monetary totals, scan acceptance, destructive
confirmation, schedule conflict, or status changes in a way that delays the
result.

## Shapes

Controls use `rounded.sm`, cards and menus use `rounded.md`, and large dialogs or
marketing surfaces may use `rounded.lg`. Status pills may be fully rounded when
supported by the local design system. Do not mix unrelated radius systems in
one view.

## Components

- Buttons: one visually dominant primary action per task region; dangerous
  actions require explicit copy and confirmation.
- Inputs: labels remain visible; date inputs state whether they represent rent
  or occupied dates.
- Tables/lists: preserve stable identifiers, status, and relevant dates; mask
  customer-private data by default.
- Cards: group one domain concept rather than arbitrary fields.
- Dialogs/drawers: use dialogs for bounded confirmation and drawers/pages for
  detail review.
- Empty states: explain why no records exist and offer only permitted next
  actions.
- Toasts: confirm accepted server actions; do not use success copy before the
  response.
- Tooltips: supplement labels, never carry required workflow instructions alone.
- Loading: use skeletons for initial content and inline progress for mutations.
- Conflicts: show the server-reported reason and a refresh/reselect action.

## Voice & Content

Use concise Simplified Chinese operational copy by default and provide complete,
equivalent English copy for every supported flow. Name domain actions directly,
for example `确认分配` / `Confirm assignment`, `扫码出库` / `Scan outbound`,
`登记回仓` / `Register return`, `完成检测` / `Complete inspection`, and
`重试同步` / `Retry sync`. Avoid vague labels such as `处理` / `Process` where
the state transition matters.

Errors state what failed and the safe next action. Do not display third-party
raw errors, credentials, complete phone numbers, addresses, identity numbers,
or payment details.

## Theme & Internationalization

- Theme capability in the code baseline: admin and staff contain `light-dark`
  runtime support; customer uni-app contains light/dark/auto theme styles; Nuxt
  web requires a new theme implementation.
- Theme product policy: approved `light-dark` coverage for all four production
  clients.
- Theme toggle: approved `theme-toggle:user`. Admin, customer uni-app, staff,
  and Nuxt web must expose an accessible user-controlled light/dark switch and
  persist the selection using the runtime's existing safe preference storage.
- Internationalization capability in the code baseline: admin contains
  `vue-i18n` dictionaries for `zh-CN` and `en`; staff includes an i18n
  dependency; customer uni-app and Nuxt web require complete rental locale
  infrastructure and dictionaries.
- Internationalization product policy: approved `i18n:enabled`.
- Supported locales: `locales:zh-CN,en`.
- Default locale: `default-locale:zh-CN`; `zh-CN` is also the fallback locale.
- Locale toggle: `locale-toggle:user`. Every production client must expose an
  accessible language switch and persist the user's selection.
- Rental copy, statuses, validation, empty/error states, emails/notifications,
  exports, and public policy content must use locale keys or server-safe codes
  mapped by the client. Do not ship raw translation keys or untranslated
  fallback fragments.
- Prototype rule: prototype every new rental flow in both light and dark modes.
  Cover both `zh-CN` and `en`. Review desktop/mobile layouts plus loading, empty,
  conflict, error, dialog, drawer, table/calendar, scan, checkout, and payment
  states in all four theme-locale combinations.

## Do's and Don'ts

- Do use the token names above in prototypes and production code.
- Do require accessible focus states and body text contrast.
- Do record theme modes and locale coverage before starting a UI prototype.
- Do pair color state with icon or text.
- Do mask customer-private values in ordinary operational views.
- Do distinguish provisional client checks from accepted backend results.
- Don't add one-off colors, spacing, shadows, or radii without updating this spec.
- Don't claim product-wide dark mode or bilingual coverage until all four
  production clients pass the required matrix.
- Don't hide important state with color alone.
