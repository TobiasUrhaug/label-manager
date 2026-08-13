package org.omt.labelmanager.catalog.label.api;

/**
 * Published when a label has been created. Catalog states the fact and does not care who reacts;
 * subscribers decide for themselves what a new label means for them.
 *
 * @param labelId the id of the newly created label
 */
public record LabelCreated(Long labelId) {}
