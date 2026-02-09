package org.omt.labelmanager.catalog.label;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.omt.labelmanager.catalog.label.api.LabelCommandFacade;
import org.omt.labelmanager.inventory.application.SalesChannelCRUDHandler;
import org.omt.labelmanager.inventory.domain.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LabelCommandHandler implements LabelCommandFacade {

    private static final Logger log = LoggerFactory.getLogger(LabelCommandHandler.class);

    private final LabelRepository repository;
    private final SalesChannelCRUDHandler salesChannelCRUDHandler;

    public LabelCommandHandler(
            LabelRepository repository,
            SalesChannelCRUDHandler salesChannelCRUDHandler
    ) {
        this.repository = repository;
        this.salesChannelCRUDHandler = salesChannelCRUDHandler;
    }

    @Transactional
    public Label createLabel(
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

        salesChannelCRUDHandler.create(entity.getId(), "Direct Sales", ChannelType.DIRECT);

        return Label.fromEntity(entity);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting label with id {}", id);
        repository.deleteById(id);
    }

    @Transactional
    public void updateLabel(
            Long id,
            String name,
            String email,
            String website,
            Address address,
            Person owner
    ) {
        log.info("Updating label {}", id);
        repository.findById(id).ifPresent(entity -> {
            entity.setName(name);
            entity.setEmail(email);
            entity.setWebsite(website);
            if (address != null) {
                entity.setAddress(AddressEmbeddable.fromAddress(address));
            }
            if (owner != null) {
                entity.setOwner(PersonEmbeddable.fromPerson(owner));
            }
            repository.save(entity);
        });
    }
}
