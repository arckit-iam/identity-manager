// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import secop.idm1.domain.model.Account;

class ArcPublisherJobAdapterTest {

    private final UUID jobId = UUID.randomUUID();

    private MockRestServiceServer server;
    private ArcAuthTokenProvider tokens;
    private ArcPublisherJobAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        tokens = mock(ArcAuthTokenProvider.class);
        adapter = new ArcPublisherJobAdapter(builder, "http://arc-publisher", "ad://DC01", "identity-manager", tokens);
    }

    private String idJson() {
        return "\"" + jobId + "\"";
    }

    private static Account account(String identity) {
        Account account = new Account();
        account.setIdentifier("e1234-adm");
        account.setType("employee");
        account.setIdentity(identity);
        account.setMetadata(Map.of("department", "IT"));
        return account;
    }

    @Test
    void publishesAnIdentityJobWithTheServiceAccountToken() {
        when(tokens.getToken()).thenReturn("t1");
        server.expect(requestTo("http://arc-publisher/jobs"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer t1"))
                .andExpect(jsonPath("$.action").value("CREATE"))
                .andExpect(jsonPath("$.targetType").value("IDENTITY"))
                .andExpect(jsonPath("$.identityId").value("e1234-adm"))
                .andExpect(jsonPath("$.source").value("identity-manager"))
                .andExpect(jsonPath("$.attributes.accountType").value("employee"))
                .andExpect(jsonPath("$.attributes.employeeId").value("e1234"))
                .andExpect(jsonPath("$.attributes['metadata.department']").value("IT"))
                .andRespond(withSuccess(idJson(), MediaType.APPLICATION_JSON));

        assertEquals(jobId, adapter.publishCreateAccount(account("e1234")));

        server.verify();
    }

    @Test
    void publishesADeleteJobForAnUncorrelatedAccountWithoutMetadata() {
        when(tokens.getToken()).thenReturn("t1");
        server.expect(requestTo("http://arc-publisher/jobs"))
                .andExpect(jsonPath("$.action").value("DELETE"))
                .andExpect(jsonPath("$.attributes.employeeId").doesNotExist())
                .andRespond(withSuccess(idJson(), MediaType.APPLICATION_JSON));
        Account account = account(null);
        account.setMetadata(null);

        assertEquals(jobId, adapter.publishDeleteAccount(account));

        server.verify();
    }

    @Test
    void renewsTheTokenAndRetriesOnceWhenTheTokenIsRefused() {
        when(tokens.getToken()).thenReturn("old", "new");
        server.expect(requestTo("http://arc-publisher/jobs"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer old"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("http://arc-publisher/jobs"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer new"))
                .andRespond(withSuccess(idJson(), MediaType.APPLICATION_JSON));

        assertEquals(jobId, adapter.publishCreateAccount(account("e1234")));

        verify(tokens).invalidate();
        server.verify();
    }

    @Test
    void givesUpAfterASecondRefusal() {
        when(tokens.getToken()).thenReturn("t");
        server.expect(requestTo("http://arc-publisher/jobs")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("http://arc-publisher/jobs")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(HttpClientErrorException.Unauthorized.class,
                () -> adapter.publishCreateAccount(account("e1234")));

        verify(tokens, times(1)).invalidate();
    }
}
