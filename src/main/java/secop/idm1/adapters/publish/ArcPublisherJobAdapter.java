// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.publish;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import secop.idm1.domain.model.Account;
import secop.idm1.domain.port.JobPublisher;

@Component
public class ArcPublisherJobAdapter implements JobPublisher {

    private static final String TARGET_TYPE = "IDENTITY";

    private final RestClient restClient;
    private final ArcAuthTokenProvider tokenProvider;
    private final String targetSystem;
    private final String source;

    @Autowired
    public ArcPublisherJobAdapter(
            @Value("${arc.publisher.url}") String arcPublisherUrl,
            @Value("${arc.publisher.target-system}") String targetSystem,
            @Value("${arc.publisher.source}") String source,
            ArcAuthTokenProvider tokenProvider) {
        this(RestClient.builder(), arcPublisherUrl, targetSystem, source, tokenProvider);
    }

    ArcPublisherJobAdapter(RestClient.Builder builder, String arcPublisherUrl, String targetSystem,
            String source, ArcAuthTokenProvider tokenProvider) {
        this.restClient = builder.baseUrl(arcPublisherUrl).build();
        this.tokenProvider = tokenProvider;
        this.targetSystem = targetSystem;
        this.source = source;
    }

    @Override
    public UUID publishCreateAccount(Account account) {
        return sendAccountJob("CREATE", account);
    }

    @Override
    public UUID publishDeleteAccount(Account account) {
        return sendAccountJob("DELETE", account);
    }

    private UUID sendAccountJob(String action, Account account) {
        CreateJobRequestDto request = new CreateJobRequestDto();
        request.setAction(action);
        request.setTargetType(TARGET_TYPE);
        request.setIdentityId(account.getIdentifier());
        request.setIdentityName(account.getIdentifier());
        request.setTargetSystem(targetSystem);
        request.setSource(source);
        Map<String, String> attributes = new HashMap<>();
        attributes.put("accountType", account.getType());
        if (account.getIdentity() != null) {
            attributes.put("employeeId", account.getIdentity());
        }
        if (account.getMetadata() != null) {
            account.getMetadata().forEach((key, value) -> attributes.put("metadata." + key, String.valueOf(value)));
        }
        request.setAttributes(attributes);
        request.setExecuteAt(OffsetDateTime.now());
        request.setCorrelationId(UUID.randomUUID().toString());
        return sendWithRetry(request);
    }

    private UUID sendWithRetry(CreateJobRequestDto request) {
        try {
            return post(request);
        } catch (HttpClientErrorException.Unauthorized e) {
            // Jeton refusé (clés d'arc-auth renouvelées, par exemple) : un nouveau jeton, une seule nouvelle tentative.
            tokenProvider.invalidate();
            return post(request);
        }
    }

    private UUID post(CreateJobRequestDto request) {
        return restClient.post()
                .uri("/jobs")
                .headers(headers -> headers.setBearerAuth(tokenProvider.getToken()))
                .body(request)
                .retrieve()
                .body(UUID.class);
    }
}
