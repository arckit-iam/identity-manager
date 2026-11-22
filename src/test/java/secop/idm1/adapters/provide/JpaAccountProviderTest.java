// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Account;
import secop.idm1.domain.model.AccountSource;

class JpaAccountProviderTest {

    private JpaAccountRepository accounts;
    private JpaIdentityRepository identities;
    private JpaAccountProvider provider;

    @BeforeEach
    void setUp() {
        accounts = mock(JpaAccountRepository.class);
        identities = mock(JpaIdentityRepository.class);
        provider = new JpaAccountProvider(accounts, identities);
        when(accounts.saveAndFlush(any(AccountEntity.class))).thenAnswer(i -> i.getArgument(0));
    }

    private IdentityEntity identity(Long id, String employeeId) {
        IdentityEntity identity = new IdentityEntity();
        identity.setId(id);
        identity.setEmployeeId(employeeId);
        when(identities.findByEmployeeId(employeeId)).thenReturn(Optional.of(identity));
        when(identities.findById(id)).thenReturn(Optional.of(identity));
        return identity;
    }

    private Account account(String identity) {
        Account account = new Account();
        account.setIdentifier("e1234");
        account.setType("employee");
        account.setIdentity(identity);
        account.setSource(AccountSource.API);
        account.setMetadata(new HashMap<>(Map.of("department", "IT")));
        account.setOrderId(UUID.randomUUID());
        account.setEndOrderId(UUID.randomUUID());
        account.setStartDate(java.time.LocalDate.of(2026, 7, 1));
        account.setEndDate(java.time.LocalDate.of(2026, 12, 31));
        return account;
    }

    @Test
    void savesANewAccountLinkedToItsIdentity() {
        identity(7L, "e1234");
        when(accounts.findByIdentifier("e1234")).thenReturn(Optional.empty());

        Account saved = provider.save(account("e1234"));

        assertEquals("e1234", saved.getIdentity());
        assertEquals(Map.of("department", "IT"), saved.getMetadata());
        assertEquals(AccountSource.API, saved.getSource());
        assertTrue(saved.getCreatedAt() != null);
    }

    @Test
    void savesAnUncorrelatedAccountAndKeepsTheCreationDate() {
        AccountEntity existing = new AccountEntity();
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        existing.setCreatedAt(created);
        when(accounts.findByIdentifier("e1234")).thenReturn(Optional.of(existing));
        Account input = account(null);
        input.setMetadata(null);

        Account saved = provider.save(input);

        assertNull(saved.getIdentity());
        assertEquals(Map.of(), saved.getMetadata());
        assertEquals(created, saved.getCreatedAt());
    }

    @Test
    void usesTheDomainCreationDateForNewAccounts() {
        when(accounts.findByIdentifier("e1234")).thenReturn(Optional.empty());
        Account input = account(null);
        OffsetDateTime created = OffsetDateTime.now().minusDays(2);
        input.setCreatedAt(created);

        assertEquals(created, provider.save(input).getCreatedAt());
    }

    @Test
    void failsOnAnUnknownIdentity() {
        when(identities.findByEmployeeId("ghost")).thenReturn(Optional.empty());
        when(accounts.findByIdentifier("e1234")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> provider.save(account("ghost")));
    }

    @Test
    void readsAnAccountWithItsEmployeeId() {
        identity(7L, "e1234");
        AccountEntity entity = new AccountEntity();
        entity.setIdentifier("e1234");
        entity.setType("employee");
        entity.setIdentityId(7L);
        entity.setSource("DISCOVERED");
        entity.setMetadata(null);
        when(accounts.findByIdentifier("e1234")).thenReturn(Optional.of(entity));
        when(accounts.existsByIdentifier("e1234")).thenReturn(true);

        Account found = provider.findByIdentifier("e1234").orElseThrow();

        assertEquals("e1234", found.getIdentity());
        assertEquals(AccountSource.DISCOVERED, found.getSource());
        assertEquals(Map.of(), found.getMetadata());
        assertTrue(provider.existsByIdentifier("e1234"));
        assertTrue(provider.findByIdentifier("absent").isEmpty());
    }

    @Test
    void readsAnAccountWhoseIdentityIsGone() {
        AccountEntity entity = new AccountEntity();
        entity.setIdentifier("e1234");
        entity.setType("employee");
        entity.setIdentityId(99L);
        entity.setSource("API");
        when(identities.findById(99L)).thenReturn(Optional.empty());
        when(accounts.findByIdentifier("e1234")).thenReturn(Optional.of(entity));

        assertNull(provider.findByIdentifier("e1234").orElseThrow().getIdentity());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listsWithFilters() {
        identity(7L, "e1234");
        AccountEntity entity = new AccountEntity();
        entity.setIdentifier("e1234");
        entity.setType("employee");
        entity.setSource("API");
        when(accounts.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(entity));

        assertEquals(1, provider.findAll(null, null, false).size());
        assertEquals(1, provider.findAll("e1234", "employee", true).size());
    }

    @Test
    void listingAnUnknownIdentityGivesNothing() {
        when(identities.findByEmployeeId("ghost")).thenReturn(Optional.empty());

        assertEquals(List.of(), provider.findAll("ghost", null, false));
    }

    @Test
    void deletes() {
        provider.deleteByIdentifier("e1234");

        verify(accounts).deleteByIdentifier("e1234");
    }
}
