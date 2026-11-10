package com.caiovaf.agentic.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentRequest(
    @NotBlank(message = "Prompt cannot be blank")
    String prompt,

    String contextUserId,

    boolean dryRun
) {}
