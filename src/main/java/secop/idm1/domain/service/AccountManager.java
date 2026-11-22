// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import secop.idm1.domain.model.Account;
import secop.idm1.domain.model.AccountType;

/** Gestion des comptes utilisateurs et de leur corrélation aux identités humaines. */
public interface AccountManager {

    /** Types de compte utilisateur configurés. */
    List<AccountType> getAccountTypes();

    List<Account> getAccounts(String employeeId, String typeName, boolean uncorrelatedOnly);

    Account getAccount(String identifier);

    /**
     * Crée et provisionne un compte. Le type et l'identité sont déduits de l'identifiant par les regex
     * quand ils ne sont pas fournis. L'identifiant peut être omis si le type sait le générer à partir du matricule
     * de l'identité.
     */
    default Account createAccount(String identifier, String typeName, String employeeId,
            Map<String, Object> metadata) {
        return createAccount(identifier, typeName, employeeId, metadata, null, null);
    }

    /**
     * Comme ci-dessus, avec une validité planifiée : la création est exécutée au début du jour {@code startDate}
     * (immédiatement si {@code null}) et la suppression au début du jour suivant {@code endDate} (jamais si {@code null}).
     */
    Account createAccount(String identifier, String typeName, String employeeId, Map<String, Object> metadata,
            LocalDate startDate, LocalDate endDate);

    /**
     * Change la validité d'un compte : les ordres planifiés non exécutés sont annulés puis émis à nouveau. La date de
     * début ne peut plus changer une fois le compte créé, ni la date de fin une fois le compte expiré.
     */
    Account updateDates(String identifier, LocalDate startDate, LocalDate endDate);

    /**
     * Annule les ordres planifiés pas encore exécutés : si la création est encore à venir, elle est annulée et le compte
     * supprimé ; sinon la suppression planifiée à la date de fin est annulée et le compte n'expire plus.
     */
    void cancelPending(String identifier);

    Account updateMetadata(String identifier, Map<String, Object> metadata);

    /** Rattache manuellement un compte à une identité. */
    Account correlate(String identifier, String employeeId);

    /** Rejoue la corrélation par regex sur les comptes sans identité ; retourne le nombre de comptes rattachés. */
    int correlateAll();

    void deleteAccount(String identifier);

    /**
     * Traite un événement consolidé d'identité : enregistre le compte s'il correspond à un type utilisateur,
     * le supprime si l'événement est une suppression. Les autres comptes sont ignorés.
     */
    void discover(String identifier, String eventType);
}
