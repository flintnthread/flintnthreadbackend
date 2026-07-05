package com.ecommerce.adminbackend.dto.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteRequest {
    private String note;
    private String reason;
    private String message;
    private String reply;
    private String status;
    private String transactionRef;
    private String gstStatus;
    private Boolean active;
    private Boolean notifyCustomer;
}
