// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import secop.idm1.domain.model.AccountOrder;
import secop.idm1.domain.model.AccountSource;
import secop.idm1.domain.model.AccountType;
import secop.idm1.domain.model.MetadataField;
import secop.idm1.domain.model.MetadataType;
import secop.idm1.domain.port.AccountProvider;
import secop.idm1.domain.port.AccountTypeCatalog;
import secop.idm1.domain.port.IdentityProvider;
import secop.idm1.domain.port.OrderPublisher;
import secop.idm1.domain.service.OrderSchedule;

class HexaAccountServiceTest {

    private final OrderSchedule schedule = new OrderSchedule(
            Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC), ZoneOffset.UTC);

    private AccountProvider accounts;
    private IdentityProvider identities;
    private OrderPublisher publisher;
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
        publisher = mock(OrderPublisher.class);
        service = new HexaAccountService(accounts, identities, catalog(List.of(serviceType, userType)), publisher, schedule);
        when(accounts.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
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
        assertNotNull(created.getOrderId());
        assertNull(created.getEndOrderId());
        verify(publisher).submit(argThat(o -> o.id().equals(created.getOrderId())
                && o.action() == AccountOrder.Action.CREATE && o.accountType().equals("employee")
                && o.identifier().equals("e1234-adm") && !o.executeAt().isAfter(OffsetDateTime.now())));
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
        verify(publisher, never()).submit(any());
    }

    @Test
    void createNeedsAnExistingIdentity() {
        AccountType free = new AccountType("free", AccountKind.USER, Pattern.compile("free-.*"), List.of());
        HexaAccountService other = new HexaAccountService(accounts, identities, catalog(List.of(free, userType)),
                publisher, schedule);

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

        verify(publisher).submit(argThat(o -> o.action() == AccountOrder.Action.DELETE
                && o.identifier().equals(account.getIdentifier()) && o.correlationId().equals("account:e1234:delete")));
        verify(accounts).deleteByIdentifier("e1234");
        assertThrows(NotFoundException.class, () -> service.deleteAccount("absent"));
    }

    private static final java.time.LocalDate TODAY = java.time.LocalDate.of(2026, 6, 15);

    private Account scheduled(java.time.LocalDate start, java.time.LocalDate end) {
        Account account = stored("e1234", "e1234");
        account.setStartDate(start);
        account.setEndDate(end);
        account.setOrderId(UUID.randomUUID());
        if (end != null) {
            account.setEndOrderId(UUID.randomUUID());
        }
        return account;
    }

    @Test
    void createsAnAccountWithAFutureStartAndAnEndDate() {
        Account created = service.createAccount("e1234-adm", null, null, Map.of("department", "IT"),
                TODAY.plusDays(10), TODAY.plusDays(20));

        assertNotNull(created.getOrderId());
        assertNotNull(created.getEndOrderId());
        verify(publisher).submit(argThat(o -> o.id().equals(created.getOrderId())
                && o.action() == AccountOrder.Action.CREATE
                && o.executeAt().toInstant().equals(Instant.parse("2026-06-25T00:00:00Z"))
                && o.correlationId().equals("account:e1234-adm")));
        verify(publisher).submit(argThat(o -> o.id().equals(created.getEndOrderId())
                && o.action() == AccountOrder.Action.DELETE
                && o.executeAt().toInstant().equals(Instant.parse("2026-07-06T00:00:00Z"))
                && o.correlationId().equals("account:e1234-adm:end")));
    }

    @Test
    void rejectsInconsistentDates() {
        Map<String, Object> metadata = Map.of("department", "IT");

        assertThrows(InvalidAccountException.class, () -> service.createAccount("e1234-adm", null, null, metadata,
                TODAY.plusDays(5), TODAY.plusDays(2)));
        assertThrows(InvalidAccountException.class, () -> service.createAccount("e1234-adm", null, null, metadata,
                null, TODAY.minusDays(1)));
        verify(publisher, never()).submit(any());
    }

    @Test
    void cancelsTheCreationOrderWhenTheEndOrderCannotBePublished() {
        org.mockito.Mockito.doNothing().doThrow(new IllegalStateException("down")).when(publisher).submit(any());

        assertThrows(IllegalStateException.class, () -> service.createAccount("e1234-adm", null, null,
                Map.of("department", "IT"), null, TODAY.plusDays(3)));

        verify(publisher).cancel(any(UUID.class));
        verify(accounts, never()).save(any());
    }

    @Test
    void deletingAnAccountNotYetCreatedOnlyCancelsItsOrders() {
        Account account = scheduled(TODAY.plusDays(5), TODAY.plusDays(30));

        service.deleteAccount("e1234");

        verify(publisher).cancel(account.getOrderId());
        verify(publisher).cancel(account.getEndOrderId());
        verify(publisher, never()).submit(any());
        verify(accounts).deleteByIdentifier("e1234");
    }

    @Test
    void deletingAnActiveAccountCancelsItsScheduledEndAndDeletesItNow() {
        Account account = scheduled(TODAY.minusDays(5), TODAY.plusDays(30));

        service.deleteAccount("e1234");

        verify(publisher).cancel(account.getEndOrderId());
        verify(publisher).submit(argThat(o -> o.action() == AccountOrder.Action.DELETE));
        verify(accounts).deleteByIdentifier("e1234");
    }

    @Test
    void deletingAnExpiredAccountDoesNotDeleteItAgain() {
        scheduled(TODAY.minusDays(30), TODAY.minusDays(1));

        service.deleteAccount("e1234");

        verify(publisher, never()).submit(any());
        verify(accounts).deleteByIdentifier("e1234");
    }

    @Test
    void reschedulesAnAccountNotYetCreated() {
        Account account = scheduled(TODAY.plusDays(5), TODAY.plusDays(30));
        UUID oldCreate = account.getOrderId();
        UUID oldEnd = account.getEndOrderId();

        Account updated = service.updateDates("e1234", TODAY.plusDays(7), TODAY.plusDays(40));

        verify(publisher).cancel(oldCreate);
        verify(publisher).cancel(oldEnd);
        verify(publisher, org.mockito.Mockito.times(2)).submit(any());
        assertNotEquals(oldCreate, updated.getOrderId());
        assertNotEquals(oldEnd, updated.getEndOrderId());
        assertEquals(TODAY.plusDays(7), updated.getStartDate());
        assertEquals(TODAY.plusDays(40), updated.getEndDate());
    }

    @Test
    void changingOnlyTheEndDateOfAnActiveAccountKeepsItsCreationOrder() {
        Account account = scheduled(TODAY.minusDays(5), null);
        UUID create = account.getOrderId();

        Account updated = service.updateDates("e1234", TODAY.minusDays(5), TODAY.plusDays(10));

        assertEquals(create, updated.getOrderId());
        assertNotNull(updated.getEndOrderId());
        verify(publisher, never()).cancel(any());
        verify(publisher).submit(argThat(o -> o.action() == AccountOrder.Action.DELETE));
    }

    @Test
    void removingTheEndDateCancelsTheScheduledEnd() {
        Account account = scheduled(null, TODAY.plusDays(10));
        UUID end = account.getEndOrderId();

        Account updated = service.updateDates("e1234", null, null);

        verify(publisher).cancel(end);
        verify(publisher, never()).submit(any());
        assertNull(updated.getEndOrderId());
        assertNull(updated.getEndDate());
    }

    @Test
    void refusesDateChangesThatCannotBeApplied() {
        scheduled(TODAY.minusDays(5), TODAY.plusDays(10));
        assertThrows(InvalidAccountException.class, () -> service.updateDates("e1234", TODAY, TODAY.plusDays(10)));

        scheduled(TODAY.minusDays(30), TODAY.minusDays(1));
        assertThrows(AccountConflictException.class, () -> service.updateDates("e1234", null, null));

        assertThrows(NotFoundException.class, () -> service.updateDates("absent", null, null));
        verify(publisher, never()).submit(any());
    }

    @Test
    void cancelPendingCreationRemovesTheAccountThatWasNeverCreated() {
        Account account = scheduled(TODAY.plusDays(5), TODAY.plusDays(30));

        service.cancelPending("e1234");

        verify(publisher).cancel(account.getOrderId());
        verify(publisher).cancel(account.getEndOrderId());
        verify(accounts).deleteByIdentifier("e1234");
    }

    @Test
    void cancelPendingEndKeepsTheAccountWithoutExpiry() {
        Account account = scheduled(TODAY.minusDays(5), TODAY.plusDays(30));
        UUID end = account.getEndOrderId();

        service.cancelPending("e1234");

        verify(publisher).cancel(end);
        assertNull(account.getEndDate());
        assertNull(account.getEndOrderId());
        verify(accounts).save(account);
        verify(accounts, never()).deleteByIdentifier("e1234");
    }

    @Test
    void cancelPendingFailsWhenNothingIsPending() {
        scheduled(TODAY.minusDays(5), null);
        assertThrows(AccountConflictException.class, () -> service.cancelPending("e1234"));

        scheduled(TODAY.minusDays(30), TODAY.minusDays(1));
        assertThrows(AccountConflictException.class, () -> service.cancelPending("e1234"));
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
        verify(publisher, never()).submit(any());
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
