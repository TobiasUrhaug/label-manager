/**
 * The {@code ProductionRun} record. Part of inventory's published surface, not internals: §5.2 rule
 * 2 makes the domain records an api returns public, and {@code ProductionRunQueryApi} returns this
 * one.
 */
@NamedInterface("api")
package org.omt.labelmanager.inventory.productionrun.domain;

import org.springframework.modulith.NamedInterface;
