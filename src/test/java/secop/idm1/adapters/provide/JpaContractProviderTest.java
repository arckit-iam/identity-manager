// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Contract;

class JpaContractProviderTest {

    private JpaContractRepository contracts;
    private JpaIdentityRepository identities;
    private JpaContractProvider provider;

    @BeforeEach
    void setUp() {
        contracts = mock(JpaContractRepository.class);
        identities = mock(JpaIdentityRepository.class);
        provider = new JpaContractProvider(contracts, identities);
        when(contracts.saveAndFlush(any(ContractEntity.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void identity(Long id, String employeeId) {
        IdentityEntity identity = new IdentityEntity();
        identity.setId(id);
        identity.setEmployeeId(employeeId);
        when(identities.findByEmployeeId(employeeId)).thenReturn(Optional.of(identity));
        when(identities.findById(id)).thenReturn(Optional.of(identity));
    }

    private static Contract contract() {
        Contract contract = new Contract();
        contract.setEmployeeId("I000001");
        contract.setType("contractor");
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2026, 12, 31));
        contract.setMetadata(new HashMap<>(Map.of("company", "Acme")));
        return contract;
    }

    @Test
    void savesANewContractLinkedToItsIdentity() {
        identity(7L, "I000001");

        Contract saved = provider.save(contract());

        assertEquals("I000001", saved.getEmployeeId());
        assertEquals("contractor", saved.getType());
        assertEquals(LocalDate.of(2026, 12, 31), saved.getEndDate());
        assertEquals("Acme", saved.getMetadata().get("company"));
        assertTrue(saved.getCreatedAt() != null);
    }

    @Test
    void savingForAnUnknownIdentityFails() {
        when(identities.findByEmployeeId("absent")).thenReturn(Optional.empty());
        Contract contract = contract();
        contract.setEmployeeId("absent");

        assertThrows(NotFoundException.class, () -> provider.save(contract));
    }

    @Test
    void updatesAnExistingContractInPlace() {
        identity(7L, "I000001");
        ContractEntity entity = new ContractEntity();
        entity.setId(3L);
        entity.setCreatedAt(java.time.OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        when(contracts.findById(3L)).thenReturn(Optional.of(entity));
        Contract contract = contract();
        contract.setId(3L);

        Contract saved = provider.save(contract);

        assertEquals(3L, saved.getId());
        assertEquals(java.time.OffsetDateTime.parse("2026-01-01T00:00:00Z"), saved.getCreatedAt());
    }

    @Test
    void findsByIdAndFilters() {
        identity(7L, "I000001");
        ContractEntity entity = new ContractEntity();
        entity.setId(3L);
        entity.setIdentityId(7L);
        entity.setType("contractor");
        entity.setMetadata(null);
        when(contracts.findById(3L)).thenReturn(Optional.of(entity));
        when(contracts.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(entity));

        assertEquals("I000001", provider.findById(3L).orElseThrow().getEmployeeId());
        assertTrue(provider.findById(3L).orElseThrow().getMetadata().isEmpty());
        assertEquals(1, provider.findAll("I000001", "contractor").size());
        assertEquals(1, provider.findAll(null, null).size());
        when(identities.findByEmployeeId("absent")).thenReturn(Optional.empty());
        assertTrue(provider.findAll("absent", null).isEmpty());
    }

    @Test
    void deletesById() {
        provider.deleteById(3L);

        verify(contracts).deleteById(3L);
    }
}