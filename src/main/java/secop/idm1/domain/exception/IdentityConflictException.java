// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.exception;

/** Une identité portant ce matricule existe déjà. */
public class IdentityConflictException extends RuntimeException {
    public IdentityConflictException(String message) {
        super(message);
    }
}
