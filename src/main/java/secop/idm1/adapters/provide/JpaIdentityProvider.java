// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import secop.idm1.domain.model.Identity;
import secop.idm1.domain.port.IdentityProvider;

@Repository
public class JpaIdentityProvider implements IdentityProvider {

    private final JpaIdentityRepository repository;

    @Autowired
    public JpaIdentityProvider(JpaIdentityRepository repository) {
        this.repository = repository;
    }

    @Override
    public Identity save(Identity identity) {
        IdentityEntity entity = repository.findByEmployeeId(identity.getEmployeeId()).orElseGet(IdentityEntity::new);
        entity.setEmployeeId(identity.getEmployeeId());
        entity.setFirstName(identity.getFirstName());
        entity.setLastName(identity.getLastName());
        entity.setEmail(identity.getEmail());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(identity.getCreatedAt() != null ? identity.getCreatedAt() : OffsetDateTime.now());
        }
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public Optional<Identity> findByEmployeeId(String employeeId) {
        return repository.findByEmployeeId(employeeId).map(JpaIdentityProvider::toDomain);
    }

    @Override
    public boolean existsByEmployeeId(String employeeId) {
        return repository.existsByEmployeeId(employeeId);
    }

    @Override
    public List<Identity> findAll() {
        return repository.findAll(Sort.by("employeeId")).stream().map(JpaIdentityProvider::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByEmployeeId(String employeeId) {
        repository.deleteByEmployeeId(employeeId);
    }

    private static Identity toDomain(IdentityEntity entity) {
        Identity identity = new Identity();
        identity.setId(entity.getId());
        identity.setEmployeeId(entity.getEmployeeId());
        identity.setFirstName(entity.getFirstName());
        identity.setLastName(entity.getLastName());
        identity.setEmail(entity.getEmail());
        identity.setCreatedAt(entity.getCreatedAt());
        return identity;
    }
}
