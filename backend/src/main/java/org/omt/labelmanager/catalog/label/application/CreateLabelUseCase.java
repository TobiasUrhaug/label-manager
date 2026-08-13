package org.omt.labelmanager.catalog.label.application;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.omt.labelmanager.catalog.label.api.LabelCreated;
import org.omt.labelmanager.catalog.label.domain.Label;
import org.omt.labelmanager.catalog.label.infrastructure.LabelEntity;
import org.omt.labelmanager.catalog.label.infrastructure.LabelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
class CreateLabelUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateLabelUseCase.class);

    private final LabelRepository repository;
    private final ApplicationEventPublisher events;

    CreateLabelUseCase(LabelRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public Label execute(
            String labelName,
            String email,
            String website,
            Address address,
            Person owner,
            Long userId) {
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

        events.publishEvent(new LabelCreated(entity.getId()));

        return Label.fromEntity(entity);
    }
}
