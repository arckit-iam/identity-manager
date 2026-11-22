// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.port;

import java.util.List;
import java.util.Optional;

import secop.idm1.domain.model.Account;

public interface AccountProvider {

    Account save(Account account);

    Optional<Account> findByIdentifier(String identifier);

    boolean existsByIdentifier(String identifier);

    /**
     * @param employeeId filtre sur l'identité (ignoré si {@code null})
     * @param typeName filtre sur le type (ignoré si {@code null})
     * @param uncorrelatedOnly ne retourne que les comptes sans identité
     */
    List<Account> findAll(String employeeId, String typeName, boolean uncorrelatedOnly);

    void deleteByIdentifier(String identifier);
}
