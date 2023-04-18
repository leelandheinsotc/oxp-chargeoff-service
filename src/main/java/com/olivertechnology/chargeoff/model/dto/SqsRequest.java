package com.olivertechnology.chargeoff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.Valid;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SqsRequest {
    @NotNull
    @Valid
    private Envelope envelope;

    @NotNull
    @NotBlank
    private RequestBody requestBody;
}
