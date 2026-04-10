package org.omt.labelmanager.finance.cost.api;

import org.omt.labelmanager.finance.cost.domain.CostOwner;
import org.omt.labelmanager.finance.shared.DocumentUpload;
import org.omt.labelmanager.finance.shared.RetrievedDocument;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@RestController
public class CostController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final CostCommandApi costCommandFacade;

    public CostController(CostCommandApi costCommandFacade) {
        this.costCommandFacade = costCommandFacade;
    }

    @PostMapping("/api/labels/{labelId}/releases/{releaseId}/costs")
    public ResponseEntity<Void> registerCostForRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            RegisterCostForm form,
            @RequestParam(value = "document", required = false) MultipartFile document
    ) throws IOException {
        costCommandFacade.registerCost(
                form.toNetAmount(),
                form.toVatAmount(),
                form.toGrossAmount(),
                form.getCostType(),
                form.getIncurredOn(),
                form.getDescription(),
                CostOwner.release(releaseId),
                form.getDocumentReference(),
                toDocumentUpload(document)
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/api/labels/{labelId}/costs")
    public ResponseEntity<Void> registerCostForLabel(
            @PathVariable Long labelId,
            RegisterCostForm form,
            @RequestParam(value = "document", required = false) MultipartFile document
    ) throws IOException {
        costCommandFacade.registerCost(
                form.toNetAmount(),
                form.toVatAmount(),
                form.toGrossAmount(),
                form.getCostType(),
                form.getIncurredOn(),
                form.getDescription(),
                CostOwner.label(labelId),
                form.getDocumentReference(),
                toDocumentUpload(document)
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/costs/{costId}/document")
    public ResponseEntity<InputStreamResource> getDocument(
            @PathVariable Long costId,
            @RequestParam(defaultValue = "view") String action
    ) {
        RetrievedDocument document = costCommandFacade.retrieveDocument(costId)
                .orElseThrow(() -> new DocumentNotFoundException(costId));

        String disposition = "download".equals(action)
                ? "attachment; filename=\"" + document.filename() + "\""
                : "inline; filename=\"" + document.filename() + "\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(document.contentType()))
                .contentLength(document.contentLength())
                .body(new InputStreamResource(document.content()));
    }

    @DeleteMapping("/api/labels/{labelId}/releases/{releaseId}/costs/{costId}")
    public ResponseEntity<Void> deleteCostForRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @PathVariable Long costId
    ) {
        costCommandFacade.deleteCost(costId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/labels/{labelId}/costs/{costId}")
    public ResponseEntity<Void> deleteCostForLabel(
            @PathVariable Long labelId,
            @PathVariable Long costId
    ) {
        costCommandFacade.deleteCost(costId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/labels/{labelId}/releases/{releaseId}/costs/{costId}")
    public ResponseEntity<Void> updateCostForRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            @PathVariable Long costId,
            RegisterCostForm form,
            @RequestParam(value = "document", required = false) MultipartFile document
    ) throws IOException {
        costCommandFacade.updateCost(
                costId,
                form.toNetAmount(),
                form.toVatAmount(),
                form.toGrossAmount(),
                form.getCostType(),
                form.getIncurredOn(),
                form.getDescription(),
                form.getDocumentReference(),
                toDocumentUpload(document)
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/labels/{labelId}/costs/{costId}")
    public ResponseEntity<Void> updateCostForLabel(
            @PathVariable Long labelId,
            @PathVariable Long costId,
            RegisterCostForm form,
            @RequestParam(value = "document", required = false) MultipartFile document
    ) throws IOException {
        costCommandFacade.updateCost(
                costId,
                form.toNetAmount(),
                form.toVatAmount(),
                form.toGrossAmount(),
                form.getCostType(),
                form.getIncurredOn(),
                form.getDescription(),
                form.getDocumentReference(),
                toDocumentUpload(document)
        );
        return ResponseEntity.noContent().build();
    }

    private DocumentUpload toDocumentUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidDocumentTypeException(contentType);
        }

        return new DocumentUpload(
                file.getOriginalFilename(),
                contentType,
                file.getInputStream()
        );
    }
}
