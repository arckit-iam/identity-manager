// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import secop.idm1.domain.model.MetadataType;

/** Types de contrat : {@code arc.contracts.types[i].name} et leurs champs de métadonnées. */
@Data
@ConfigurationProperties(prefix = "arc.contracts")
public class ContractTypeProperties {

    private List<Type> types = new ArrayList<>();

    @Data
    public static class Type {
        private String name;
        private List<Field> metadata = new ArrayList<>();
    }

    @Data
    public static class Field {
        private String name;
        private MetadataType type = MetadataType.STRING;
        private boolean required;
    }
}