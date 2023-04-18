package com.olivertechnology.chargeoff;

import com.olivertechnology.chargeoff.model.Chargeoff;
import com.olivertechnology.chargeoff.model.ChargeoffSqs;
import org.springframework.stereotype.Service;

@Service
public class Processor {

  public String buildChargeoffRequest(Chargeoff chargeoff) {

    return Chargeoff.chargeoff(chargeoff.matterUuid, chargeoff.chargeOffDate, chargeoff.chargeOffAmount,
            chargeoff.accountNumber);
  }

  public String buildChargeoffSqsRequest(ChargeoffSqs chargeoffSqs) {

    return ChargeoffSqs.chargeoffSqs(chargeoffSqs.requestID, chargeoffSqs.matterUuid, chargeoffSqs.chargeOffDate,
            chargeoffSqs.chargeOffAmount, chargeoffSqs.accountNumber);
  }
}

