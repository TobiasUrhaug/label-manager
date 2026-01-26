package org.omt.labelmanager.catalog.label;

import jakarta.transaction.Transactional;
import org.omt.labelmanager.catalog.domain.shared.Address;
import org.omt.labelmanager.catalog.domain.shared.Person;
import org.omt.labelmanager.catalog.shared.persistence.AddressEmbeddable;
import org.omt.labelmanager.catalog.shared.persistence.PersonEmbeddable;
import org.omt.labelmanager.catalog.domain.label.Label;
import org.omt.labelmanager.catalog.label.persistence.LabelEntity;
import org.omt.labelmanager.catalog.label.persistence.LabelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LabelCRUDHandler {

    private static final Logger log = LoggerFactory.getLogger(LabelCRUDHandler.class);

    private final LabelRepository repository;

    public LabelCRUDHandler(LabelRepository repository) {
        this.repository = repository;
    }

    public List<Label> getLabelsForUser(Long userId) {
        List<Label> labels = repository.findByUserId(userId).stream()
                .map(Label::fromEntity)
                .toList();
        log.debug("Retrieved {} labels for user {}", labels.size(), userId);
        return labels;
    }

    public void createLabel(
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
        repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting label with id {}", id);
        repository.deleteById(id);
    }

    public Optional<Label> findById(long id) {
        Optional<Label> label = repository.findById(id).map(Label::fromEntity);
        if (label.isEmpty()) {
            log.debug("Label with id {} not found", id);
        }
        return label;
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
