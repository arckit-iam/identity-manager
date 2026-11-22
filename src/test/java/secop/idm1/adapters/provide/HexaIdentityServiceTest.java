// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Identity;
import secop.idm1.domain.port.EmployeeIdGenerator;
import secop.idm1.domain.port.IdentityProvider;

class HexaIdentityServiceTest {

    private IdentityProvider provider;
    private EmployeeIdGenerator generator;
    private HexaIdentityService service;

    @BeforeEach
    void setUp() {
        provider = mock(IdentityProvider.class);
        generator = mock(EmployeeIdGenerator.class);
        service = new HexaIdentityService(provider, generator);
        when(provider.save(any(Identity.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Identity identity(String employeeId) {
        Identity identity = new Identity();
        identity.setEmployeeId(employeeId);
        identity.setFirstName("Alice");
        identity.setLastName("Martin");
        identity.setEmail("alice@example.org");
        return identity;
    }

    @Test
    void listsAndFetchesIdentities() {
        Identity alice = identity("I000001");
        when(provider.findAll()).thenReturn(List.of(alice));
        when(provider.findByEmployeeId("I000001")).thenReturn(Optional.of(alice));

        assertEquals(List.of(alice), service.getIdentities());
        assertEquals(alice, service.getIdentity("I000001"));
        assertThrows(NotFoundException.class, () -> service.getIdentity("absent"));
    }

    @Test
    void createsAnIdentityWithAGeneratedEmployeeIdAndACreationDate() {
        when(generator.next()).thenReturn("I000042");

        Identity created = service.createIdentity(identity(null));

        assertEquals("I000042", created.getEmployeeId());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void createIgnoresAnEmployeeIdGivenByTheCaller() {
        when(generator.next()).thenReturn("I000043");

        assertEquals("I000043", service.createIdentity(identity("I999999")).getEmployeeId());
    }

    @Test
    void createSkipsAGeneratedEmployeeIdAlreadyInUse() {
        when(generator.next()).thenReturn("I000001", "I000002");
        when(provider.existsByEmployeeId("I000001")).thenReturn(true);

        assertEquals("I000002", service.createIdentity(identity(null)).getEmployeeId());
    }

    @Test
    void updateChangesTheNamesAndTheEmailButNotTheEmployeeId() {
        when(provider.findByEmployeeId("I000001")).thenReturn(Optional.of(identity("I000001")));
        Identity changes = identity("other");
        changes.setLastName("Durand");
        changes.setEmail("a.durand@example.org");

        Identity updated = service.updateIdentity("I000001", changes);

        assertEquals("I000001", updated.getEmployeeId());
        assertEquals("Durand", updated.getLastName());
        assertEquals("a.durand@example.org", updated.getEmail());
        assertThrows(NotFoundException.class, () -> service.updateIdentity("absent", changes));
    }

    @Test
    void deletesAnExistingIdentityOnly() {
        when(provider.findByEmployeeId("I000001")).thenReturn(Optional.of(identity("I000001")));

        service.deleteIdentity("I000001");

        verify(provider).deleteByEmployeeId("I000001");
        assertThrows(NotFoundException.class, () -> service.deleteIdentity("absent"));
    }
}