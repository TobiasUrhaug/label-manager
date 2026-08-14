/**
 * The release aggregate's records. Part of catalog's published surface, not internals: §5.2 rule 2
 * makes the domain records an api returns public, and {@code ReleaseQueryApi} returns {@code
 * Release} while {@code ReleaseCommandApi} accepts {@code TrackInput}. Behaviour still belongs to
 * catalog — callers get the records, not the use cases that build them.
 */
@NamedInterface("api")
package org.omt.labelmanager.catalog.release.domain;

import org.springframework.modulith.NamedInterface;
