// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AccountDto(
        String identifier,
        String type,
        String identity,
        String source,
        Map<String, Object> metadata,
        LocalDate startDate,
        LocalDate endDate,
        UUID orderId,
        UUID endOrderId,
        OffsetDateTime createdAt) {
}
