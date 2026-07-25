# Prototype Question: xian-guanjia-data-integration-v1

## Question

Can a read-only XianGuanJia order sync preserve external evidence, convert only
eligible orders, route incomplete data to manual review, and atomically assign
a concrete device without schedule overlaps?

## Branch

`data-flow`

## Review Target

- Entry: `data-flow-map.md`
- Required reviewer decision: approve the conversion and assignment boundaries
  before production implementation.

## Out of Scope

- Production implementation.
- Database writes.
- Deployment behavior.
- XianGuanJia write operations.
