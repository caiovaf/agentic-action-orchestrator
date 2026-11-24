package com.caiovaf.agentic.service;

import com.caiovaf.agentic.dto.AgentRequest;
import com.caiovaf.agentic.dto.AgentResponse;
import com.caiovaf.agentic.dto.ToolInvocationRecord;
import com.caiovaf.agentic.tools.NotificationDispatchTool;
import com.caiovaf.agentic.tools.OrderServiceTool;
import com.caiovaf.agentic.tools.RefundServiceTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorServiceTest {

    @Mock
    private OrderServiceTool orderServiceTool;

    @Mock
    private RefundServiceTool refundServiceTool;

    @Mock
    private NotificationDispatchTool notificationDispatchTool;

    @InjectMocks
    private AgentOrchestratorService orchestratorService;

    @Test
    @DisplayName("Should detect cancellation, refund, and notification intents and execute registered tools")
    void shouldOrchestrateMultiToolExecution() {
        when(orderServiceTool.apply(any())).thenReturn(
            new OrderServiceTool.Response("ord_7894", "CANCELLED", "Order successfully cancelled")
        );
        when(refundServiceTool.apply(any())).thenReturn(
            new RefundServiceTool.Response("ref_123", "ord_7894", "APPROVED", "Refund initiated")
        );
        when(notificationDispatchTool.apply(any())).thenReturn(
            new NotificationDispatchTool.Response("deliv_99", "DELIVERED", "customer@enterprise.com")
        );

        AgentRequest request = new AgentRequest(
            "Please cancel order ord_7894, issue refund of $149.99 and notify customer via email",
            "usr_ops_1",
            false
        );

        AgentResponse response = orchestratorService.orchestrate(request);

        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("ORDER_CANCELLATION_AND_REFUND", response.intentDetected());
        assertEquals(3, response.toolInvocations().size());

        verify(orderServiceTool, times(1)).apply(any());
        verify(refundServiceTool, times(1)).apply(any());
        verify(notificationDispatchTool, times(1)).apply(any());
    }

    @Test
    @DisplayName("Should dynamically extract custom order ID, amount, currency and recipient from prompt")
    void shouldExtractCustomEntitiesDynamically() {
        when(orderServiceTool.apply(any())).thenReturn(
            new OrderServiceTool.Response("ord_9842", "CANCELLED", "Order cancelled")
        );
        when(refundServiceTool.apply(any())).thenReturn(
            new RefundServiceTool.Response("ref_555", "ord_9842", "APPROVED", "Refund confirmed")
        );
        when(notificationDispatchTool.apply(any())).thenReturn(
            new NotificationDispatchTool.Response("deliv_101", "DELIVERED", "finance@enterprise.com")
        );

        AgentRequest request = new AgentRequest(
            "Customer wants to cancel order #9842 with refund of R$ 350.50 and notify finance@enterprise.com via sms",
            "usr_ops_2",
            false
        );

        AgentResponse response = orchestratorService.orchestrate(request);

        assertNotNull(response);
        assertTrue(response.success());
        assertEquals(3, response.toolInvocations().size());

        ArgumentCaptor<OrderServiceTool.Request> orderCaptor = ArgumentCaptor.forClass(OrderServiceTool.Request.class);
        verify(orderServiceTool).apply(orderCaptor.capture());
        assertEquals("ord_9842", orderCaptor.getValue().orderId());

        ArgumentCaptor<RefundServiceTool.Request> refundCaptor = ArgumentCaptor.forClass(RefundServiceTool.Request.class);
        verify(refundServiceTool).apply(refundCaptor.capture());
        assertEquals("ord_9842", refundCaptor.getValue().orderId());
        assertEquals(new BigDecimal("350.50"), refundCaptor.getValue().amount());
        assertEquals("BRL", refundCaptor.getValue().currency());

        ArgumentCaptor<NotificationDispatchTool.Request> notifyCaptor = ArgumentCaptor.forClass(NotificationDispatchTool.Request.class);
        verify(notificationDispatchTool).apply(notifyCaptor.capture());
        assertEquals("finance@enterprise.com", notifyCaptor.getValue().recipient());
        assertEquals("SMS", notifyCaptor.getValue().channel());
    }

    @Test
    @DisplayName("Should generate simulation dry-run plan without invoking backend tools when dryRun=true")
    void shouldExecuteDryRunSimulationWithoutMutatingTools() {
        AgentRequest request = new AgentRequest(
            "Cancel order ord_5511 and refund $89.00",
            "usr_auditor",
            true
        );

        AgentResponse response = orchestratorService.orchestrate(request);

        assertNotNull(response);
        assertTrue(response.success());
        assertEquals(2, response.toolInvocations().size());
        assertTrue(response.summaryResponse().contains("simulation dry-run"));

        for (ToolInvocationRecord record : response.toolInvocations()) {
            assertEquals("DRY_RUN_PLAN", record.status());
            assertTrue(record.output().startsWith("SIMULATED:"));
        }

        // Verify that mutating tools were NEVER called
        verify(orderServiceTool, never()).apply(any());
        verify(refundServiceTool, never()).apply(any());
        verify(notificationDispatchTool, never()).apply(any());
    }

    @Test
    @DisplayName("Should handle general inquiry without tool invocations")
    void shouldHandleGeneralInquiryWithoutToolExecutions() {
        AgentRequest request = new AgentRequest(
            "What are your business operating hours?",
            "usr_guest",
            false
        );

        AgentResponse response = orchestratorService.orchestrate(request);

        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("GENERAL_INQUIRY", response.intentDetected());
        assertTrue(response.toolInvocations().isEmpty());
        assertTrue(response.summaryResponse().contains("No automated tool actions required"));

        verify(orderServiceTool, never()).apply(any());
        verify(refundServiceTool, never()).apply(any());
        verify(notificationDispatchTool, never()).apply(any());
    }
}
