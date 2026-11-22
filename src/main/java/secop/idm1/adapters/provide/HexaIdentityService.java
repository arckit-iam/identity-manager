// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import secop.idm1.domain.exception.IdentityConflictException;
import secop.idm1.domain.exception.InvalidAccountException;
import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Identity;
import secop.idm1.domain.port.IdentityProvider;
import secop.idm1.domain.service.IdentityManager;

@Service
public class HexaIdentityService implements IdentityManager {

    private final IdentityProvider identityProvider;

    @Autowired
    HexaIdentityService(IdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
    }

    @Override
    public List<Identity> getIdentities() {
        return identityProvider.findAll();
    }

    @Override
    public Identity getIdentity(String employeeId) {
        return identityProvider.findByEmployeeId(employeeId)
                .orElseThrow(() -> new NotFoundException("The identity does not exist: " + employeeId));
    }

    @Override
    public Identity createIdentity(Identity identity) {
        if (identity.getEmployeeId() == null || identity.getEmployeeId().isBlank()) {
            throw new InvalidAccountException("The employee id is required");
        }
        if (identityProvider.existsByEmployeeId(identity.getEmployeeId())) {
            throw new IdentityConflictException("The identity already exists: " + identity.getEmployeeId());
        }
        identity.setCreatedAt(OffsetDateTime.now());
        return identityProvider.save(identity);
    }

    @Override
    public Identity updateIdentity(String employeeId, Identity identity) {
        Identity existing = getIdentity(employeeId);
        existing.setFirstName(identity.getFirstName());
        existing.setLastName(identity.getLastName());
        existing.setEmail(identity.getEmail());
        return identityProvider.save(existing);
    }

    @Override
    public void deleteIdentity(String employeeId) {
        getIdentity(employeeId);
        identityProvider.deleteByEmployeeId(employeeId);
    }
}
