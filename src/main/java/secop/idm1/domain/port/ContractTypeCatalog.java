// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.port;

import java.util.List;
import java.util.Optional;

import secop.idm1.domain.model.ContractType;

/** Types de contrat connus, définis par la configuration du déploiement. */
public interface ContractTypeCatalog {

    List<ContractType> findAll();

    Optional<ContractType> findByName(String name);
}