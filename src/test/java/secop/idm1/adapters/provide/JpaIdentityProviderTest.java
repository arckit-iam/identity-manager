// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import secop.idm1.domain.model.Identity;

class JpaIdentityProviderTest {

    private JpaIdentityRepository repository;
    private JpaIdentityProvider provider;

    @BeforeEach
    void setUp() {
        repository = mock(JpaIdentityRepository.class);
        provider = new JpaIdentityProvider(repository);
        when(repository.saveAndFlush(any(IdentityEntity.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Identity identity() {
        Identity identity = new Identity();
        identity.setEmployeeId("e1234");
        identity.setFirstName("Alice");
        identity.setLastName("Martin");
        identity.setEmail("alice@example.org");
        return identity;
    }

    @Test
    void savesANewIdentity() {
        when(repository.findByEmployeeId("e1234")).thenReturn(Optional.empty());

        Identity saved = provider.save(identity());

        assertEquals("e1234", saved.getEmployeeId());
        assertEquals("Martin", saved.getLastName());
        assertTrue(saved.getCreatedAt() != null);
    }

    @Test
    void keepsTheStoredCreationDateOnUpdate() {
        IdentityEntity existing = new IdentityEntity();
        OffsetDateTime created = OffsetDateTime.now().minusDays(3);
        existing.setCreatedAt(created);
        when(repository.findByEmployeeId("e1234")).thenReturn(Optional.of(existing));

        assertEquals(created, provider.save(identity()).getCreatedAt());
    }

    @Test
    void usesTheDomainCreationDateForNewIdentities() {
        when(repository.findByEmployeeId("e1234")).thenReturn(Optional.empty());
        Identity input = identity();
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        input.setCreatedAt(created);

        assertEquals(created, provider.save(input).getCreatedAt());
    }

    @Test
    void readsListsAndDeletes() {
        IdentityEntity entity = new IdentityEntity();
        entity.setEmployeeId("e1234");
        when(repository.findByEmployeeId("e1234")).thenReturn(Optional.of(entity));
        when(repository.existsByEmployeeId("e1234")).thenReturn(true);
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(entity));

        assertEquals("e1234", provider.findByEmployeeId("e1234").orElseThrow().getEmployeeId());
        assertTrue(provider.findByEmployeeId("absent").isEmpty());
        assertTrue(provider.existsByEmployeeId("e1234"));
        assertEquals(1, provider.findAll().size());
        provider.deleteByEmployeeId("e1234");
        verify(repository).deleteByEmployeeId("e1234");
    }
}
