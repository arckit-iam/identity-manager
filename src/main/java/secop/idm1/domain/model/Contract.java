// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/** Contrat d'une identité : un type configurable, une période et des métadonnées propres au type. */
@Data
public class Contract {
    Long id;
    String employeeId;
    String type;
    LocalDate startDate;
    LocalDate endDate;
    Map<String, Object> metadata = new HashMap<>();
    OffsetDateTime createdAt;
}