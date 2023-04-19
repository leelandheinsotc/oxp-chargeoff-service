package com.olivertechnology.chargeoff.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageBody {
    private String requestID;
    private String eventName;
    private String eventDate;
    private String accountNumber;
    private String matterID;
    private String message;
}
