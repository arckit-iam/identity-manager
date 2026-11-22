// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import secop.idm1.domain.model.MetadataType;

class ConfiguredContractTypeCatalogTest {

    private static ContractTypeProperties bind(Map<String, String> source) {
        return new Binder(new MapConfigurationPropertySource(source))
                .bind("arc.contracts", ContractTypeProperties.class).orElseGet(ContractTypeProperties::new);
    }

    @Test
    void buildsTypesFromConfiguration() {
        ConfiguredContractTypeCatalog catalog = new ConfiguredContractTypeCatalog(bind(Map.of(
                "arc.contracts.types[0].name", "employee",
                "arc.contracts.types[1].name", "contractor",
                "arc.contracts.types[1].metadata[0].name", "company",
                "arc.contracts.types[1].metadata[0].required", "true",
                "arc.contracts.types[1].metadata[1].name", "dailyRate",
                "arc.contracts.types[1].metadata[1].type", "NUMBER")));

        assertEquals(2, catalog.findAll().size());
        var contractor = catalog.findByName("contractor").orElseThrow();
        assertEquals(MetadataType.STRING, contractor.metadata().get(0).type());
        assertTrue(contractor.metadata().get(0).required());
        assertEquals(MetadataType.NUMBER, contractor.metadata().get(1).type());
        assertEquals(Optional.empty(), catalog.findByName("unknown"));
    }

    @Test
    void noConfigurationMeansNoTypes() {
        assertTrue(new ConfiguredContractTypeCatalog(bind(Map.of())).findAll().isEmpty());
    }

    @Test
    void duplicateTypesAreRejected() {
        ContractTypeProperties properties = bind(Map.of(
                "arc.contracts.types[0].name", "employee",
                "arc.contracts.types[1].name", "employee"));

        assertThrows(IllegalArgumentException.class, () -> new ConfiguredContractTypeCatalog(properties));
    }
}