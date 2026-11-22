// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import secop.idm1.domain.exception.AccountConflictException;
import secop.idm1.domain.exception.InvalidAccountException;
import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Account;
import secop.idm1.domain.model.AccountKind;
import secop.idm1.domain.model.AccountSource;
import secop.idm1.domain.model.AccountType;
import secop.idm1.domain.model.MetadataField;
import secop.idm1.domain.model.MetadataType;
import secop.idm1.domain.port.AccountProvider;
import secop.idm1.domain.port.AccountTypeCatalog;
import secop.idm1.domain.port.IdentityProvider;
import secop.idm1.domain.port.JobPublisher;

class HexaAccountServiceTest {

    private final UUID jobId = UUID.randomUUID();

    private AccountProvider accounts;
    private IdentityProvider identities;
    private JobPublisher publisher;
    private HexaAccountService service;

    private final AccountType userType = new AccountType("employee", AccountKind.USER,
            Pattern.compile("(?<identity>[a-z]+[0-9]+)(-adm)?"),
            List.of(new MetadataField("department", MetadataType.STRING, true)));
    private final AccountType serviceType = new AccountType("service-account", AccountKind.NON_USER,
            Pattern.compile("svc-(?<product>[a-z0-9]+)-[a-z]+"), List.of());

    private static AccountTypeCatalog catalog(List<AccountType> types) {
        return new AccountTypeCatalog() {
            @Override
            public List<AccountType> findAll() {
                return types;
            }

            @Override
            public Optional<AccountType> findByName(String name) {
                return types.stream().filter(t -> t.name().equals(name)).findFirst();
            }
        };
    }

    @BeforeEach
    void setUp() {
        accounts = mock(AccountProvider.class);
        identities = mock(IdentityProvider.class);
        publisher = mock(JobPublisher.class);
        service = new HexaAccountService(accounts, identities, catalog(List.of(serviceType, userType)), publisher);
        when(accounts.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(publisher.publishCreateAccount(any(Account.class))).thenReturn(jobId);
        when(identities.existsByEmployeeId("e1234")).thenReturn(true);
    }

    private Account stored(String identifier, String identity) {
        Account account = new Account();
        account.setIdentifier(identifier);
        account.setType("employee");
        account.setIdentity(identity);
        account.setSource(AccountSource.DISCOVERED);
        when(accounts.findByIdentifier(identifier)).thenReturn(Optional.of(account));
        return account;
    }

    @Test
    void onlyUserTypesAreExposed() {
        assertEquals(List.of(userType), service.getAccountTypes());
    }

    @Test
    void listsAndFetchesAccounts() {
        Account account = stored("e1234", "e1234");
        when(accounts.findAll("e1234", null, false)).thenReturn(List.of(account));

        assertEquals(List.of(account), service.getAccounts("e1234", null, false));
        assertEquals(account, service.getAccount("e1234"));
        assertThrows(NotFoundException.class, () -> service.getAccount("absent"));
    }

    @Test
    void createsAnAccountDeducingTypeAndIdentityFromTheRegex() {
        Account created = service.createAccount("e1234-adm", null, null, Map.of("department", "IT"));

        assertEquals("employee", created.getType());
        assertEquals("e1234", created.getIdentity());
        assertEquals(AccountSource.API, created.getSource());
        assertEquals(jobId, created.getJobId());
        verify(publisher).publishCreateAccount(any(Account.class));
    }

    @Test
    void explicitIdentityAndTypeWin() {
        when(identities.existsByEmployeeId("e9999")).thenReturn(true);

        Account created = service.createAccount("e1234", "employee", "e9999", Map.of("department", "IT"));

        assertEquals("e9999", created.getIdentity());
    }

    @Test
    void createRejectsInconsistentRequests() {
        assertThrows(InvalidAccountException.class, () -> service.createAccount(" ", null, null, Map.of()));
        assertThrows(InvalidAccountException.class,
                () -> service.createAccount("zzz", null, "e1234", Map.of("department", "IT")));
        assertThrows(InvalidAccountException.class,
                () -> service.createAccount("e1234", "nope", null, Map.of("department", "IT")));
        assertThrows(InvalidAccountException.class,
                () -> service.createAccount("svc-compta-batch", "service-account", null, Map.of()));
        assertThrows(InvalidAccountException.class,
                () -> service.createAccount("zzz", "employee", "e1234", Map.of("department", "IT")));
        assertThrows(InvalidAccountException.class, () -> service.createAccount("e1234", null, null, Map.of()));
        verify(publisher, never()).publishCreateAccount(any());
    }

    @Test
    void createNeedsAnExistingIdentity() {
        AccountType free = new AccountType("free", AccountKind.USER, Pattern.compile("free-.*"), List.of());
        HexaAccountService other = new HexaAccountService(accounts, identities, catalog(List.of(free, userType)),
                publisher);

        assertThrows(InvalidAccountException.class, () -> other.createAccount("free-x", null, null, null));
        assertThrows(NotFoundException.class,
                () -> other.createAccount("z9999", null, null, Map.of("department", "IT")));
        assertThrows(NotFoundException.class, () -> other.createAccount("free-x", null, "ghost", null));
    }

    @Test
    void createRejectsDuplicates() {
        when(accounts.existsByIdentifier("e1234")).thenReturn(true);

        assertThrows(AccountConflictException.class,
                () -> service.createAccount("e1234", null, null, Map.of("department", "IT")));
    }

    @Test
    void updatesMetadataAfterValidation() {
        Account account = stored("e1234", "e1234");

        Account updated = service.updateMetadata("e1234", Map.of("department", "HR"));

        assertEquals(Map.of("department", "HR"), updated.getMetadata());
        assertEquals(account, updated);
        assertThrows(InvalidAccountException.class, () -> service.updateMetadata("e1234", Map.of()));
        assertThrows(InvalidAccountException.class, () -> service.updateMetadata("e1234", Map.of("department", 1)));
    }

    @Test
    void updateMetadataFailsWhenTheTypeIsNoLongerConfigured() {
        Account account = stored("old", null);
        account.setType("removed");

        assertThrows(InvalidAccountException.class, () -> service.updateMetadata("old", Map.of()));
    }

    @Test
    void correlatesManually() {
        stored("x1", null);

        assertEquals("e1234", service.correlate("x1", "e1234").getIdentity());
        assertThrows(NotFoundException.class, () -> service.correlate("x1", "ghost"));
        assertThrows(NotFoundException.class, () -> service.correlate("x1", null));
        assertThrows(NotFoundException.class, () -> service.correlate("absent", "e1234"));
    }

    @Test
    void correlatesAllUncorrelatedAccountsByRegex() {
        Account matching = stored("e1234-adm", null);
        Account unknownIdentity = stored("z9999", null);
        Account unknownType = stored("legacy", null);
        unknownType.setType("removed");
        when(accounts.findAll(null, null, true)).thenReturn(List.of(matching, unknownIdentity, unknownType));

        assertEquals(1, service.correlateAll());
        assertEquals("e1234", matching.getIdentity());
        assertNull(unknownIdentity.getIdentity());
    }

    @Test
    void deletesAnAccountAfterRequestingItsDeprovisioning() {
        Account account = stored("e1234", "e1234");

        service.deleteAccount("e1234");

        verify(publisher).publishDeleteAccount(account);
        verify(accounts).deleteByIdentifier("e1234");
        assertThrows(NotFoundException.class, () -> service.deleteAccount("absent"));
    }

    @Test
    void discoversUserAccountsAndCorrelatesThemWhenTheIdentityExists() {
        service.discover("e1234-adm", "CREATED");
        service.discover("z9999", "CREATED");

        verify(accounts).save(org.mockito.ArgumentMatchers.argThat(a ->
                a.getIdentifier().equals("e1234-adm") && "e1234".equals(a.getIdentity())
                        && a.getSource() == AccountSource.DISCOVERED));
        verify(accounts).save(org.mockito.ArgumentMatchers.argThat(a ->
                a.getIdentifier().equals("z9999") && a.getIdentity() == null));
        verify(publisher, never()).publishCreateAccount(any());
    }

    @Test
    void discoverIgnoresKnownAccountsOtherKindsAndBlankIdentifiers() {
        when(accounts.existsByIdentifier("e1234")).thenReturn(true);

        service.discover("e1234", "CREATED");
        service.discover("svc-compta-batch", "CREATED");
        service.discover(" ", "CREATED");
        service.discover(null, "CREATED");

        verify(accounts, never()).save(any());
    }

    @Test
    void discoverRemovesDeletedAccounts() {
        service.discover("e1234", "deleted");

        verify(accounts).deleteByIdentifier("e1234");
        verify(accounts, never()).save(any());
    }
}
