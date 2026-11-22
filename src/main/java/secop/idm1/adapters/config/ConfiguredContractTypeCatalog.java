// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.config;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import secop.idm1.domain.model.ContractType;
import secop.idm1.domain.model.MetadataField;
import secop.idm1.domain.port.ContractTypeCatalog;

/** Catalogue des types de contrat, construit une fois pour toutes à partir de la configuration. */
@Component
@EnableConfigurationProperties(ContractTypeProperties.class)
public class ConfiguredContractTypeCatalog implements ContractTypeCatalog {

    private final List<ContractType> types;

    public ConfiguredContractTypeCatalog(ContractTypeProperties properties) {
        Set<String> names = new HashSet<>();
        this.types = properties.getTypes().stream().map(type -> {
            if (!names.add(type.getName())) {
                throw new IllegalArgumentException("Duplicate contract type: " + type.getName());
            }
            List<MetadataField> fields = type.getMetadata().stream()
                    .map(f -> new MetadataField(f.getName(), f.getType(), f.isRequired()))
                    .toList();
            return new ContractType(type.getName(), fields);
        }).toList();
    }

    @Override
    public List<ContractType> findAll() {
        return types;
    }

    @Override
    public Optional<ContractType> findByName(String name) {
        return types.stream().filter(t -> t.name().equals(name)).findFirst();
    }
}