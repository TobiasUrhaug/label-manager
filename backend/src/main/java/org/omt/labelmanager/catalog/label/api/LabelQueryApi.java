package org.omt.labelmanager.catalog.label.api;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.catalog.label.domain.Label;

public interface LabelQueryApi {

    Optional<Label> findById(Long id);

    boolean exists(Long id);

    List<Label> getLabelsForUser(Long id);
}
