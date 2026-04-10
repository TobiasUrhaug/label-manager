package org.omt.labelmanager.distribution.agreement.api;

import org.omt.labelmanager.distribution.agreement.CommissionType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/labels/{labelId}/distributors/{distributorId}/agreements")
public class AgreementController {

    private final AgreementCommandApi commandApi;
    private final AgreementQueryApi queryApi;

    public AgreementController(AgreementCommandApi commandApi, AgreementQueryApi queryApi) {
        this.commandApi = commandApi;
        this.queryApi = queryApi;
    }

    record CreateAgreementRequest(
            Long productionRunId,
            BigDecimal unitPrice,
            CommissionType commissionType,
            BigDecimal commissionValue
    ) {}

    record UpdateAgreementRequest(
            BigDecimal unitPrice,
            CommissionType commissionType,
            BigDecimal commissionValue
    ) {}

    @PostMapping
    public ResponseEntity<Void> createAgreement(
            @PathVariable Long distributorId,
            @RequestBody CreateAgreementRequest request
    ) {
        commandApi.create(distributorId, request.productionRunId(),
                request.unitPrice(), request.commissionType(), request.commissionValue());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateAgreement(
            @PathVariable Long distributorId,
            @PathVariable Long id,
            @RequestBody UpdateAgreementRequest request
    ) {
        var agreement = queryApi.findById(id)
                .orElseThrow(() -> new AgreementNotFoundException(id));
        if (!agreement.distributorId().equals(distributorId)) {
            throw new AgreementNotFoundException(id);
        }
        commandApi.update(id, request.unitPrice(), request.commissionType(), request.commissionValue());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgreement(
            @PathVariable Long distributorId,
            @PathVariable Long id
    ) {
        var agreement = queryApi.findById(id)
                .orElseThrow(() -> new AgreementNotFoundException(id));
        if (!agreement.distributorId().equals(distributorId)) {
            throw new AgreementNotFoundException(id);
        }
        commandApi.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler({DuplicateAgreementException.class, IllegalArgumentException.class})
    public ResponseEntity<Void> handleBadRequest() {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(AgreementNotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }
}
