package org.omt.labelmanager.sales.distributorreturn.domain;

import org.omt.labelmanager.shared.Format;

/** Value object carrying the data for a single return line item submitted by the user. */
public record ReturnLineItemInput(Long releaseId, Format format, int quantity) {}
