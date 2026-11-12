package com.example.lms.controller;

import com.example.lms.dto.CertificateResponse;
import com.example.lms.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {
    private final CertificateService certificateService;

    @GetMapping("/{code}")
    public CertificateResponse get(@PathVariable String code) {
        return certificateService.getByCode(code);
    }

    @Operation(summary = "Скачать текстовый сертификат в UTF-8")
    @ApiResponse(responseCode = "200", description = "Текст сертификата",
            content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(type = "string")))
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("certificate_" + id + ".txt").build().toString())
                .body(certificateService.download(id));
    }
}
