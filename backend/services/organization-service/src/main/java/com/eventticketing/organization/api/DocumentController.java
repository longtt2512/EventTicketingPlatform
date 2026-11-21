package com.eventticketing.organization.api;
import com.eventticketing.organization.application.VerificationDocumentService;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
@RequestMapping("/api/v1/verification-documents")
public class DocumentController {
    private final VerificationDocumentService documents;
    public DocumentController(VerificationDocumentService documents) {this.documents=documents;}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public Map<String,UUID> upload(@AuthenticationPrincipal Jwt jwt,@RequestParam("file") MultipartFile file) {return Map.of("id",documents.upload(OrganizationController.user(jwt),file));}
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,Authentication a) {
        var document=documents.download(id,OrganizationController.user(jwt),OrganizationController.admin(a));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(document.type())).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options","nosniff").header("Content-Disposition","attachment; filename=verification-document").body(document.bytes());
    }
}
