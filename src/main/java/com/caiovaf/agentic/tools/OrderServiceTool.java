package com.caiovaf.agentic.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component("orderServiceTool")
@Description("Inspect or cancel a customer order by order ID in the enterprise system")
public class OrderServiceTool implements Function<OrderServiceTool.Request, OrderServiceTool.Response> {

    public record Request(String orderId, String action, String reason) {}
    public record Response(String orderId, String status, String message) {}

    @Override
    public Response apply(Request request) {
        log.info("Executing OrderServiceTool: orderId={}, action={}, reason={}",
            request.orderId(), request.action(), request.reason());

        if ("CANCEL".equalsIgnoreCase(request.action())) {
            return new Response(request.orderId(), "CANCELLED",
                "Order " + request.orderId() + " successfully marked as CANCELLED. Reason: " + request.reason());
        }

        return new Response(request.orderId(), "ACTIVE",
            "Order " + request.orderId() + " is active. Total items: 2. Status: PENDING_DISPATCH.");
    }
}
