// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

/** Corps de création ou de mise à jour d'une identité. */
public record IdentityRequest(String employeeId, String firstName, String lastName, String email) {
}
