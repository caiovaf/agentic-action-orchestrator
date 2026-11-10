package com.caiovaf.agentic.dto;

import java.time.Instant;
import java.util.Map;

public record ToolInvocationRecord(
    String toolName,
    Map<String, Object> arguments,
    String status,
    String output,
    Instant executedAt
) {}
