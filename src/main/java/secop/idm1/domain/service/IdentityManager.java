// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.service;

import java.util.List;

import secop.idm1.domain.model.Identity;

/** Gestion des identités humaines. */
public interface IdentityManager {

    List<Identity> getIdentities();

    Identity getIdentity(String employeeId);

    Identity createIdentity(Identity identity);

    /** Met à jour le prénom, le nom et l'e-mail ; le matricule ne change pas. */
    Identity updateIdentity(String employeeId, Identity identity);

    /** Supprime l'identité ; ses comptes sont conservés mais redeviennent non corrélés. */
    void deleteIdentity(String employeeId);
}
