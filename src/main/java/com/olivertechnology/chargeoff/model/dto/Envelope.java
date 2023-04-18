package com.olivertechnology.chargeoff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Envelope {
    private String httpMethod;
    private String customer;
    private String eventType;
    private String requestID;
    private String timestamp;
}
