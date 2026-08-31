## ADDED Requirements

### Requirement: Rental configuration is a standalone permissioned admin page
The system SHALL provide a Rental Configuration page under Rental Operations and SHALL require independent query and update permissions.

#### Scenario: Authorized administrator opens configuration
- **WHEN** an administrator has `rental:configuration:query`
- **THEN** the administrator can open the page and view device catalog, channel product rules, and remark conventions

#### Scenario: Missing permission
- **WHEN** an administrator lacks the configuration query permission
- **THEN** the menu and actions are unavailable and the backend rejects direct access

### Requirement: Device catalog mutation belongs to Rental Configuration
The system SHALL manage device categories, models, numbering prefixes, sort order, and enabled state from Rental Configuration, while Rental Device SHALL only consume the catalog.

#### Scenario: Create or update a model
- **WHEN** an authorized administrator saves a valid model and unique numbering prefix
- **THEN** the shared catalog is updated and becomes selectable when creating a physical device

#### Scenario: Rental Device page
- **WHEN** an operator creates or filters a physical device
- **THEN** the operator can select the shared category and model but cannot quick-create catalog records

### Requirement: Product rules are scoped by tenant, shop, and Xianyu item
The system MUST uniquely scope each channel product rule by current tenant, internal shop, and Xianyu `item_id`.

#### Scenario: Same item id in different shops
- **WHEN** two shops contain the same Xianyu item id
- **THEN** each shop can have a different rule without cross-shop matching

#### Scenario: Ambiguous shop seed
- **WHEN** an initial shop label cannot be resolved to exactly one internal shop
- **THEN** the system refuses to seed that shop's rules

### Requirement: Product handling policy is explicit
The system SHALL support `CREATE_RENTAL` and `CONFIG_SKIPPED` handling policies.

#### Scenario: Create rental policy
- **WHEN** a durable order matches an enabled `CREATE_RENTAL` rule
- **THEN** the system creates or updates its internal rental order and evaluates remark and model readiness

#### Scenario: Configuration skipped policy
- **WHEN** a durable order matches an enabled `CONFIG_SKIPPED` rule
- **THEN** the system retains channel revenue and raw evidence but does not parse remarks, create a review or rental order, or generate a schedule

### Requirement: Single-model products map at product level
The system SHALL map a single-model product by `shop_id + xianyu_item_id`.

#### Scenario: Exact single-model match
- **WHEN** an order exactly matches an enabled single-model rule
- **THEN** the configured equipment model is applied to an unassigned rental order item

#### Scenario: Missing item id
- **WHEN** an order lacks Xianyu item id
- **THEN** the system does not apply a product mapping by XianGuanJia product id or title

### Requirement: Multi-model products require exact synchronized SKU mappings
The system MUST map a multi-model product by `shop_id + xianyu_item_id + xgj_sku_id` and MUST NOT use a product-level fallback.

#### Scenario: Exact SKU match
- **WHEN** the order SKU belongs to the synchronized shop and item and has an enabled model mapping
- **THEN** the configured model is applied

#### Scenario: Unknown or unmapped SKU
- **WHEN** the order has no SKU, the SKU is not synchronized, or its mapping is disabled or missing
- **THEN** the order remains waiting for model configuration and cannot be allocated

#### Scenario: Forged cross-product SKU
- **WHEN** a request submits a SKU that belongs to another shop or item
- **THEN** the backend rejects the configuration without creating a mapping

### Requirement: Configuration changes preview and protect impact
The system SHALL preview affected order counts before a mapping, policy, mode, or enabled-state change and SHALL reconcile accepted changes asynchronously.

#### Scenario: Change affecting fulfilled orders
- **WHEN** an impact preview includes assigned or dispatched orders
- **THEN** the page warns that those orders will be reported for review rather than automatically changed

#### Scenario: Accepted change
- **WHEN** an authorized administrator confirms a version-valid change
- **THEN** the backend saves the rule, records an audit trail, and returns a reconciliation task reference

### Requirement: Remark conventions are visible and copyable
The system SHALL display the three approved base remark templates and the approved special-case suffixes with localized copy actions.

#### Scenario: Copy base template
- **WHEN** an operator copies a remark template
- **THEN** the exact template is copied and the UI confirms the action after clipboard success

#### Scenario: Theme and locale matrix
- **WHEN** the page renders in light/dark and `zh-CN`/`en`
- **THEN** templates, identifiers, states, dialogs, errors, and narrow layouts remain readable and operable
