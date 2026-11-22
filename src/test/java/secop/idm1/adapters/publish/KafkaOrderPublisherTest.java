// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import secop.idm1.domain.model.AccountOrder;

class KafkaOrderPublisherTest {

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
    private final UUID orderId = UUID.randomUUID();

    private KafkaTemplate<String, String> kafka;
    private KafkaOrderPublisher publisher;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafka = mock(KafkaTemplate.class);
        publisher = new KafkaOrderPublisher(kafka, mapper, "arc.orders", "ad://DC01", "identity-manager");
    }

    @SuppressWarnings("unchecked")
    private void sendSucceeds() {
        when(kafka.send(eq("arc.orders"), eq(orderId.toString()), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @Test
    void submitsAFlatOrderMessageKeyedByTheOrderId() throws Exception {
        sendSucceeds();
        publisher.submit(new AccountOrder(orderId, AccountOrder.Action.CREATE, "e1234-adm", "admin", "e1234",
                Map.of("department", "IT"), OffsetDateTime.parse("2026-07-01T00:00:00Z"), "account:e1234-adm"));

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("arc.orders"), eq(orderId.toString()), json.capture());
        JsonNode message = mapper.readTree(json.getValue());
        assertEquals("SUBMIT", message.get("operation").asText());
        assertEquals(orderId.toString(), message.get("id").asText());
        assertEquals("CREATE", message.get("action").asText());
        assertEquals("IDENTITY", message.get("targetType").asText());
        assertEquals("e1234-adm", message.get("identityId").asText());
        assertEquals("admin", message.get("accountType").asText());
        assertEquals("ad://DC01", message.get("targetSystem").asText());
        assertEquals("identity-manager", message.get("source").asText());
        assertEquals("2026-07-01T00:00:00Z", message.get("executeAt").asText());
        assertEquals("e1234", message.get("attributes").get("employeeId").asText());
        assertEquals("IT", message.get("attributes").get("metadata.department").asText());
    }

    @Test
    void theMessageCanBeReadBackIntoTheSameDto() throws Exception {
        sendSucceeds();
        publisher.submit(new AccountOrder(orderId, AccountOrder.Action.DELETE, "e1234", "standard", null, null,
                OffsetDateTime.parse("2026-07-01T00:00:00Z"), "account:e1234:end"));
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("arc.orders"), eq(orderId.toString()), json.capture());

        OrderRequestDto dto = mapper.readValue(json.getValue(), OrderRequestDto.class);

        assertEquals("SUBMIT", dto.getOperation());
        assertEquals(orderId, dto.getId());
        assertEquals("DELETE", dto.getAction());
        assertEquals("IDENTITY", dto.getTargetType());
        assertEquals("e1234", dto.getIdentityId());
        assertEquals("e1234", dto.getIdentityName());
        assertEquals("ad://DC01", dto.getTargetSystem());
        assertEquals("standard", dto.getAccountType());
        assertEquals("identity-manager", dto.getSource());
        assertEquals(Map.of(), dto.getAttributes());
        assertEquals(OffsetDateTime.parse("2026-07-01T00:00:00Z").toInstant(), dto.getExecuteAt().toInstant());
        assertEquals("account:e1234:end", dto.getCorrelationId());
    }

    @Test
    void cancelsAnOrderWithAMinimalMessage() throws Exception {
        sendSucceeds();

        publisher.cancel(orderId);

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("arc.orders"), eq(orderId.toString()), json.capture());
        JsonNode message = mapper.readTree(json.getValue());
        assertEquals("CANCEL", message.get("operation").asText());
        assertEquals(orderId.toString(), message.get("id").asText());
        assertEquals(2, message.size());
    }

    @Test
    void reportsABrokerFailure() {
        when(kafka.send(eq("arc.orders"), eq(orderId.toString()), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        assertThrows(IllegalStateException.class, () -> publisher.cancel(orderId));
    }

    @Test
    void keepsTheInterruptionFlag() {
        CompletableFuture<SendResult<String, String>> never = new CompletableFuture<>();
        when(kafka.send(eq("arc.orders"), eq(orderId.toString()), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(never);
        Thread.currentThread().interrupt();

        try {
            assertThrows(IllegalStateException.class, () -> publisher.cancel(orderId));
            org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
