// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.port;

import java.util.List;
import java.util.Optional;

import secop.idm1.domain.model.Identity;

public interface IdentityProvider {

    Identity save(Identity identity);

    Optional<Identity> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    List<Identity> findAll();

    void deleteByEmployeeId(String employeeId);
}
