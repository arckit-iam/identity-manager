// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.publish;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import secop.idm1.domain.model.AccountOrder;

/**
 * Ordre tel que reçu par arc-publisher : corps de {@code POST /orders}, ou message Kafka à plat
 * (avec {@code operation}) sur le topic des ordres.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderRequestDto {

    private static final String TARGET_TYPE = "IDENTITY";

    private String operation;
    private UUID id;
    private String action;
    private String targetType;
    private String identityId;
    private String identityName;
    private String targetSystem;
    private String accountType;
    private String source;
    private Map<String, String> attributes;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime executeAt;
    private String correlationId;

    public static OrderRequestDto from(AccountOrder order, String targetSystem, String source) {
        OrderRequestDto dto = new OrderRequestDto();
        dto.id = order.id();
        dto.action = order.action().name();
        dto.targetType = TARGET_TYPE;
        dto.identityId = order.identifier();
        dto.identityName = order.identifier();
        dto.targetSystem = targetSystem;
        dto.accountType = order.accountType();
        dto.source = source;
        Map<String, String> attributes = new HashMap<>();
        if (order.identity() != null) {
            attributes.put("employeeId", order.identity());
        }
        if (order.metadata() != null) {
            order.metadata().forEach((key, value) -> attributes.put("metadata." + key, String.valueOf(value)));
        }
        dto.attributes = attributes;
        dto.executeAt = order.executeAt();
        dto.correlationId = order.correlationId();
        return dto;
    }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }
    public String getIdentityName() { return identityName; }
    public void setIdentityName(String identityName) { this.identityName = identityName; }
    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    public OffsetDateTime getExecuteAt() { return executeAt; }
    public void setExecuteAt(OffsetDateTime executeAt) { this.executeAt = executeAt; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
