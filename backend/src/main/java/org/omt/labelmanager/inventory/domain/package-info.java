/**
 * The stock rule — {@code StockLedger} and the records it draws with. Part of inventory's published
 * surface: sales and returns ask it which pressings a line item comes out of, so §5.2 rule 2 makes
 * it public the same way the api-returned domain records are.
 */
@NamedInterface("api")
package org.omt.labelmanager.inventory.domain;

import org.springframework.modulith.NamedInterface;
