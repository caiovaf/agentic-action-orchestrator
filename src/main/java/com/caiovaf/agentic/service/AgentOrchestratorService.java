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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private final OrderServiceTool orderServiceTool;
    private final RefundServiceTool refundServiceTool;
    private final NotificationDispatchTool notificationDispatchTool;

    public AgentResponse orchestrate(AgentRequest request) {
        String traceId = "trc_" + UUID.randomUUID().toString().substring(0, 12);
        boolean dryRun = request.dryRun();
        log.info("Starting agentic orchestration [traceId={}, dryRun={}]: prompt='{}'", traceId, dryRun, request.prompt());

        List<ToolInvocationRecord> toolRecords = new ArrayList<>();
        String prompt = request.prompt();
        String promptLower = prompt.toLowerCase();

        // Step 1: Detect intent and resolve tool chain
        boolean isCancellation = promptLower.contains("cancel") || promptLower.contains("cancela");
        boolean isRefund = promptLower.contains("refund") || promptLower.contains("reembolso") || promptLower.contains("$") || promptLower.contains("r$");
        boolean isNotify = promptLower.contains("notify") || promptLower.contains("notifique") || promptLower.contains("email") || promptLower.contains("sms");

        String detectedIntent = isCancellation && isRefund ? "ORDER_CANCELLATION_AND_REFUND" :
                                isCancellation ? "ORDER_CANCELLATION" :
                                isRefund ? "FINANCIAL_REFUND" : "GENERAL_INQUIRY";

        // Dynamic Entity Extraction
        String orderId = extractOrderId(prompt, "ord_7894");
        ExtractedAmount extractedAmount = extractAmountAndCurrency(prompt, new BigDecimal("149.99"), "USD");
        String recipient = extractRecipient(prompt, "customer@enterprise.com");
        String channel = extractChannel(prompt, "EMAIL");

        // Step 2: Execute Tool Chain with deterministic validation or simulation (Dry-Run)
        if (isCancellation) {
            long start = System.currentTimeMillis();
            Map<String, Object> orderArgs = Map.of(
                "orderId", orderId,
                "action", "CANCEL",
                "reason", "Customer requested cancellation via natural language"
            );

            if (dryRun) {
                String simOutput = String.format("SIMULATED: Order %s scheduled for CANCEL", orderId);
                toolRecords.add(new ToolInvocationRecord("orderServiceTool", orderArgs, "DRY_RUN_PLAN", simOutput, Instant.now(), System.currentTimeMillis() - start));
            } else {
                OrderServiceTool.Response orderRes = orderServiceTool.apply(
                    new OrderServiceTool.Request(orderId, "CANCEL", "User intent via agent")
                );
                toolRecords.add(new ToolInvocationRecord("orderServiceTool", orderArgs, "SUCCESS", orderRes.message(), Instant.now(), System.currentTimeMillis() - start));
            }
        }

        if (isRefund) {
            long start = System.currentTimeMillis();
            Map<String, Object> refundArgs = Map.of(
                "orderId", orderId,
                "amount", extractedAmount.amount(),
                "currency", extractedAmount.currency()
            );

            if (dryRun) {
                String simOutput = String.format("SIMULATED: Refund of %s %s planned for %s", extractedAmount.amount(), extractedAmount.currency(), orderId);
                toolRecords.add(new ToolInvocationRecord("refundServiceTool", refundArgs, "DRY_RUN_PLAN", simOutput, Instant.now(), System.currentTimeMillis() - start));
            } else {
                RefundServiceTool.Response refundRes = refundServiceTool.apply(
                    new RefundServiceTool.Request(orderId, extractedAmount.amount(), extractedAmount.currency(), "Approved by automated agent workflow")
                );
                toolRecords.add(new ToolInvocationRecord("refundServiceTool", refundArgs, "SUCCESS", refundRes.confirmation(), Instant.now(), System.currentTimeMillis() - start));
            }
        }

        if (isNotify) {
            long start = System.currentTimeMillis();
            Map<String, Object> notifyArgs = Map.of(
                "recipient", recipient,
                "channel", channel
            );

            if (dryRun) {
                String simOutput = String.format("SIMULATED: Dispatch notification via %s to %s", channel, recipient);
                toolRecords.add(new ToolInvocationRecord("notificationDispatchTool", notifyArgs, "DRY_RUN_PLAN", simOutput, Instant.now(), System.currentTimeMillis() - start));
            } else {
                String notificationMsg = String.format("Your order %s has been processed for intent [%s].", orderId, detectedIntent);
                NotificationDispatchTool.Response notifyRes = notificationDispatchTool.apply(
                    new NotificationDispatchTool.Request(recipient, channel, notificationMsg)
                );
                toolRecords.add(new ToolInvocationRecord("notificationDispatchTool", notifyArgs, "SUCCESS", notifyRes.dispatchedTo(), Instant.now(), System.currentTimeMillis() - start));
            }
        }

        String summary;
        if (dryRun) {
            summary = String.format(
                "Agent generated simulation dry-run plan with %d tool action(s) for intent [%s]. Target order: %s. No state mutations committed.",
                toolRecords.size(), detectedIntent, orderId
            );
        } else if (toolRecords.isEmpty()) {
            summary = String.format(
                "No automated tool actions required for intent [%s].",
                detectedIntent
            );
        } else {
            summary = String.format(
                "Agent executed %d deterministic tool action(s) for intent [%s]. Order %s updated and confirmed.",
                toolRecords.size(), detectedIntent, orderId
            );
        }

        return new AgentResponse(
            traceId,
            detectedIntent,
            summary,
            toolRecords,
            true,
            Instant.now()
        );
    }

    private String extractOrderId(String prompt, String defaultId) {
        Pattern p1 = Pattern.compile("\\b(ord_[a-zA-Z0-9_-]+)\\b", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(prompt);
        if (m1.find()) {
            return m1.group(1);
        }

        Pattern p2 = Pattern.compile("#([a-zA-Z0-9_-]+)");
        Matcher m2 = p2.matcher(prompt);
        if (m2.find()) {
            String id = m2.group(1);
            return id.startsWith("ord_") ? id : "ord_" + id;
        }

        Pattern p3 = Pattern.compile("\\b(?:order|pedido)\\s+([a-zA-Z0-9_-]+)\\b", Pattern.CASE_INSENSITIVE);
        Matcher m3 = p3.matcher(prompt);
        if (m3.find()) {
            String id = m3.group(1);
            return id.startsWith("ord_") ? id : "ord_" + id;
        }

        return defaultId;
    }

    private record ExtractedAmount(BigDecimal amount, String currency) {}

    private ExtractedAmount extractAmountAndCurrency(String prompt, BigDecimal defaultAmount, String defaultCurrency) {
        Pattern pCurrencySymbol = Pattern.compile("(\\$|R\\$|€|£)\\s*([0-9]+(?:[.,][0-9]{1,2})?)", Pattern.CASE_INSENSITIVE);
        Matcher m1 = pCurrencySymbol.matcher(prompt);
        if (m1.find()) {
            String symbol = m1.group(1).toUpperCase();
            String valStr = m1.group(2).replace(',', '.');
            String currency = symbol.contains("R$") ? "BRL" : symbol.equals("€") ? "EUR" : symbol.equals("£") ? "GBP" : "USD";
            return new ExtractedAmount(new BigDecimal(valStr), currency);
        }

        Pattern pCurrencyCode = Pattern.compile("([0-9]+(?:[.,][0-9]{1,2})?)\\s*(USD|BRL|EUR|GBP|reais|dollars|dolares)", Pattern.CASE_INSENSITIVE);
        Matcher m2 = pCurrencyCode.matcher(prompt);
        if (m2.find()) {
            String valStr = m2.group(1).replace(',', '.');
            String code = m2.group(2).toUpperCase();
            String currency = (code.contains("BRL") || code.contains("REAIS")) ? "BRL" : (code.contains("EUR")) ? "EUR" : (code.contains("GBP")) ? "GBP" : "USD";
            return new ExtractedAmount(new BigDecimal(valStr), currency);
        }

        return new ExtractedAmount(defaultAmount, defaultCurrency);
    }

    private String extractRecipient(String prompt, String defaultRecipient) {
        Pattern pEmail = Pattern.compile("([a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)");
        Matcher m = pEmail.matcher(prompt);
        if (m.find()) {
            return m.group(1);
        }
        return defaultRecipient;
    }

    private String extractChannel(String prompt, String defaultChannel) {
        String lower = prompt.toLowerCase();
        if (lower.contains("sms")) return "SMS";
        if (lower.contains("webhook")) return "WEBHOOK";
        if (lower.contains("slack") || lower.contains("teams")) return "CHAT";
        return defaultChannel;
    }
}
