// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ordre de provisioning d'un compte, identifié par le manager (idempotent) et exécuté à {@code executeAt}.
 * Le manager ne connaît pas le rangement dans l'annuaire : il transmet seulement le type de compte.
 */
public record AccountOrder(
        UUID id,
        Action action,
        String identifier,
        String accountType,
        String identity,
        Map<String, Object> metadata,
        OffsetDateTime executeAt,
        String correlationId) {

    public enum Action {
        CREATE, DELETE
    }

    public static AccountOrder of(UUID id, Action action, Account account, OffsetDateTime executeAt,
            String correlationId) {
        Map<String, Object> metadata = account.getMetadata() == null ? Map.of()
                : new HashMap<>(account.getMetadata());
        return new AccountOrder(id, action, account.getIdentifier(), account.getType(), account.getIdentity(),
                metadata, executeAt, correlationId);
    }
}
