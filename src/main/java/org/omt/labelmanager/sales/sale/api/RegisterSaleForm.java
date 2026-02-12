package org.omt.labelmanager.sales.sale.api;

import org.omt.labelmanager.distribution.distributor.domain.ChannelType;
import org.omt.labelmanager.sales.sale.domain.SaleLineItemInput;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Form backing bean for registering a sale.
 */
public class RegisterSaleForm {

    private LocalDate saleDate;
    private ChannelType channel;
    private String notes;
    private List<SaleLineItemForm> lineItems = new ArrayList<>();

    public RegisterSaleForm() {
        // Initialize with one empty line item for convenience
        lineItems.add(new SaleLineItemForm());
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

    public ChannelType getChannel() {
        return channel;
    }

    public void setChannel(ChannelType channel) {
        this.channel = channel;
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
