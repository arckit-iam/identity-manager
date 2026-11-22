// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

import java.util.List;
import java.util.Map;

/** Type de contrat (salarié, prestataire, stagiaire...), défini par configuration, avec ses métadonnées autorisées. */
public record ContractType(String name, List<MetadataField> metadata) {

    public ContractType {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Contract type name is required");
        }
        metadata = metadata == null ? List.of() : List.copyOf(metadata);
    }

    /** @throws secop.idm1.domain.exception.InvalidAccountException si les métadonnées ne respectent pas le type */
    public void validateMetadata(Map<String, Object> values) {
        MetadataValidator.validate(name, metadata, values);
    }
}