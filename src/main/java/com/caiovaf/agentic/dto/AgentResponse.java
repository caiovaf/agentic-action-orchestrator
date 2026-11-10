package com.caiovaf.agentic.dto;

import java.time.Instant;
import java.util.List;

public record AgentResponse(
    String traceId,
    String intentDetected,
    String summaryResponse,
    List<ToolInvocationRecord> toolInvocations,
    boolean success,
    Instant completedAt
) {}
