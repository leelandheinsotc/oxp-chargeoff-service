package com.olivertechnology.chargeoff.controller;

import com.olivertechnology.chargeoff.model.Chargeoff;
import com.olivertechnology.chargeoff.model.ChargeoffSqs;
import com.olivertechnology.chargeoff.Processor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
public class ChargeoffController {

  @Autowired
  private Processor processor;

  @PostMapping("/chargeoff/chargeoff")
  String doChargeoff(@RequestBody Chargeoff request) {

    String result = null;

    try {
      result = processor.buildChargeoffRequest(request);
    } catch (Exception ex) {
      log.error("chargeoff error: ", ex);
    }
    log.info(result);
    return result;
  }

  @PostMapping("/chargeoff/chargeoffsqs")
  String doChargeoffSqs(@RequestBody ChargeoffSqs request) {

    String result = null;

    try {
      result = processor.buildChargeoffSqsRequest(request);
    } catch (Exception ex) {
      log.error("chargeoffsqs error: ", ex);
    }
    log.info(result);
    return result;
  }
}
