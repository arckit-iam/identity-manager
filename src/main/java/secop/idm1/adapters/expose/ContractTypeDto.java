// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.util.List;

public record ContractTypeDto(String name, List<AccountTypeDto.MetadataFieldDto> metadata) {
}