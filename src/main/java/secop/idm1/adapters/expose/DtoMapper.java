// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import org.springframework.stereotype.Component;

import secop.idm1.domain.model.Account;
import secop.idm1.domain.model.AccountType;
import secop.idm1.domain.model.Contract;
import secop.idm1.domain.model.ContractType;
import secop.idm1.domain.model.Identity;

@Component
public class DtoMapper {

    public AccountDto toDto(Account account) {
        return new AccountDto(account.getIdentifier(), account.getType(), account.getIdentity(),
                account.getSource() == null ? null : account.getSource().name(), account.getMetadata(),
                account.getStartDate(), account.getEndDate(), account.getOrderId(), account.getEndOrderId(),
                account.getCreatedAt());
    }

    public AccountTypeDto toDto(AccountType type) {
        return new AccountTypeDto(type.name(), type.kind().name(), type.pattern().pattern(),
                type.metadata().stream()
                        .map(f -> new AccountTypeDto.MetadataFieldDto(f.name(), f.type().name(), f.required()))
                        .toList(),
                type.identityTemplate(), type.identifierTemplate());
    }

    public ContractDto toDto(Contract contract) {
        return new ContractDto(contract.getId(), contract.getEmployeeId(), contract.getType(), contract.getStartDate(),
                contract.getEndDate(), contract.getMetadata(), contract.getCreatedAt());
    }

    public ContractTypeDto toDto(ContractType type) {
        return new ContractTypeDto(type.name(), type.metadata().stream()
                .map(f -> new AccountTypeDto.MetadataFieldDto(f.name(), f.type().name(), f.required()))
                .toList());
    }

    public IdentityDto toDto(Identity identity) {
        return new IdentityDto(identity.getEmployeeId(), identity.getFirstName(), identity.getLastName(),
                identity.getEmail(), identity.getCreatedAt());
    }

    public Identity toDomain(IdentityRequest request) {
        Identity identity = new Identity();
        identity.setFirstName(request.firstName());
        identity.setLastName(request.lastName());
        identity.setEmail(request.email());
        return identity;
    }
}
