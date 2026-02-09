package org.omt.labelmanager.catalog.label.api;

import org.omt.labelmanager.catalog.label.Label;

import java.util.List;
import java.util.Optional;

public interface LabelQueryFacade {

    Optional<Label> findById(Long id);

    boolean exists(Long id);

    List<Label> getLabelsForUser(Long id);
}
