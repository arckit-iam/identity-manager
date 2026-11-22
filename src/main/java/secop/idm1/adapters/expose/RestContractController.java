// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import secop.idm1.domain.service.ContractManager;
import secop.idm1.domain.service.IdentityManager;

@RestController
public class RestContractController {

    private final ContractManager contractManager;
    private final IdentityManager identityManager;
    private final DtoMapper mapper;

    @Autowired
    public RestContractController(ContractManager contractManager, IdentityManager identityManager,
            DtoMapper mapper) {
        this.contractManager = contractManager;
        this.identityManager = identityManager;
        this.mapper = mapper;
    }

    @GetMapping("/contract-types")
    public List<ContractTypeDto> getContractTypes() {
        return contractManager.getContractTypes().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/contracts")
    public List<ContractDto> getContracts(@RequestParam(required = false) String identity,
            @RequestParam(required = false) String type) {
        return contractManager.getContracts(identity, type).stream().map(mapper::toDto).toList();
    }

    @GetMapping("/identities/{employeeId}/contracts")
    public List<ContractDto> getIdentityContracts(@PathVariable String employeeId) {
        identityManager.getIdentity(employeeId);
        return contractManager.getContracts(employeeId, null).stream().map(mapper::toDto).toList();
    }

    @PostMapping("/identities/{employeeId}/contracts")
    public ResponseEntity<ContractDto> createContract(@PathVariable String employeeId,
            @RequestBody ContractRequest request) {
        ContractDto created = mapper.toDto(contractManager.createContract(employeeId, request.type(),
                request.startDate(), request.endDate(), request.metadata()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/identities/{employeeId}/contracts/{contractId}")
    public ContractDto updateContract(@PathVariable String employeeId, @PathVariable Long contractId,
            @RequestBody ContractRequest request) {
        return mapper.toDto(contractManager.updateContract(employeeId, contractId, request.type(),
                request.startDate(), request.endDate(), request.metadata()));
    }

    @DeleteMapping("/identities/{employeeId}/contracts/{contractId}")
    public ResponseEntity<Void> deleteContract(@PathVariable String employeeId, @PathVariable Long contractId) {
        contractManager.deleteContract(employeeId, contractId);
        return ResponseEntity.noContent().build();
    }
}