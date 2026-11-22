// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.time.OffsetDateTime;

public record IdentityDto(
        String employeeId,
        String firstName,
        String lastName,
        String email,
        OffsetDateTime createdAt) {
}
