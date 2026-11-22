// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import secop.idm1.domain.exception.InvalidAccountException;
import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Contract;
import secop.idm1.domain.model.ContractType;
import secop.idm1.domain.model.MetadataField;
import secop.idm1.domain.model.MetadataType;
import secop.idm1.domain.port.ContractProvider;
import secop.idm1.domain.port.ContractTypeCatalog;
import secop.idm1.domain.port.IdentityProvider;

class HexaContractServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private ContractProvider contracts;
    private IdentityProvider identities;
    private ContractTypeCatalog catalog;
    private HexaContractService service;

    @BeforeEach
    void setUp() {
        contracts = mock(ContractProvider.class);
        identities = mock(IdentityProvider.class);
        catalog = mock(ContractTypeCatalog.class);
        service = new HexaContractService(contracts, identities, catalog);
        ContractType contractor = new ContractType("contractor",
                List.of(new MetadataField("company", MetadataType.STRING, true)));
        when(catalog.findAll()).thenReturn(List.of(contractor));
        when(catalog.findByName("contractor")).thenReturn(Optional.of(contractor));
        when(identities.existsByEmployeeId("I000001")).thenReturn(true);
        when(contracts.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Contract stored(Long id, String employeeId) {
        Contract contract = new Contract();
        contract.setId(id);
        contract.setEmployeeId(employeeId);
        contract.setType("contractor");
        contract.setStartDate(START);
        return contract;
    }

    @Test
    void exposesTheConfiguredTypesAndFiltersTheContracts() {
        when(contracts.findAll("I000001", "contractor")).thenReturn(List.of(stored(1L, "I000001")));

        assertEquals("contractor", service.getContractTypes().get(0).name());
        assertEquals(1, service.getContracts("I000001", "contractor").size());
    }

    @Test
    void createsAContractForAnExistingIdentity() {
        Contract created = service.createContract("I000001", "contractor", START, null, Map.of("company", "Acme"));

        assertEquals("I000001", created.getEmployeeId());
        assertEquals("contractor", created.getType());
        assertEquals("Acme", created.getMetadata().get("company"));
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void createRejectsAnUnknownIdentityTypeOrInvalidContent() {
        assertThrows(NotFoundException.class,
                () -> service.createContract("absent", "contractor", START, null, Map.of("company", "Acme")));
        assertThrows(InvalidAccountException.class,
                () -> service.createContract("I000001", "unknown", START, null, Map.of()));
        assertThrows(InvalidAccountException.class,
                () -> service.createContract("I000001", "contractor", null, null, Map.of("company", "Acme")));
        assertThrows(InvalidAccountException.class, () -> service.createContract("I000001", "contractor", START,
                START.minusDays(1), Map.of("company", "Acme")));
        assertThrows(InvalidAccountException.class,
                () -> service.createContract("I000001", "contractor", START, null, Map.of()));
        verify(contracts, never()).save(any());
    }

    @Test
    void updatesTheContractOfTheIdentity() {
        when(contracts.findById(5L)).thenReturn(Optional.of(stored(5L, "I000001")));

        Contract updated = service.updateContract("I000001", 5L, "contractor", START,
                START.plusYears(1), Map.of("company", "Globex"));

        assertEquals(START.plusYears(1), updated.getEndDate());
        assertEquals("Globex", updated.getMetadata().get("company"));
    }

    @Test
    void aContractOfAnotherIdentityOrAbsentIs404() {
        when(contracts.findById(5L)).thenReturn(Optional.of(stored(5L, "I000002")));

        assertThrows(NotFoundException.class,
                () -> service.updateContract("I000001", 5L, "contractor", START, null, Map.of("company", "x")));
        assertThrows(NotFoundException.class, () -> service.deleteContract("I000001", 5L));
        assertThrows(NotFoundException.class, () -> service.deleteContract("I000001", 9L));
        assertThrows(NotFoundException.class, () -> service.deleteContract("absent", 5L));
    }

    @Test
    void deletesTheContractOfTheIdentity() {
        when(contracts.findById(5L)).thenReturn(Optional.of(stored(5L, "I000001")));

        service.deleteContract("I000001", 5L);

        verify(contracts).deleteById(5L);
    }
}