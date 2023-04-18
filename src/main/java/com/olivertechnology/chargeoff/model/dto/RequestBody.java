package com.olivertechnology.chargeoff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestBody {
    private String requestID;
    private String accountNumber;
    private String eventName;
    private String eventDate;
    private ChargeOffPayLoad payload;
}
