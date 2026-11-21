package com.eventticketing.payment.api;

import com.eventticketing.payment.application.FinancialLedger;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/finance")
public class FinancialLedgerController {
    private final com.eventticketing.payment.application.PayoutAuthorization payouts;private final FinancialLedger ledger;private final String token;
    public FinancialLedgerController(FinancialLedger ledger,com.eventticketing.payment.application.PayoutAuthorization payouts,@Value("${app.identity.service-token}")String token){this.payouts=payouts;this.ledger=ledger;this.token=token;}
    @PostMapping("/payout-authorizations")
    public com.fasterxml.jackson.databind.JsonNode authorize(@jakarta.validation.Valid @RequestBody com.eventticketing.payment.application.PayoutAuthorization.Request input,@RequestHeader(value="X-Service-Token",required=false)String supplied){
        if(supplied==null||!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication is required.");
        return payouts.authorize(input);
    }
    @GetMapping("/events/{event}")
    public Map<String,Object> snapshot(@PathVariable UUID event,@RequestHeader(value="X-Service-Token",required=false)String supplied){
        if(supplied==null||!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication is required.");
        return ledger.snapshot(event);
    }
}
