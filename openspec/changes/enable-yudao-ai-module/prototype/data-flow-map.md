# Data Flow Prototype

## Flow

1. A tenant administrator opens the existing AI API-key page and submits an
   OpenAI-compatible relay configuration.
2. The existing admin client sends the request to the protected
   `/admin-api/ai/api-key/**` API with tenant and bearer authentication.
3. Spring Security and `ai:api-key:*` permission checks reject unauthenticated
   or unauthorized requests before persistence.
4. `AiApiKeyService` maps the request to `AiApiKeyDO`.
5. `EncryptTypeHandler` encrypts `apiKey` using
   `MYBATIS_PLUS_ENCRYPTOR_PASSWORD`; `ai_api_key.api_key` receives ciphertext.
6. A tenant administrator creates an `ai_model` row referencing the key and
   model identifier `gpt-5.6-luna`.
7. `AiModelFactory` loads and decrypts the key at runtime and creates the
   OpenAI-compatible client dynamically from database configuration.
8. AI chat or model testing uses that dynamic client. Startup does not create
   unconfigured provider clients because all Spring AI auto-model selectors and
   optional provider integrations default to disabled.
9. Existing AI pages query the 14 tenant-aware `ai_*` tables. The customer
   return service remains independently registered under
   `/app-api/rental/return-registration/**`.

## Review Focus

- Loading behavior: the backend starts with no AI key and no auto-configured
  external provider.
- Empty behavior: all AI page queries return empty pages instead of missing-table
  errors before a model is configured.
- Error behavior: an unavailable relay is reported by the model call without
  preventing application startup.
- Permission behavior: unauthenticated AI management requests return business
  code `401`; API-key CRUD remains permission protected.
- Retry behavior: retry remains owned by the existing AI client/service; the
  migration and deployment runner are independently idempotent.

## Invariants

- No real API key appears in source, SQL, deployment artifacts, or evidence.
- A newly persisted API key is ciphertext in MySQL and decrypts only inside the
  application.
- Migration `20260802_038_enable_yudao_ai_module.sql` can run repeatedly without
  duplicate tables or active tenant-admin menu grants.
- Enabling AI does not remove the rental module from the executable JAR.
