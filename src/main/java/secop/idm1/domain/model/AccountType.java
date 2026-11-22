// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import secop.idm1.domain.exception.InvalidAccountException;

/**
 * Type de compte, défini par configuration : il porte l'expression régulière qui reconnaît ses
 * identifiants (le groupe nommé {@code identity} sert à la corrélation à l'identité humaine, {@code product} dans product-manager) et
 * la liste des champs de métadonnées autorisés.
 *
 * <p>Deux gabarits optionnels, de la forme {@code A{number}}, permettent de passer du compte à l'identité et inversement :
 * <ul>
 *   <li>{@code identityTemplate} : matricule de l'identité, construit à partir des groupes nommés capturés par la regex
 *       (sans gabarit, le groupe {@code identity} est utilisé tel quel) ;</li>
 *   <li>{@code identifierTemplate} : identifiant du compte, construit à partir du matricule ({@code {identity}} : le
 *       matricule, {@code {number}} : ses chiffres). Sans gabarit, l'identifiant doit être fourni à la création.</li>
 * </ul>
 */
public record AccountType(String name, AccountKind kind, Pattern pattern, List<MetadataField> metadata,
        String identityTemplate, String identifierTemplate) {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z][A-Za-z0-9]*)}");

    public AccountType {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Account type name is required");
        }
        if (kind == null) {
            throw new IllegalArgumentException("Account type '" + name + "' needs a kind");
        }
        if (pattern == null) {
            throw new IllegalArgumentException("Account type '" + name + "' needs a pattern");
        }
        metadata = metadata == null ? List.of() : List.copyOf(metadata);
        identityTemplate = identityTemplate == null || identityTemplate.isBlank() ? null : identityTemplate;
        identifierTemplate = identifierTemplate == null || identifierTemplate.isBlank() ? null : identifierTemplate;
        if (identityTemplate != null) {
            Matcher placeholders = PLACEHOLDER.matcher(identityTemplate);
            while (placeholders.find()) {
                if (!pattern.namedGroups().containsKey(placeholders.group(1))) {
                    throw new IllegalArgumentException("Account type '" + name + "': the identity template uses '"
                            + placeholders.group(1) + "', which is not a named group of the pattern");
                }
            }
        }
        if (identifierTemplate != null) {
            Matcher placeholders = PLACEHOLDER.matcher(identifierTemplate);
            while (placeholders.find()) {
                if (!List.of("identity", "number").contains(placeholders.group(1))) {
                    throw new IllegalArgumentException("Account type '" + name + "': the identifier template can only"
                            + " use {identity} and {number}, not {" + placeholders.group(1) + "}");
                }
            }
        }
    }

    public AccountType(String name, AccountKind kind, Pattern pattern, List<MetadataField> metadata) {
        this(name, kind, pattern, metadata, null, null);
    }

    /** Reconnaît l'identifiant en entier (la regex doit couvrir tout l'identifiant). */
    public Optional<AccountMatch> match(String identifier) {
        if (identifier == null) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(identifier);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Map<String, String> captures = new HashMap<>();
        pattern.namedGroups().keySet().forEach(group -> {
            String value = matcher.group(group);
            if (value != null && !value.isBlank()) {
                captures.put(group, value);
            }
        });
        return Optional.of(new AccountMatch(this, captures));
    }

    /** Matricule de l'identité déduit des groupes capturés dans l'identifiant du compte. */
    public Optional<String> identityFrom(Map<String, String> captures) {
        if (identityTemplate == null) {
            return Optional.ofNullable(captures.get(AccountMatch.IDENTITY_GROUP));
        }
        return fill(identityTemplate, captures);
    }

    /** Identifiant du compte déduit du matricule, vide si le type ne sait pas le générer. */
    public Optional<String> identifierFor(String employeeId) {
        if (identifierTemplate == null || employeeId == null) {
            return Optional.empty();
        }
        return fill(identifierTemplate, Map.of("identity", employeeId, "number", employeeId.replaceAll("\\D", "")));
    }

    private static Optional<String> fill(String template, Map<String, String> values) {
        Matcher placeholders = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();
        while (placeholders.find()) {
            String value = values.get(placeholders.group(1));
            if (value == null) {
                return Optional.empty();
            }
            placeholders.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        placeholders.appendTail(result);
        return Optional.of(result.toString());
    }

    /**
     * Vérifie les métadonnées : aucun champ inconnu, champs obligatoires présents, valeurs du bon type.
     *
     * @throws InvalidAccountException si les métadonnées ne respectent pas le type
     */
    public void validateMetadata(Map<String, Object> values) {
        MetadataValidator.validate(name, metadata, values);
    }
}