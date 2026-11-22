// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

import java.util.List;
import java.util.Map;

import secop.idm1.domain.exception.InvalidAccountException;

/** Contrôle des métadonnées libres d'un compte ou d'un contrat par rapport aux champs déclarés par son type. */
final class MetadataValidator {

    private MetadataValidator() {
    }

    static void validate(String typeName, List<MetadataField> fields, Map<String, Object> values) {
        Map<String, Object> actual = values == null ? Map.of() : values;
        for (String key : actual.keySet()) {
            if (fields.stream().noneMatch(f -> f.name().equals(key))) {
                throw new InvalidAccountException("Unknown metadata '" + key + "' for type " + typeName);
            }
        }
        for (MetadataField field : fields) {
            Object value = actual.get(field.name());
            if (value == null) {
                if (field.required()) {
                    throw new InvalidAccountException("Metadata '" + field.name() + "' is required for type " + typeName);
                }
            } else if (!field.type().accepts(value)) {
                throw new InvalidAccountException(
                        "Metadata '" + field.name() + "' must be of type " + field.type());
            }
        }
    }
}