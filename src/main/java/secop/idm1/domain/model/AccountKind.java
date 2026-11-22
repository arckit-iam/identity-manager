// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.domain.model;

/**
 * Nature d'un compte. Identity-manager ne gère que les comptes {@link #USER} (rattachés à une identité humaine) ;
 * les comptes {@link #NON_USER} (rattachés à un produit) relèvent de product-manager. Les deux services peuvent
 * recevoir la même liste de types : chacun ignore la nature qui n'est pas la sienne.
 */
public enum AccountKind {
    NON_USER,
    USER
}
