// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.port;

/** Génère les matricules des nouvelles identités. */
public interface EmployeeIdGenerator {

    /** @return un nouveau matricule, au format {@code I<DDDDDD>} par défaut, jamais renvoyé deux fois */
    String next();
}