# Changelog

All notable changes to `agentic-action-orchestrator` are documented here.

## [1.1.0] - 2026-11-24
### Added
- **Dynamic Natural Language Entity Extraction**: Regex-based token parsing for order IDs (`ord_xxx`, `#1234`, `order 1234`), monetary amounts and currencies (`$`, `R$`, `€`, `£`, `USD`, `BRL`, `EUR`), and recipient emails/channels (`SMS`, `EMAIL`, `WEBHOOK`, `CHAT`).
- **Dry-Run Simulation Mode**: When `dryRun: true`, the engine validates intent and builds structured execution plans (`DRY_RUN_PLAN`) without invoking mutating backend services.
- **Per-Tool Latency Telemetry**: Added `durationMs` to `ToolInvocationRecord` for execution time tracking and SLA auditing.
- **Dynamic Contextual Summaries**: Orchestration summary now accurately references target entity IDs and indicates simulation status.
- **Expanded Test Suite**: Comprehensive unit tests covering dynamic entity parsing, dry-run safety guarantees, and inquiry handling.

## [1.0.0] - 2026-11-05
### Added
- Core orchestration engine decomposing natural user prompts into atomic tool sequences.
- Registered enterprise tools: `OrderServiceTool`, `RefundServiceTool`, and `NotificationDispatchTool`.
- Unique correlation `traceId` generation for distributed tracking and compliance.
- Structured execution response including parameter schemas, execution status, and timestamps.
- Global RFC-7807 ProblemDetail controller advice for consistent client error handling.
- Comprehensive unit tests mocking enterprise tool invocations.
- GitHub Actions CI workflow for automated compilation and test execution.
