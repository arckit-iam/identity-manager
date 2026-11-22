// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import secop.idm1.domain.exception.InvalidAccountException;
import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Contract;
import secop.idm1.domain.model.ContractType;
import secop.idm1.domain.port.ContractProvider;
import secop.idm1.domain.port.ContractTypeCatalog;
import secop.idm1.domain.port.IdentityProvider;
import secop.idm1.domain.service.ContractManager;

@Service
public class HexaContractService implements ContractManager {

    private final ContractProvider contractProvider;
    private final IdentityProvider identityProvider;
    private final ContractTypeCatalog catalog;

    @Autowired
    HexaContractService(ContractProvider contractProvider, IdentityProvider identityProvider,
            ContractTypeCatalog catalog) {
        this.contractProvider = contractProvider;
        this.identityProvider = identityProvider;
        this.catalog = catalog;
    }

    @Override
    public List<ContractType> getContractTypes() {
        return catalog.findAll();
    }

    @Override
    public List<Contract> getContracts(String employeeId, String typeName) {
        return contractProvider.findAll(employeeId, typeName);
    }

    @Override
    public Contract createContract(String employeeId, String typeName, LocalDate startDate, LocalDate endDate,
            Map<String, Object> metadata) {
        requireIdentity(employeeId);
        Contract contract = new Contract();
        contract.setEmployeeId(employeeId);
        contract.setCreatedAt(OffsetDateTime.now());
        return contractProvider.save(fill(contract, typeName, startDate, endDate, metadata));
    }

    @Override
    public Contract updateContract(String employeeId, Long contractId, String typeName, LocalDate startDate,
            LocalDate endDate, Map<String, Object> metadata) {
        Contract contract = find(employeeId, contractId);
        return contractProvider.save(fill(contract, typeName, startDate, endDate, metadata));
    }

    @Override
    public void deleteContract(String employeeId, Long contractId) {
        find(employeeId, contractId);
        contractProvider.deleteById(contractId);
    }

    private Contract fill(Contract contract, String typeName, LocalDate startDate, LocalDate endDate,
            Map<String, Object> metadata) {
        ContractType type = catalog.findByName(typeName)
                .orElseThrow(() -> new InvalidAccountException("Unknown contract type: " + typeName));
        if (startDate == null) {
            throw new InvalidAccountException("The contract start date is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidAccountException("The contract end date cannot be before its start date");
        }
        type.validateMetadata(metadata);
        contract.setType(type.name());
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setMetadata(metadata == null ? new HashMap<>() : new HashMap<>(metadata));
        return contract;
    }

    private Contract find(String employeeId, Long contractId) {
        requireIdentity(employeeId);
        return contractProvider.findById(contractId)
                .filter(contract -> employeeId.equals(contract.getEmployeeId()))
                .orElseThrow(() -> new NotFoundException("The contract does not exist: " + contractId));
    }

    private void requireIdentity(String employeeId) {
        if (!identityProvider.existsByEmployeeId(employeeId)) {
            throw new NotFoundException("The identity does not exist: " + employeeId);
        }
    }
}