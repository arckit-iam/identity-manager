// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import secop.idm1.domain.model.Contract;
import secop.idm1.domain.model.ContractType;

/** Gestion des contrats d'une identité (salarié, prestataire...). */
public interface ContractManager {

    List<ContractType> getContractTypes();

    List<Contract> getContracts(String employeeId, String typeName);

    Contract createContract(String employeeId, String typeName, LocalDate startDate, LocalDate endDate,
            Map<String, Object> metadata);

    Contract updateContract(String employeeId, Long contractId, String typeName, LocalDate startDate,
            LocalDate endDate, Map<String, Object> metadata);

    void deleteContract(String employeeId, Long contractId);
}