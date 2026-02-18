package org.omt.labelmanager.sales.distributor_return.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.omt.labelmanager.sales.distributor_return.domain.ReturnLineItemInput;

/**
 * Form backing bean for editing a distributor return.
 * The distributor is immutable after registration and is not included here.
 */
public class EditReturnForm {

    private LocalDate returnDate;
    private String notes;
    private List<ReturnLineItemForm> lineItems = new ArrayList<>();

    public EditReturnForm() {
    }

    public List<ReturnLineItemInput> toLineItemInputs() {
        return lineItems.stream()
                .map(ReturnLineItemForm::toInput)
                .collect(Collectors.toList());
    }

    // Getters and setters

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
