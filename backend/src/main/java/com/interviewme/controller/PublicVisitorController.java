package com.interviewme.controller;

import com.interviewme.dto.visitor.VisitorIdentifyRequest;
import com.interviewme.dto.visitor.VisitorIdentifyResponse;
import com.interviewme.service.VisitorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/visitors")
@RequiredArgsConstructor
@Slf4j
public class PublicVisitorController {

    private final VisitorService visitorService;

    @PostMapping("/{slug}/identify")
    @Transactional
    public ResponseEntity<VisitorIdentifyResponse> identify(
            @PathVariable String slug,
            @Valid @RequestBody VisitorIdentifyRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        log.info("Visitor identifying for slug={} name={} company={} ip={}", slug, request.name(), request.company(), ip);
        VisitorIdentifyResponse response = visitorService.identify(slug, request, ip, ua);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
