// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import java.time.LocalDate;

/** Nouvelle validité d'un compte ; une date absente signifie « sans date ». */
public record DatesRequest(LocalDate startDate, LocalDate endDate) {
}
