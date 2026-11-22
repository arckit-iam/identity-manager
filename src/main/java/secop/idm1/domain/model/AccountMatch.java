// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

import java.util.Map;
import java.util.Optional;

/**
 * Résultat de la reconnaissance d'un identifiant par un type de compte : le type, et les valeurs
 * capturées par les groupes nommés de l'expression régulière.
 */
public record AccountMatch(AccountType type, Map<String, String> captures) {

    public static final String IDENTITY_GROUP = "identity";

    public AccountMatch {
        captures = Map.copyOf(captures);
    }

    /** Référence de l'identité humaine capturée (groupe {@code identity}), s'il y en a une. */
    public Optional<String> identity() {
        return Optional.ofNullable(captures.get(IDENTITY_GROUP));
    }
}
