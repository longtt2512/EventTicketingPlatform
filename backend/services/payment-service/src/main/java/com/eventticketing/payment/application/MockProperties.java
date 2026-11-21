package com.eventticketing.payment.application;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("app.mock")
public record MockProperties(boolean enabled,String secret,String merchant){
 public MockProperties {if(enabled&&(secret==null||secret.length()<32||merchant==null||merchant.isBlank()))throw new IllegalArgumentException("Local mock requires a secret of at least 32 characters and a merchant identifier.");}
}
