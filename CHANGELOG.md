# Changelog

All notable changes to `agentic-action-orchestrator` are documented here.

## [1.0.0] - 2026-11-05
### Added
- Core orchestration engine decomposing natural user prompts into atomic tool sequences.
- Registered enterprise tools: `OrderServiceTool`, `RefundServiceTool`, and `NotificationDispatchTool`.
- Unique correlation `traceId` generation for distributed tracking and compliance.
- Structured execution response including parameter schemas, execution status, and timestamps.
- Global RFC-7807 ProblemDetail controller advice for consistent client error handling.
- Comprehensive unit tests mocking enterprise tool invocations.
- GitHub Actions CI workflow for automated compilation and test execution.
