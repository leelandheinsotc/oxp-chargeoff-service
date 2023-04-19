package com.olivertechnology.chargeoff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseBody {
    private int statusCode;
    private String status;
    private MessageBody body;

}
