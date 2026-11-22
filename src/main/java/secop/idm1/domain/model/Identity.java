// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

import java.time.OffsetDateTime;

import lombok.Data;

/** Identité humaine, identifiée par son matricule ({@code employeeId}). */
@Data
public class Identity {
    Long id;
    String employeeId;
    String firstName;
    String lastName;
    String email;
    OffsetDateTime createdAt;
}
