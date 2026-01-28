package org.omt.labelmanager.finance.api.cost;

import java.io.IOException;
import java.util.Set;
import org.omt.labelmanager.finance.application.DocumentUpload;
import org.omt.labelmanager.finance.application.RegisterCostUseCase;
import org.omt.labelmanager.finance.domain.cost.CostOwner;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
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

    private final RegisterCostUseCase registerCostUseCase;

    public CostController(RegisterCostUseCase registerCostUseCase) {
        this.registerCostUseCase = registerCostUseCase;
    }

    @PostMapping("/labels/{labelId}/releases/{releaseId}/costs")
    public String registerCostForRelease(
            @PathVariable Long labelId,
            @PathVariable Long releaseId,
            RegisterCostForm form,
            @RequestParam(value = "document", required = false) MultipartFile document
    ) throws IOException {
        registerCostUseCase.registerCost(
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
        return "redirect:/labels/" + labelId + "/releases/" + releaseId;
    }

    @PostMapping("/labels/{labelId}/costs")
    public String registerCostForLabel(
            @PathVariable Long labelId,
            RegisterCostForm form,
            @RequestParam(value = "document", required = false) MultipartFile document
    ) throws IOException {
        registerCostUseCase.registerCost(
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
        return "redirect:/labels/" + labelId;
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
