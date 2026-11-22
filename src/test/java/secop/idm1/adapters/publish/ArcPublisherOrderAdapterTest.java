// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.publish;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import secop.idm1.domain.model.AccountOrder;

class ArcPublisherOrderAdapterTest {

    private final UUID orderId = UUID.randomUUID();

    private MockRestServiceServer server;
    private ArcAuthTokenProvider tokens;
    private ArcPublisherOrderAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        tokens = mock(ArcAuthTokenProvider.class);
        adapter = new ArcPublisherOrderAdapter(builder, "http://arc-publisher", "ad://DC01", "identity-manager",
                tokens);
    }

    private AccountOrder order(AccountOrder.Action action, String identity, Map<String, Object> metadata) {
        return new AccountOrder(orderId, action, "e1234-adm", "admin", identity, metadata,
                OffsetDateTime.parse("2026-07-01T00:00:00Z"), "account:e1234-adm");
    }

    @Test
    void submitsAnIdentityOrderWithTheServiceAccountToken() {
        when(tokens.getToken()).thenReturn("t1");
        server.expect(requestTo("http://arc-publisher/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer t1"))
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.action").value("CREATE"))
                .andExpect(jsonPath("$.targetType").value("IDENTITY"))
                .andExpect(jsonPath("$.identityId").value("e1234-adm"))
                .andExpect(jsonPath("$.accountType").value("admin"))
                .andExpect(jsonPath("$.source").value("identity-manager"))
                .andExpect(jsonPath("$.correlationId").value("account:e1234-adm"))
                .andExpect(jsonPath("$.operation").doesNotExist())
                .andExpect(jsonPath("$.attributes.employeeId").value("e1234"))
                .andExpect(jsonPath("$.attributes['metadata.department']").value("IT"))
                .andRespond(withSuccess("\"" + orderId + "\"", MediaType.APPLICATION_JSON));

        adapter.submit(order(AccountOrder.Action.CREATE, "e1234", Map.of("department", "IT")));

        server.verify();
    }

    @Test
    void submitsADeleteOrderForAnUncorrelatedAccountWithoutMetadata() {
        when(tokens.getToken()).thenReturn("t1");
        server.expect(requestTo("http://arc-publisher/orders"))
                .andExpect(jsonPath("$.action").value("DELETE"))
                .andExpect(jsonPath("$.attributes.employeeId").doesNotExist())
                .andRespond(withSuccess("\"" + orderId + "\"", MediaType.APPLICATION_JSON));

        adapter.submit(order(AccountOrder.Action.DELETE, null, null));

        server.verify();
    }

    @Test
    void renewsTheTokenAndRetriesOnceWhenTheTokenIsRefused() {
        when(tokens.getToken()).thenReturn("old", "new");
        server.expect(requestTo("http://arc-publisher/orders"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer old"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("http://arc-publisher/orders"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer new"))
                .andRespond(withSuccess("\"" + orderId + "\"", MediaType.APPLICATION_JSON));

        adapter.submit(order(AccountOrder.Action.CREATE, "e1234", Map.of()));

        verify(tokens).invalidate();
        server.verify();
    }

    @Test
    void givesUpAfterASecondRefusal() {
        when(tokens.getToken()).thenReturn("t");
        server.expect(requestTo("http://arc-publisher/orders")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("http://arc-publisher/orders")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(HttpClientErrorException.Unauthorized.class,
                () -> adapter.submit(order(AccountOrder.Action.CREATE, "e1234", Map.of())));

        verify(tokens, times(1)).invalidate();
    }

    @Test
    void cancelsAnOrder() {
        when(tokens.getToken()).thenReturn("t1");
        server.expect(requestTo("http://arc-publisher/orders/" + orderId))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer t1"))
                .andRespond(withNoContent());

        adapter.cancel(orderId);

        server.verify();
    }

    @Test
    void cancellingAnUnknownOrAlreadyHandledOrderIsHarmless() {
        when(tokens.getToken()).thenReturn("t1");
        server.expect(requestTo("http://arc-publisher/orders/" + orderId)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo("http://arc-publisher/orders/" + orderId)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertDoesNotThrow(() -> adapter.cancel(orderId));
        assertDoesNotThrow(() -> adapter.cancel(orderId));

        server.verify();
    }

    @Test
    void aFailingPublisherIsReportedOnCancel() {
        when(tokens.getToken()).thenReturn("t1");
        server.expect(requestTo("http://arc-publisher/orders/" + orderId))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThrows(HttpServerErrorException.class, () -> adapter.cancel(orderId));
    }
}
