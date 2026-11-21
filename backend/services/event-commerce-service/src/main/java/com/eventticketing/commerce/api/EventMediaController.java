package com.eventticketing.commerce.api;
import com.eventticketing.commerce.application.EventMediaService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
@RestController @RequestMapping("/api/v1/media")
public class EventMediaController {
 private final EventMediaService media;public EventMediaController(EventMediaService media){this.media=media;}
 @PostMapping("/uploads") @ResponseStatus(HttpStatus.CREATED) public EventMediaService.UploadIntent create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody EventMediaService.UploadInput input){return media.create(EventController.actor(jwt),input);}
 @PostMapping("/uploads/{id}/complete") public Map<String,UUID> complete(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return Map.of("mediaId",media.complete(id,EventController.actor(jwt)));}
 @DeleteMapping("/uploads/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void abandon(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){media.abandon(id,EventController.actor(jwt));}
 @GetMapping("/{id}/download") public ResponseEntity<Map<String,String>> download(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("url",media.download(id,jwt==null?null:EventController.actor(jwt),jwt!=null&&EventController.admin(jwt))));}
}
