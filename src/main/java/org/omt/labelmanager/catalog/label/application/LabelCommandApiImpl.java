package org.omt.labelmanager.catalog.label.application;

import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.catalog.label.api.LabelCommandApi;
import org.springframework.stereotype.Service;

@Service
class LabelCommandApiImpl implements LabelCommandApi {

    private final CreateLabelUseCase createLabel;
    private final UpdateLabelUseCase updateLabel;
    private final DeleteLabelUseCase deleteLabel;

    LabelCommandApiImpl(
            CreateLabelUseCase createLabel,
            UpdateLabelUseCase updateLabel,
            DeleteLabelUseCase deleteLabel
    ) {
        this.createLabel = createLabel;
        this.updateLabel = updateLabel;
        this.deleteLabel = deleteLabel;
    }

    @Override
    public Label createLabel(
            String labelName,
            String email,
            String website,
            Address address,
            Person owner,
            Long userId
    ) {
        return createLabel.execute(
                labelName, email, website, address, owner, userId
        );
    }

    @Override
    public void delete(Long id) {
        deleteLabel.execute(id);
    }

    @Override
    public void updateLabel(
            Long id,
            String name,
            String email,
            String website,
            Address address,
            Person owner
    ) {
        updateLabel.execute(id, name, email, website, address, owner);
    }
}
