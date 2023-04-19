package com.olivertechnology.chargeoff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargeOffResponse {
    private Envelope envelope;
    private ResponseBody requestBody;
}
