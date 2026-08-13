package org.omt.labelmanager.sales.distributor_return.domain;

import org.omt.labelmanager.shared.Format;

/**
 * A single line item within a distributor return, representing one release format and quantity
 * being returned from the distributor to the warehouse.
 */
public record ReturnLineItem(Long id, Long returnId, Long releaseId, Format format, int quantity) {}
