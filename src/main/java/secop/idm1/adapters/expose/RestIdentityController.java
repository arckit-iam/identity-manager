// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import secop.idm1.domain.service.AccountManager;
import secop.idm1.domain.service.IdentityManager;

/** API des identités humaines. */
@RestController
public class RestIdentityController {

    private final IdentityManager identityManager;
    private final AccountManager accountManager;
    private final DtoMapper mapper;

    @Autowired
    public RestIdentityController(IdentityManager identityManager, AccountManager accountManager, DtoMapper mapper) {
        this.identityManager = identityManager;
        this.accountManager = accountManager;
        this.mapper = mapper;
    }

    @GetMapping("/identities")
    public List<IdentityDto> getIdentities() {
        return identityManager.getIdentities().stream().map(mapper::toDto).toList();
    }

    @GetMapping("/identities/{employeeId}")
    public IdentityDto getIdentity(@PathVariable String employeeId) {
        return mapper.toDto(identityManager.getIdentity(employeeId));
    }

    @GetMapping("/identities/{employeeId}/accounts")
    public List<AccountDto> getIdentityAccounts(@PathVariable String employeeId) {
        identityManager.getIdentity(employeeId);
        return accountManager.getAccounts(employeeId, null, false).stream().map(mapper::toDto).toList();
    }

    @PostMapping("/identities")
    public ResponseEntity<IdentityDto> createIdentity(@RequestBody IdentityRequest request) {
        IdentityDto created = mapper.toDto(identityManager.createIdentity(mapper.toDomain(request)));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/identities/" + created.employeeId())
                .body(created);
    }

    @PutMapping("/identities/{employeeId}")
    public IdentityDto updateIdentity(@PathVariable String employeeId, @RequestBody IdentityRequest request) {
        return mapper.toDto(identityManager.updateIdentity(employeeId, mapper.toDomain(request)));
    }

    @DeleteMapping("/identities/{employeeId}")
    public ResponseEntity<Void> deleteIdentity(@PathVariable String employeeId) {
        identityManager.deleteIdentity(employeeId);
        return ResponseEntity.noContent().build();
    }
}
