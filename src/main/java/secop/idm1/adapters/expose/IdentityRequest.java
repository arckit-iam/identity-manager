// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

/** Corps de création ou de mise à jour d'une identité. */
/** Le matricule n'est pas saisi : il est généré à la création et ne change plus. */
public record IdentityRequest(String firstName, String lastName, String email) {
}
