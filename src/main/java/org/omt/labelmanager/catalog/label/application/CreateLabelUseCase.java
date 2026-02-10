package org.omt.labelmanager.catalog.label.application;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.catalog.label.infrastructure.LabelEntity;
import org.omt.labelmanager.catalog.label.infrastructure.LabelRepository;
import org.omt.labelmanager.inventory.application.SalesChannelCRUDHandler;
import org.omt.labelmanager.inventory.domain.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class CreateLabelUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(CreateLabelUseCase.class);

    private final LabelRepository repository;
    private final SalesChannelCRUDHandler salesChannelCRUDHandler;

    CreateLabelUseCase(
            LabelRepository repository,
            SalesChannelCRUDHandler salesChannelCRUDHandler
    ) {
        this.repository = repository;
        this.salesChannelCRUDHandler = salesChannelCRUDHandler;
    }

    @Transactional
    public Label execute(
            String labelName,
            String email,
            String website,
            Address address,
            Person owner,
            Long userId
    ) {
        log.info("Creating label '{}' for user {}", labelName, userId);
        var entity = new LabelEntity(labelName, email, website);
        entity.setUserId(userId);
        if (address != null) {
            entity.setAddress(AddressEmbeddable.fromAddress(address));
        }
        if (owner != null) {
            entity.setOwner(PersonEmbeddable.fromPerson(owner));
        }
        entity = repository.save(entity);

        salesChannelCRUDHandler.create(
                entity.getId(), "Direct Sales", ChannelType.DIRECT
        );

        return Label.fromEntity(entity);
    }
}
