package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenMRSInventoryDispenseItem {
    @JsonProperty("item")
    private String itemUuid;
    private int quantity;
    private boolean calculatedExpiration;

    public String getItemUuid() {
        return itemUuid;
    }

    public void setItemUuid(String itemUuid) {
        this.itemUuid = itemUuid;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean getCalculatedExpiration() {
        return calculatedExpiration;
    }

    public void setCalculatedExpiration(boolean calculatedExpiration) {
        this.calculatedExpiration = calculatedExpiration;
    }
}
