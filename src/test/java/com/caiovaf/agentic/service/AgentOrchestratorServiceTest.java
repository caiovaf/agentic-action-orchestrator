package com.caiovaf.agentic.service;

import com.caiovaf.agentic.dto.AgentRequest;
import com.caiovaf.agentic.dto.AgentResponse;
import com.caiovaf.agentic.tools.NotificationDispatchTool;
import com.caiovaf.agentic.tools.OrderServiceTool;
import com.caiovaf.agentic.tools.RefundServiceTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    void setUp() {
        when(orderServiceTool.apply(any())).thenReturn(
            new OrderServiceTool.Response("ord_7894", "CANCELLED", "Order successfully cancelled")
        );
        when(refundServiceTool.apply(any())).thenReturn(
            new RefundServiceTool.Response("ref_123", "ord_7894", "APPROVED", "Refund initiated")
        );
        when(notificationDispatchTool.apply(any())).thenReturn(
            new NotificationDispatchTool.Response("deliv_99", "DELIVERED", "customer@enterprise.com")
        );
    }

    @Test
    @DisplayName("Should detect cancellation, refund, and notification intents and execute registered tools")
    void shouldOrchestrateMultiToolExecution() {
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
}
