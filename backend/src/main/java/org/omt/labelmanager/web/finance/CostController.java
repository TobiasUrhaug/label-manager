package org.omt.labelmanager.web.finance;

import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.omt.labelmanager.catalog.release.api.ReleaseQueryApi;
import org.omt.labelmanager.finance.cost.api.CostCommandApi;
import org.omt.labelmanager.finance.cost.api.CostQueryApi;
import org.omt.labelmanager.finance.cost.api.DocumentNotFoundException;
import org.omt.labelmanager.finance.cost.api.InvalidDocumentTypeException;
import org.omt.labelmanager.finance.cost.domain.Cost;
import org.omt.labelmanager.finance.cost.domain.CostOwner;
import org.omt.labelmanager.finance.shared.DocumentUpload;
import org.omt.labelmanager.finance.shared.RetrievedDocument;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * One collection for a label's costs.
 *
 * <p>Costs are owned by either the label or one of its releases. That used to be two parallel sets
 * of three mappings under different paths; it is now the optional {@code releaseId} form field —
 * absent means the label itself owns the cost.
 */
@RestController
@RequestMapping("/api/labels/{labelId}/costs")
public class CostController {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "application/pdf",
                    "image/png",
                    "image/jpeg",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final CostCommandApi costCommandApi;
    private final CostQueryApi costQueryApi;
    private final ReleaseQueryApi releaseQueryApi;

    public CostController(
            CostCommandApi costCommandApi,
            CostQueryApi costQueryApi,
            ReleaseQueryApi releaseQueryApi) {
        this.costCommandApi = costCommandApi;
        this.costQueryApi = costQueryApi;
        this.releaseQueryApi = releaseQueryApi;
    }

    private void requireRelease(Long labelId, Long releaseId) {
        if (!releaseQueryApi.belongsToLabel(releaseId, labelId)) {
            throw new EntityNotFoundException(
                    "Release " + releaseId + " does not belong to label " + labelId);
        }
    }

    /**
     * The label's costs, or one release's when {@code releaseId} is given.
     *
     * <p>Replaces the {@code costs} field of the release detail response. There was no way to read
     * costs over HTTP at all before this — they were only ever bundled into that page model.
     */
    @GetMapping
    public List<Cost> costs(
            @PathVariable Long labelId,
            @RequestParam(value = "releaseId", required = false) Long releaseId) {
        if (releaseId == null) {
            return costQueryApi.getCostsForLabel(labelId);
        }
        requireRelease(labelId, releaseId);
        return costQueryApi.getCostsForRelease(releaseId);
    }

    @PostMapping
    public ResponseEntity<Void> registerCost(
            @PathVariable Long labelId,
            @RequestParam(value = "releaseId", required = false) Long releaseId,
            RegisterCostForm form,
            @RequestParam(value = "document", required = false) MultipartFile document)
            throws IOException {
        costCommandApi.registerCost(
                form.toNetAmount(),
                form.toVatAmount(),
                form.toGrossAmount(),
                form.getCostType(),
                form.getIncurredOn(),
                form.getDescription(),
                toOwner(labelId, releaseId),
                form.getDocumentReference(),
                toDocumentUpload(document));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{costId}")
    public ResponseEntity<Void> updateCost(
            @PathVariable Long labelId,
            @PathVariable Long costId,
            RegisterCostForm form,
            @RequestParam(value = "document", required = false) MultipartFile document)
            throws IOException {
        requireCostOfLabel(labelId, costId);
        costCommandApi.updateCost(
                costId,
                form.toNetAmount(),
                form.toVatAmount(),
                form.toGrossAmount(),
                form.getCostType(),
                form.getIncurredOn(),
                form.getDescription(),
                form.getDocumentReference(),
                toDocumentUpload(document));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{costId}")
    public ResponseEntity<Void> deleteCost(@PathVariable Long labelId, @PathVariable Long costId) {
        requireCostOfLabel(labelId, costId);
        costCommandApi.deleteCost(costId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{costId}/document")
    public ResponseEntity<InputStreamResource> getDocument(
            @PathVariable Long labelId,
            @PathVariable Long costId,
            @RequestParam(defaultValue = "view") String action) {
        requireCostOfLabel(labelId, costId);

        RetrievedDocument document =
                costCommandApi
                        .retrieveDocument(costId)
                        .orElseThrow(() -> new DocumentNotFoundException(costId));

        String disposition =
                "download".equals(action)
                        ? "attachment; filename=\"" + document.filename() + "\""
                        : "inline; filename=\"" + document.filename() + "\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(document.contentType()))
                .contentLength(document.contentLength())
                .body(new InputStreamResource(document.content()));
    }

    private CostOwner toOwner(Long labelId, Long releaseId) {
        if (releaseId == null) {
            return CostOwner.label(labelId);
        }
        requireRelease(labelId, releaseId);
        return CostOwner.release(releaseId);
    }

    /**
     * Rejects a cost id that is not reachable under this label, so the path segment means
     * something. This is a consistency check between path and resource, not the tenant guard —
     * nothing here establishes that the caller owns the label.
     */
    private void requireCostOfLabel(Long labelId, Long costId) {
        Cost cost =
                costQueryApi
                        .findById(costId)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Cost not found: " + costId));

        boolean reachable =
                switch (cost.owner().type()) {
                    case LABEL -> labelId.equals(cost.owner().id());
                    case RELEASE -> releaseQueryApi.belongsToLabel(cost.owner().id(), labelId);
                    case USER -> false;
                };

        if (!reachable) {
            throw new EntityNotFoundException(
                    "Cost " + costId + " does not belong to label " + labelId);
        }
    }

    private DocumentUpload toDocumentUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidDocumentTypeException(contentType);
        }

        return new DocumentUpload(file.getOriginalFilename(), contentType, file.getInputStream());
    }
}
