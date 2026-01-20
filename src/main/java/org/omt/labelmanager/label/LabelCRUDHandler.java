package org.omt.labelmanager.label;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.common.Address;
import org.omt.labelmanager.common.persistence.AddressEmbeddable;
import org.omt.labelmanager.label.persistence.LabelEntity;
import org.omt.labelmanager.label.persistence.LabelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LabelCRUDHandler {

    private static final Logger log = LoggerFactory.getLogger(LabelCRUDHandler.class);

    private final LabelRepository repository;

    public LabelCRUDHandler(LabelRepository repository) {
        this.repository = repository;
    }

    public List<Label> getAllLabels() {
        List<Label> labels = repository.findAll().stream().map(Label::fromEntity).toList();
        log.debug("Retrieved {} labels", labels.size());
        return labels;
    }

    public void createLabel(String labelName, String email, String website, Address address) {
        log.info("Creating label '{}'", labelName);
        var entity = new LabelEntity(labelName, email, website);
        if (address != null) {
            entity.setAddress(AddressEmbeddable.fromAddress(address));
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
    public void updateAddress(Long labelId, Address address) {
        log.info("Updating address for label {}", labelId);
        repository.findById(labelId).ifPresent(entity -> {
            entity.setAddress(AddressEmbeddable.fromAddress(address));
            repository.save(entity);
        });
    }
}
