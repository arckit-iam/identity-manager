// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Data;

/** Compte utilisateur, rattaché à une identité humaine. */
@Data
public class Account {
    Long id;
    String identifier;
    String type;
    /** Matricule de l'identité à laquelle le compte est corrélé ; {@code null} tant qu'il n'est pas rattaché. */
    String identity;
    AccountSource source;
    Map<String, Object> metadata = new HashMap<>();
    /** Premier jour de validité ; {@code null} : le compte est créé immédiatement. */
    LocalDate startDate;
    /** Dernier jour de validité (inclus) ; {@code null} : le compte n'expire pas. */
    LocalDate endDate;
    /** Ordre de création du compte. */
    UUID orderId;
    /** Ordre de suppression planifié à la date de fin, tant qu'il n'est pas exécuté. */
    UUID endOrderId;
    OffsetDateTime createdAt;
}
