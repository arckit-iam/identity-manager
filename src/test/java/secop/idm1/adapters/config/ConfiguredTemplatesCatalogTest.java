// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import secop.idm1.domain.model.AccountKind;

class ConfiguredTemplatesCatalogTest {

    private static AccountTypeProperties bind(Map<String, String> source) {
        return new Binder(new MapConfigurationPropertySource(source))
                .bind("arc.accounts", AccountTypeProperties.class).orElseGet(AccountTypeProperties::new);
    }

    @Test
    void standardAndAdminTypesAreBuiltFromConfiguration() {
        ConfiguredAccountTypeCatalog catalog = new ConfiguredAccountTypeCatalog(bind(Map.of(
                "arc.accounts.types[0].name", "standard",
                "arc.accounts.types[0].kind", "USER",
                "arc.accounts.types[0].pattern", "(?<identity>I\\d{6})",
                "arc.accounts.types[0].identifier-template", "{identity}",
                "arc.accounts.types[1].name", "admin",
                "arc.accounts.types[1].kind", "USER",
                "arc.accounts.types[1].pattern", "A(?<number>\\d{6})",
                "arc.accounts.types[1].identity-template", "I{number}",
                "arc.accounts.types[1].identifier-template", "A{number}")));

        assertEquals(Optional.of("I000042"), catalog.classify("A000042", AccountKind.USER).orElseThrow().identity());
        assertEquals(Optional.of("I000042"), catalog.classify("I000042", AccountKind.USER).orElseThrow().identity());
        assertEquals(Optional.of("A000042"), catalog.findByName("admin").orElseThrow().identifierFor("I000042"));
    }

    @Test
    void anInvalidTemplateStopsTheStartup() {
        AccountTypeProperties properties = bind(Map.of(
                "arc.accounts.types[0].name", "admin",
                "arc.accounts.types[0].kind", "USER",
                "arc.accounts.types[0].pattern", "A(?<number>\\d{6})",
                "arc.accounts.types[0].identity-template", "I{nope}"));

        assertThrows(IllegalArgumentException.class, () -> new ConfiguredAccountTypeCatalog(properties));
    }
}