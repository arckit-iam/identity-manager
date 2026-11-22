// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.port;

import java.util.UUID;

import secop.idm1.domain.model.Account;

/** Demande le provisioning des comptes auprès d'arc-publisher. */
public interface JobPublisher {

    UUID publishCreateAccount(Account account);

    UUID publishDeleteAccount(Account account);
}
