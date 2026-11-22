// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Contract;
import secop.idm1.domain.port.ContractProvider;

@Repository
public class JpaContractProvider implements ContractProvider {

    private final JpaContractRepository contractRepository;
    private final JpaIdentityRepository identityRepository;

    @Autowired
    public JpaContractProvider(JpaContractRepository contractRepository, JpaIdentityRepository identityRepository) {
        this.contractRepository = contractRepository;
        this.identityRepository = identityRepository;
    }

    @Override
    public Contract save(Contract contract) {
        ContractEntity entity = contract.getId() == null ? new ContractEntity()
                : contractRepository.findById(contract.getId()).orElseGet(ContractEntity::new);
        entity.setIdentityId(identityRepository.findByEmployeeId(contract.getEmployeeId())
                .orElseThrow(() -> new NotFoundException("The identity does not exist: " + contract.getEmployeeId()))
                .getId());
        entity.setType(contract.getType());
        entity.setStartDate(contract.getStartDate());
        entity.setEndDate(contract.getEndDate());
        entity.setMetadata(contract.getMetadata() == null ? new HashMap<>() : new HashMap<>(contract.getMetadata()));
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(contract.getCreatedAt() != null ? contract.getCreatedAt() : OffsetDateTime.now());
        }
        return toDomain(contractRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<Contract> findById(Long id) {
        return contractRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Contract> findAll(String employeeId, String typeName) {
        Specification<ContractEntity> spec = (root, query, cb) -> cb.conjunction();
        if (employeeId != null) {
            Optional<IdentityEntity> identity = identityRepository.findByEmployeeId(employeeId);
            if (identity.isEmpty()) {
                return List.of();
            }
            Long identityId = identity.get().getId();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("identityId"), identityId));
        }
        if (typeName != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), typeName));
        }
        return contractRepository.findAll(spec, Sort.by("startDate", "id")).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        contractRepository.deleteById(id);
    }

    private Contract toDomain(ContractEntity entity) {
        Contract contract = new Contract();
        contract.setId(entity.getId());
        contract.setEmployeeId(identityRepository.findById(entity.getIdentityId()).map(IdentityEntity::getEmployeeId)
                .orElse(null));
        contract.setType(entity.getType());
        contract.setStartDate(entity.getStartDate());
        contract.setEndDate(entity.getEndDate());
        contract.setMetadata(entity.getMetadata() == null ? new HashMap<>() : new HashMap<>(entity.getMetadata()));
        contract.setCreatedAt(entity.getCreatedAt());
        return contract;
    }
}