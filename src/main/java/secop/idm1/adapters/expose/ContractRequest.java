// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.time.LocalDate;
import java.util.Map;

public record ContractRequest(String type, LocalDate startDate, LocalDate endDate, Map<String, Object> metadata) {
}