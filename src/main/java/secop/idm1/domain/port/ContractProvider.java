// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.port;

import java.util.List;
import java.util.Optional;

import secop.idm1.domain.model.Contract;

/** Persistance des contrats des identités. */
public interface ContractProvider {

    Contract save(Contract contract);

    Optional<Contract> findById(Long id);

    /** @param employeeId matricule, ou {@code null} pour toutes les identités
     *  @param typeName type de contrat, ou {@code null} pour tous */
    List<Contract> findAll(String employeeId, String typeName);

    void deleteById(Long id);
}