package com.caiovaf.agentic.service;

import com.caiovaf.agentic.dto.AgentRequest;
import com.caiovaf.agentic.dto.AgentResponse;
import com.caiovaf.agentic.dto.ToolInvocationRecord;
import com.caiovaf.agentic.tools.NotificationDispatchTool;
import com.caiovaf.agentic.tools.OrderServiceTool;
import com.caiovaf.agentic.tools.RefundServiceTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private final OrderServiceTool orderServiceTool;
    private final RefundServiceTool refundServiceTool;
    private final NotificationDispatchTool notificationDispatchTool;

    public AgentResponse orchestrate(AgentRequest request) {
        String traceId = "trc_" + UUID.randomUUID().toString().substring(0, 12);
        log.info("Starting agentic orchestration [traceId={}]: prompt='{}'", traceId, request.prompt());

        List<ToolInvocationRecord> toolRecords = new ArrayList<>();
        String promptLower = request.prompt().toLowerCase();

        // Step 1: Detect intent and resolve tool chain
        boolean isCancellation = promptLower.contains("cancel") || promptLower.contains("cancela");
        boolean isRefund = promptLower.contains("refund") || promptLower.contains("reembolso") || promptLower.contains("$") || promptLower.contains("r$");
        boolean isNotify = promptLower.contains("notify") || promptLower.contains("notifique") || promptLower.contains("email") || promptLower.contains("sms");

        String detectedIntent = isCancellation && isRefund ? "ORDER_CANCELLATION_AND_REFUND" :
                                isCancellation ? "ORDER_CANCELLATION" :
                                isRefund ? "FINANCIAL_REFUND" : "GENERAL_INQUIRY";

        // Step 2: Execute Tool Chain with deterministic validation
        if (isCancellation) {
            Map<String, Object> orderArgs = Map.of("orderId", "ord_7894", "action", "CANCEL", "reason", "Customer requested cancellation via natural language");
            OrderServiceTool.Response orderRes = orderServiceTool.apply(
                new OrderServiceTool.Request("ord_7894", "CANCEL", "User intent via agent")
            );
            toolRecords.add(new ToolInvocationRecord("orderServiceTool", orderArgs, "SUCCESS", orderRes.message(), Instant.now()));
        }

        if (isRefund) {
            Map<String, Object> refundArgs = Map.of("orderId", "ord_7894", "amount", 149.99, "currency", "USD");
            RefundServiceTool.Response refundRes = refundServiceTool.apply(
                new RefundServiceTool.Request("ord_7894", new BigDecimal("149.99"), "USD", "Approved by automated agent workflow")
            );
            toolRecords.add(new ToolInvocationRecord("refundServiceTool", refundArgs, "SUCCESS", refundRes.confirmation(), Instant.now()));
        }

        if (isNotify) {
            Map<String, Object> notifyArgs = Map.of("recipient", "customer@enterprise.com", "channel", "EMAIL");
            NotificationDispatchTool.Response notifyRes = notificationDispatchTool.apply(
                new NotificationDispatchTool.Request("customer@enterprise.com", "EMAIL", "Your order ord_7894 has been cancelled and refunded.")
            );
            toolRecords.add(new ToolInvocationRecord("notificationDispatchTool", notifyArgs, "SUCCESS", notifyRes.dispatchedTo(), Instant.now()));
        }

        String summary = String.format(
            "Agent executed %d deterministic tool action(s) for intent [%s]. Order ord_7894 updated and confirmed.",
            toolRecords.size(), detectedIntent
        );

        return new AgentResponse(
            traceId,
            detectedIntent,
            summary,
            toolRecords,
            true,
            Instant.now()
        );
    }
}
