// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import secop.idm1.domain.exception.AccountConflictException;
import secop.idm1.domain.exception.InvalidAccountException;
import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Account;
import secop.idm1.domain.model.AccountKind;
import secop.idm1.domain.model.AccountMatch;
import secop.idm1.domain.model.AccountSource;
import secop.idm1.domain.model.AccountType;
import secop.idm1.domain.port.AccountProvider;
import secop.idm1.domain.port.AccountTypeCatalog;
import secop.idm1.domain.port.IdentityProvider;
import secop.idm1.domain.port.JobPublisher;
import secop.idm1.domain.service.AccountManager;

@Service
public class HexaAccountService implements AccountManager {

    private static final String DELETED = "DELETED";

    private final AccountProvider accountProvider;
    private final IdentityProvider identityProvider;
    private final AccountTypeCatalog catalog;
    private final JobPublisher jobPublisher;

    @Autowired
    HexaAccountService(AccountProvider accountProvider, IdentityProvider identityProvider,
            AccountTypeCatalog catalog, JobPublisher jobPublisher) {
        this.accountProvider = accountProvider;
        this.identityProvider = identityProvider;
        this.catalog = catalog;
        this.jobPublisher = jobPublisher;
    }

    @Override
    public List<AccountType> getAccountTypes() {
        return catalog.findAll().stream().filter(t -> t.kind() == AccountKind.USER).toList();
    }

    @Override
    public List<Account> getAccounts(String employeeId, String typeName, boolean uncorrelatedOnly) {
        return accountProvider.findAll(employeeId, typeName, uncorrelatedOnly);
    }

    @Override
    public Account getAccount(String identifier) {
        return accountProvider.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException("The account does not exist: " + identifier));
    }

    @Override
    public Account createAccount(String identifier, String typeName, String employeeId,
            Map<String, Object> metadata) {
        if (identifier == null || identifier.isBlank()) {
            identifier = generateIdentifier(typeName, employeeId);
        }
        if (accountProvider.existsByIdentifier(identifier)) {
            throw new AccountConflictException("The account already exists: " + identifier);
        }
        AccountMatch match = resolveType(identifier, typeName);
        String identity = employeeId != null ? employeeId : match.identity().orElse(null);
        if (identity == null) {
            throw new InvalidAccountException(
                    "An identity is required: none given and none captured by the type pattern");
        }
        if (!identityProvider.existsByEmployeeId(identity)) {
            throw new NotFoundException("The identity does not exist: " + identity);
        }
        match.type().validateMetadata(metadata);

        Account account = new Account();
        account.setIdentifier(identifier);
        account.setType(match.type().name());
        account.setIdentity(identity);
        account.setSource(AccountSource.API);
        account.setMetadata(metadata == null ? new HashMap<>() : new HashMap<>(metadata));
        account.setJobId(jobPublisher.publishCreateAccount(account));
        account.setCreatedAt(OffsetDateTime.now());
        return accountProvider.save(account);
    }

    private String generateIdentifier(String typeName, String employeeId) {
        if (typeName == null || employeeId == null) {
            throw new InvalidAccountException(
                    "The account identifier is required, unless a type and an identity are given to generate it");
        }
        AccountType type = catalog.findByName(typeName)
                .orElseThrow(() -> new InvalidAccountException("Unknown account type: " + typeName));
        if (type.kind() != AccountKind.USER) {
            throw new InvalidAccountException("Type " + typeName + " is not a user account type");
        }
        if (!identityProvider.existsByEmployeeId(employeeId)) {
            throw new NotFoundException("The identity does not exist: " + employeeId);
        }
        return type.identifierFor(employeeId).orElseThrow(
                () -> new InvalidAccountException("Type " + typeName + " cannot generate an identifier"));
    }

    private AccountMatch resolveType(String identifier, String typeName) {
        if (typeName == null) {
            return catalog.classify(identifier, AccountKind.USER).orElseThrow(
                    () -> new InvalidAccountException("No account type recognizes the identifier " + identifier));
        }
        AccountType type = catalog.findByName(typeName)
                .orElseThrow(() -> new InvalidAccountException("Unknown account type: " + typeName));
        if (type.kind() != AccountKind.USER) {
            throw new InvalidAccountException("Type " + typeName + " is not a user account type");
        }
        return type.match(identifier).orElseThrow(
                () -> new InvalidAccountException("The identifier does not match the pattern of type " + typeName));
    }

    @Override
    public Account updateMetadata(String identifier, Map<String, Object> metadata) {
        Account account = getAccount(identifier);
        AccountType type = catalog.findByName(account.getType())
                .orElseThrow(() -> new InvalidAccountException("Unknown account type: " + account.getType()));
        type.validateMetadata(metadata);
        account.setMetadata(metadata == null ? new HashMap<>() : new HashMap<>(metadata));
        return accountProvider.save(account);
    }

    @Override
    public Account correlate(String identifier, String employeeId) {
        Account account = getAccount(identifier);
        if (employeeId == null || !identityProvider.existsByEmployeeId(employeeId)) {
            throw new NotFoundException("The identity does not exist: " + employeeId);
        }
        account.setIdentity(employeeId);
        return accountProvider.save(account);
    }

    @Override
    public int correlateAll() {
        int correlated = 0;
        for (Account account : accountProvider.findAll(null, null, true)) {
            Optional<String> identity = catalog.findByName(account.getType())
                    .flatMap(type -> type.match(account.getIdentifier()))
                    .flatMap(AccountMatch::identity)
                    .filter(identityProvider::existsByEmployeeId);
            if (identity.isPresent()) {
                account.setIdentity(identity.get());
                accountProvider.save(account);
                correlated++;
            }
        }
        return correlated;
    }

    @Override
    public void deleteAccount(String identifier) {
        Account account = getAccount(identifier);
        jobPublisher.publishDeleteAccount(account);
        accountProvider.deleteByIdentifier(identifier);
    }

    @Override
    public void discover(String identifier, String eventType) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        if (DELETED.equalsIgnoreCase(eventType)) {
            accountProvider.deleteByIdentifier(identifier);
            return;
        }
        if (accountProvider.existsByIdentifier(identifier)) {
            return;
        }
        catalog.classify(identifier, AccountKind.USER).ifPresent(match -> {
            Account account = new Account();
            account.setIdentifier(identifier);
            account.setType(match.type().name());
            account.setIdentity(match.identity().filter(identityProvider::existsByEmployeeId).orElse(null));
            account.setSource(AccountSource.DISCOVERED);
            account.setCreatedAt(OffsetDateTime.now());
            accountProvider.save(account);
        });
    }
}
