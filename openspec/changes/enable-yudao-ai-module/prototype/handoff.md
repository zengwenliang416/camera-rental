# Prototype Handoff: enable-yudao-ai-module

## Approved Branch Variant

- Branch: `data-flow`
- Variant: database-backed dynamic OpenAI-compatible provider with auto-model
  providers disabled by default.
- Approval source: the user's explicit request to enable the current complete
  `yudao-module-ai` implementation and database migration.

## Screens Or Flows

- Existing AI administration pages and `/admin-api/ai/**` APIs.
- API key creation, encrypted persistence, model creation, and dynamic relay
  invocation.
- Independent customer return route regression flow.

## Components To Create

- Incremental AI schema migration.
- API-key encrypted-mapping regression test.

## Components To Reuse

- Existing `yudao-module-ai`, admin AI views, controllers, services, mappers,
  `AiModelFactory`, `EncryptTypeHandler`, menu records, permission checks, and
  deployment runner.

## Extraction Targets

- None. Encryption and migration execution already have shared project
  components.

## API Contracts

- AI management remains under authenticated `/admin-api/ai/**`.
- Customer return remains under public-session
  `/app-api/rental/return-registration/**`.

## Data Flows

- Admin request -> permission check -> `AiApiKeyService` ->
  `EncryptTypeHandler` -> tenant `ai_api_key` row.
- Model configuration -> `ai_model` -> `AiModelFactory` -> OpenAI-compatible
  relay.

## State Behavior

- Loading: application starts without a configured AI provider.
- Empty: AI list APIs return empty pages when no AI data exists.
- Error: relay failures do not prevent application startup.
- Disabled: optional provider integrations remain disabled unless explicitly
  configured.
- Permission: unauthorized AI management calls return business `401`.

## Theme And Locale Policy

- Theme support: not applicable to this non-visual prototype.
- Theme modes shown in prototype: none.
- Theme toggle: intentionally omitted.
- Internationalization: not applicable.
- Locales shown in prototype: none.
- Locale switcher: intentionally omitted.

## Out Of Scope Items

- AI frontend redesign.
- Automatic AI parsing of rental remarks.
- Enabling unconfigured image, music, vector-store, MCP, or search providers.
- Any plaintext secret in repository or deployment evidence.

## Required Tests

- AI reactor compile and executable JAR package.
- `AiApiKeyDOTest`.
- Migration first run and repeat-run idempotency.
- Authenticated query of all 14 AI table-backed page APIs.
- Unauthenticated AI API rejection.
- Customer return route and rental API regression probes.
- Production encrypted-value and end-to-end relay verification.

## Open Risks

- Production deployment must use the existing non-empty
  `MYBATIS_PLUS_ENCRYPTOR_PASSWORD`; rotating it would make stored ciphertext
  unreadable.
- External relay availability is independent of application health.
