// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.time.LocalDate;
import java.util.Map;

/**
 * Corps de création d'un compte ; {@code type} et {@code identity} sont déduits de l'identifiant s'ils sont absents.
 * {@code startDate} et {@code endDate} (dernier jour inclus) planifient la création et la suppression.
 */
public record CreateAccountRequest(String identifier, String type, String identity, Map<String, Object> metadata,
        LocalDate startDate, LocalDate endDate) {
}
