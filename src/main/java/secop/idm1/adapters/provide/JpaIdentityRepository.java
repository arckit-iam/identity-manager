// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaIdentityRepository extends JpaRepository<IdentityEntity, Long> {

    Optional<IdentityEntity> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    void deleteByEmployeeId(String employeeId);
}
