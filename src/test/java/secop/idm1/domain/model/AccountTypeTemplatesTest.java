// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class AccountTypeTemplatesTest {

    private final AccountType standard = new AccountType("standard", AccountKind.USER,
            Pattern.compile("(?<identity>I\\d{6})"), List.of(), null, "{identity}");
    private final AccountType admin = new AccountType("admin", AccountKind.USER,
            Pattern.compile("A(?<number>\\d{6})"), List.of(), "I{number}", "A{number}");

    @Test
    void standardAccountsShareTheEmployeeIdFormat() {
        assertEquals(Optional.of("I123456"), standard.match("I123456").orElseThrow().identity());
        assertEquals(Optional.empty(), standard.match("A123456"));
        assertEquals(Optional.empty(), standard.match("I12345"));
        assertEquals(Optional.of("I123456"), standard.identifierFor("I123456"));
    }

    @Test
    void adminAccountsAreCorrelatedToTheEmployeeIdWithTheSameNumber() {
        assertEquals(Optional.of("I000042"), admin.match("A000042").orElseThrow().identity());
        assertEquals(Optional.of("A000042"), admin.identifierFor("I000042"));
    }

    @Test
    void withoutIdentityTemplateTheIdentityGroupIsUsedAsIs() {
        AccountType plain = new AccountType("plain", AccountKind.USER, Pattern.compile("(?<identity>[a-z]+)"), List.of());

        assertEquals(Optional.of("alice"), plain.match("alice").orElseThrow().identity());
        assertEquals(Optional.empty(), plain.identifierFor("alice"));
        assertEquals(Optional.empty(), plain.match("alice").orElseThrow().type().identifierFor(null));
    }

    @Test
    void anIdentityTemplateNeedsEveryItsGroupsCaptured() {
        AccountType optional = new AccountType("opt", AccountKind.USER,
                Pattern.compile("A(?<number>\\d{6})?"), List.of(), "I{number}", null);

        assertEquals(Optional.empty(), optional.match("A").orElseThrow().identity());
    }

    @Test
    void templatesAreCheckedAgainstThePattern() {
        Pattern pattern = Pattern.compile("A(?<number>\\d{6})");

        assertThrows(IllegalArgumentException.class,
                () -> new AccountType("bad", AccountKind.USER, pattern, List.of(), "I{unknown}", null));
        assertThrows(IllegalArgumentException.class,
                () -> new AccountType("bad", AccountKind.USER, pattern, List.of(), null, "A{unknown}"));
    }

    @Test
    void blankTemplatesAreIgnored() {
        AccountType type = new AccountType("t", AccountKind.USER, Pattern.compile("(?<identity>x)"), List.of(), " ", "");

        assertEquals(null, type.identityTemplate());
        assertEquals(null, type.identifierTemplate());
    }
}