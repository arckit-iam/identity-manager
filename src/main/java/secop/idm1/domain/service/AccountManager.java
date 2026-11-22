// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.service;

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
     * quand ils ne sont pas fournis.
     */
    /** L'identifiant peut être omis si le type sait le générer à partir du matricule de l'identité. */
    Account createAccount(String identifier, String typeName, String employeeId, Map<String, Object> metadata);

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
