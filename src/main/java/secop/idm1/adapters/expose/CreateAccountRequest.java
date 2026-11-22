// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.util.Map;

/** Corps de création d'un compte ; {@code type} et {@code identity} sont déduits de l'identifiant s'ils sont absents. */
public record CreateAccountRequest(String identifier, String type, String identity, Map<String, Object> metadata) {
}
