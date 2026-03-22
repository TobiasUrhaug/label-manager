package org.omt.labelmanager.catalog.label.application;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.AddressEmbeddable;
import org.omt.labelmanager.catalog.infrastructure.persistence.shared.PersonEmbeddable;
import org.omt.labelmanager.catalog.label.infrastructure.LabelEntity;
import org.omt.labelmanager.catalog.label.infrastructure.LabelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class UpdateLabelUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(UpdateLabelUseCase.class);

    private final LabelRepository repository;

    UpdateLabelUseCase(LabelRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(
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
