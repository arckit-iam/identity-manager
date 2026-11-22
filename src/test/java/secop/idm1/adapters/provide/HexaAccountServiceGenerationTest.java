// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import secop.idm1.domain.model.AccountType;
import secop.idm1.domain.port.AccountProvider;
import secop.idm1.domain.port.AccountTypeCatalog;
import secop.idm1.domain.port.IdentityProvider;
import secop.idm1.domain.port.JobPublisher;

/** Comptes standard (I+6 chiffres) et admin (A+6 chiffres) dont l'identifiant est généré à partir du matricule. */
class HexaAccountServiceGenerationTest {

    private final AccountType standard = new AccountType("standard", AccountKind.USER,
            Pattern.compile("(?<identity>I\\d{6})"), List.of(), null, "{identity}");
    private final AccountType admin = new AccountType("admin", AccountKind.USER,
            Pattern.compile("A(?<number>\\d{6})"), List.of(), "I{number}", "A{number}");
    private final AccountType manual = new AccountType("manual", AccountKind.USER,
            Pattern.compile("(?<identity>[a-z]+)"), List.of());
    private final AccountType technical = new AccountType("service", AccountKind.NON_USER,
            Pattern.compile("svc-[a-z]+"), List.of(), null, "svc-x");

    private AccountProvider accounts;
    private IdentityProvider identities;
    private HexaAccountService service;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountProvider.class);
        identities = mock(IdentityProvider.class);
        JobPublisher publisher = mock(JobPublisher.class);
        List<AccountType> types = List.of(standard, admin, manual, technical);
        service = new HexaAccountService(accounts, identities, new AccountTypeCatalog() {
            @Override
            public List<AccountType> findAll() {
                return types;
            }

            @Override
            public Optional<AccountType> findByName(String name) {
                return types.stream().filter(t -> t.name().equals(name)).findFirst();
            }
        }, publisher);
        when(accounts.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(publisher.publishCreateAccount(any(Account.class))).thenReturn(UUID.randomUUID());
        when(identities.existsByEmployeeId("I000042")).thenReturn(true);
    }

    @Test
    void generatesTheStandardAccountIdentifierFromTheEmployeeId() {
        Account created = service.createAccount(null, "standard", "I000042", Map.of());

        assertEquals("I000042", created.getIdentifier());
        assertEquals("standard", created.getType());
        assertEquals("I000042", created.getIdentity());
    }

    @Test
    void generatesTheAdminAccountIdentifierWithTheSameNumber() {
        Account created = service.createAccount(" ", "admin", "I000042", Map.of());

        assertEquals("A000042", created.getIdentifier());
        assertEquals("I000042", created.getIdentity());
    }

    @Test
    void anExistingGeneratedIdentifierIsAConflict() {
        when(accounts.existsByIdentifier("A000042")).thenReturn(true);

        assertThrows(AccountConflictException.class, () -> service.createAccount(null, "admin", "I000042", Map.of()));
    }

    @Test
    void generationNeedsATypeAnIdentityAndATypeThatCanGenerate() {
        assertThrows(InvalidAccountException.class, () -> service.createAccount(null, null, "I000042", Map.of()));
        assertThrows(InvalidAccountException.class, () -> service.createAccount(null, "admin", null, Map.of()));
        assertThrows(InvalidAccountException.class, () -> service.createAccount(null, "unknown", "I000042", Map.of()));
        assertThrows(InvalidAccountException.class, () -> service.createAccount(null, "service", "I000042", Map.of()));
        assertThrows(InvalidAccountException.class, () -> service.createAccount(null, "manual", "I000042", Map.of()));
        assertThrows(NotFoundException.class, () -> service.createAccount(null, "admin", "I999999", Map.of()));
    }

    @Test
    void anAdminAccountIsCorrelatedWithItsEmployeeIdWhenTyped() {
        Account created = service.createAccount("A000042", null, null, Map.of());

        assertEquals("admin", created.getType());
        assertEquals("I000042", created.getIdentity());
    }
}