/**
 * Distribution's published surface for distributors: the command and query APIs, the records they
 * return, and {@code ChannelType}, which {@code sales} reaches through here rather than importing
 * directly (Q4). Everything else in {@code distribution} is internal.
 */
@NamedInterface("api")
package org.omt.labelmanager.distribution.distributor.api;

import org.springframework.modulith.NamedInterface;
