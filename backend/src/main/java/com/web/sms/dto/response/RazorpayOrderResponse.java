package com.web.sms.dto.response;

public class RazorpayOrderResponse {
    private String keyId;
    private String orderId;
    private Long amount;
    private Long amountPaise;
    private String currency;
    private String name;
    private String description;
    private String prefillName;
    private String prefillEmail;
    private String prefillContact;

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPrefillName() { return prefillName; }
    public void setPrefillName(String prefillName) { this.prefillName = prefillName; }
    public String getPrefillEmail() { return prefillEmail; }
    public void setPrefillEmail(String prefillEmail) { this.prefillEmail = prefillEmail; }
    public String getPrefillContact() { return prefillContact; }
    public void setPrefillContact(String prefillContact) { this.prefillContact = prefillContact; }
}
