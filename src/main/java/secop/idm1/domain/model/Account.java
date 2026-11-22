// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

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
    UUID jobId;
    OffsetDateTime createdAt;
}
