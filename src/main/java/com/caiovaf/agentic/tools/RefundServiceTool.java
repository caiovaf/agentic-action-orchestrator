package com.caiovaf.agentic.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component("refundServiceTool")
@Description("Issue an automated financial refund for an order back to the customer's payment method")
public class RefundServiceTool implements Function<RefundServiceTool.Request, RefundServiceTool.Response> {

    public record Request(String orderId, BigDecimal amount, String currency, String justification) {}
    public record Response(String refundId, String orderId, String status, String confirmation) {}

    @Override
    public Response apply(Request request) {
        log.info("Executing RefundServiceTool: orderId={}, amount={}, currency={}, justification={}",
            request.orderId(), request.amount(), request.currency(), request.justification());

        String refundId = "ref_" + UUID.randomUUID().toString().substring(0, 8);

        return new Response(
            refundId,
            request.orderId(),
            "APPROVED",
            "Refund of " + request.amount() + " " + request.currency() + " successfully initiated with ID: " + refundId
        );
    }
}
