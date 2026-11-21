package com.eventticketing.commerce.api;

import com.eventticketing.commerce.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class CategoryController {
    private final Categories categories;
    public CategoryController(Categories categories){this.categories=categories;}
    public record Create(@NotBlank @Size(max=40)String name){}
    public record Change(boolean active,@NotBlank @Size(max=1000)String reason){}
    @GetMapping("/discover/categories") public List<String> publicList(){return categories.active();}
    @GetMapping("/admin/categories") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> list(@RequestParam(required=false)UUID cursor,@RequestParam(defaultValue="50")int limit){return categories.list(cursor,limit);}
    @PostMapping("/admin/categories") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Create input){return categories.create(input.name(),UUID.fromString(jwt.getSubject()));}
    @PatchMapping("/admin/categories/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> change(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match")String version,@Valid @RequestBody Change input){
        long expected;try{expected=Long.parseLong(version.replace("\"",""));}catch(NumberFormatException ex){throw EventRepository.invalid("Phiên bản danh mục không hợp lệ.");}
        return categories.change(id,expected,input.active(),UUID.fromString(jwt.getSubject()),input.reason());
    }
}
