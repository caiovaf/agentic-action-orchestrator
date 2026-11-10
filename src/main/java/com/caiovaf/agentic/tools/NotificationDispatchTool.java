package com.caiovaf.agentic.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component("notificationDispatchTool")
@Description("Dispatch customer notifications via Email, SMS, or Event Webhook")
public class NotificationDispatchTool implements Function<NotificationDispatchTool.Request, NotificationDispatchTool.Response> {

    public record Request(String recipient, String channel, String messageContent) {}
    public record Response(String deliveryId, String status, String dispatchedTo) {}

    @Override
    public Response apply(Request request) {
        log.info("Executing NotificationDispatchTool: channel={}, recipient={}",
            request.channel(), request.recipient());

        String deliveryId = "deliv_" + UUID.randomUUID().toString().substring(0, 8);

        return new Response(
            deliveryId,
            "DELIVERED",
            "Notification successfully queued via " + request.channel() + " to " + request.recipient()
        );
    }
}
