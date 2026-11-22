// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import secop.idm1.domain.exception.IdentityConflictException;
import secop.idm1.domain.exception.InvalidAccountException;
import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Identity;
import secop.idm1.domain.port.IdentityProvider;

class HexaIdentityServiceTest {

    private IdentityProvider provider;
    private HexaIdentityService service;

    @BeforeEach
    void setUp() {
        provider = mock(IdentityProvider.class);
        service = new HexaIdentityService(provider);
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
        Identity alice = identity("e1234");
        when(provider.findAll()).thenReturn(List.of(alice));
        when(provider.findByEmployeeId("e1234")).thenReturn(Optional.of(alice));

        assertEquals(List.of(alice), service.getIdentities());
        assertEquals(alice, service.getIdentity("e1234"));
        assertThrows(NotFoundException.class, () -> service.getIdentity("absent"));
    }

    @Test
    void createsAnIdentityWithACreationDate() {
        Identity created = service.createIdentity(identity("e1234"));

        assertEquals("e1234", created.getEmployeeId());
        assertEquals(true, created.getCreatedAt() != null);
    }

    @Test
    void createRejectsBlankAndDuplicateEmployeeIds() {
        when(provider.existsByEmployeeId("e1234")).thenReturn(true);

        assertThrows(InvalidAccountException.class, () -> service.createIdentity(identity(" ")));
        assertThrows(InvalidAccountException.class, () -> service.createIdentity(identity(null)));
        assertThrows(IdentityConflictException.class, () -> service.createIdentity(identity("e1234")));
        verify(provider, never()).save(any());
    }

    @Test
    void updateChangesTheNamesAndTheEmailButNotTheEmployeeId() {
        when(provider.findByEmployeeId("e1234")).thenReturn(Optional.of(identity("e1234")));
        Identity changes = identity("other");
        changes.setLastName("Durand");
        changes.setEmail("a.durand@example.org");

        Identity updated = service.updateIdentity("e1234", changes);

        assertEquals("e1234", updated.getEmployeeId());
        assertEquals("Durand", updated.getLastName());
        assertEquals("a.durand@example.org", updated.getEmail());
        assertThrows(NotFoundException.class, () -> service.updateIdentity("absent", changes));
    }

    @Test
    void deletesAnExistingIdentityOnly() {
        when(provider.findByEmployeeId("e1234")).thenReturn(Optional.of(identity("e1234")));

        service.deleteIdentity("e1234");

        verify(provider).deleteByEmployeeId("e1234");
        assertThrows(NotFoundException.class, () -> service.deleteIdentity("absent"));
    }
}
