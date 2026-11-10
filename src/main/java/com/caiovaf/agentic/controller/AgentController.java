package com.caiovaf.agentic.controller;

import com.caiovaf.agentic.dto.AgentRequest;
import com.caiovaf.agentic.dto.AgentResponse;
import com.caiovaf.agentic.service.AgentOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
@Tag(name = "Agentic Orchestrator API", description = "Translates natural user prompts into automated backend tool executions")
public class AgentController {

    private final AgentOrchestratorService orchestratorService;

    @PostMapping("/orchestrate")
    @Operation(summary = "Submit a natural language instruction to be evaluated and executed across system tools")
    public ResponseEntity<AgentResponse> orchestrateAction(@Valid @RequestBody AgentRequest request) {
        log.info("Incoming orchestration request: prompt='{}'", request.prompt());
        AgentResponse response = orchestratorService.orchestrate(request);
        return ResponseEntity.ok(response);
    }
}
