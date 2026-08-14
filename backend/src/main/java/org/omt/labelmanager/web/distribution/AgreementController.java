package org.omt.labelmanager.web.distribution;

import java.math.BigDecimal;
import org.omt.labelmanager.distribution.agreement.api.AgreementCommandApi;
import org.omt.labelmanager.distribution.agreement.api.AgreementNotFoundException;
import org.omt.labelmanager.distribution.agreement.api.AgreementQueryApi;
import org.omt.labelmanager.distribution.agreement.api.CommissionType;
import org.omt.labelmanager.distribution.agreement.api.DuplicateAgreementException;
import org.omt.labelmanager.web.LabelScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labels/{labelId}/distributors/{distributorId}/agreements")
public class AgreementController {

    private final AgreementCommandApi commandApi;
    private final AgreementQueryApi queryApi;
    private final LabelScope labelScope;

    public AgreementController(
            AgreementCommandApi commandApi, AgreementQueryApi queryApi, LabelScope labelScope) {
        this.commandApi = commandApi;
        this.queryApi = queryApi;
        this.labelScope = labelScope;
    }

    record CreateAgreementRequest(
            Long productionRunId,
            BigDecimal unitPrice,
            CommissionType commissionType,
            BigDecimal commissionValue) {}

    record UpdateAgreementRequest(
            BigDecimal unitPrice, CommissionType commissionType, BigDecimal commissionValue) {}

    @PostMapping
    public ResponseEntity<Void> createAgreement(
            @PathVariable Long labelId,
            @PathVariable Long distributorId,
            @RequestBody CreateAgreementRequest request) {
        labelScope.requireDistributor(labelId, distributorId);
        commandApi.create(
                distributorId,
                request.productionRunId(),
                request.unitPrice(),
                request.commissionType(),
                request.commissionValue());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateAgreement(
            @PathVariable Long labelId,
            @PathVariable Long distributorId,
            @PathVariable Long id,
            @RequestBody UpdateAgreementRequest request) {
        labelScope.requireDistributor(labelId, distributorId);
        var agreement = queryApi.findById(id).orElseThrow(() -> new AgreementNotFoundException(id));
        if (!agreement.distributorId().equals(distributorId)) {
            throw new AgreementNotFoundException(id);
        }
        commandApi.update(
                id, request.unitPrice(), request.commissionType(), request.commissionValue());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgreement(
            @PathVariable Long labelId, @PathVariable Long distributorId, @PathVariable Long id) {
        labelScope.requireDistributor(labelId, distributorId);
        var agreement = queryApi.findById(id).orElseThrow(() -> new AgreementNotFoundException(id));
        if (!agreement.distributorId().equals(distributorId)) {
            throw new AgreementNotFoundException(id);
        }
        commandApi.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DuplicateAgreementException.class)
    public ProblemDetail handleDuplicate(DuplicateAgreementException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(AgreementNotFoundException.class)
    public ProblemDetail handleNotFound(AgreementNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }
}
