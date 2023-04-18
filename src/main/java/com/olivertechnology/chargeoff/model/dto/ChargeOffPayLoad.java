package com.olivertechnology.chargeoff.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChargeOffPayLoad {
	@NotNull
	@NotBlank
	private String matterID;

	@NotNull
	@NotBlank
	private String chargeOffDate;

	@NotNull
	@NotBlank
	private String chargeOffAmount;
}
