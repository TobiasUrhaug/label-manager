package org.omt.labelmanager.sales.distributor_return.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;

/**
 * Form backing bean for registering a distributor return.
 */
public class RegisterReturnForm {

    private Long distributorId;
    private LocalDate returnDate;
    private String notes;
    private List<ReturnLineItemForm> lineItems = new ArrayList<>();

    public RegisterReturnForm() {
        // Initialize with one empty line item for convenience
        lineItems.add(new ReturnLineItemForm());
    }

    public List<ReturnLineItemInput> toLineItemInputs() {
        return lineItems.stream()
                .map(ReturnLineItemForm::toInput)
                .toList();
    }

    // Getters and setters

    public Long getDistributorId() {
        return distributorId;
    }

    public void setDistributorId(Long distributorId) {
        this.distributorId = distributorId;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<ReturnLineItemForm> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<ReturnLineItemForm> lineItems) {
        this.lineItems = lineItems;
    }
}
