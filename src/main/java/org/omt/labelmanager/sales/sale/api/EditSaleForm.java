package org.omt.labelmanager.sales.sale.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;

/**
 * Form backing bean for editing an existing sale.
 *
 * <p>Channel and distributor are intentionally absent — they are immutable after
 * registration. If the wrong distributor was used, delete and re-register the sale.</p>
 */
public class EditSaleForm {

    private LocalDate saleDate;
    private String notes;
    private List<SaleLineItemForm> lineItems = new ArrayList<>();

    public EditSaleForm() {
    }

    public List<SaleLineItemInput> toLineItemInputs() {
        return lineItems.stream()
                .map(SaleLineItemForm::toInput)
                .collect(Collectors.toList());
    }

    // Getters and setters

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<SaleLineItemForm> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<SaleLineItemForm> lineItems) {
        this.lineItems = lineItems;
    }
}
