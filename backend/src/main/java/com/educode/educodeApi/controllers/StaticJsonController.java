package com.educode.educodeApi.controllers;

import com.educode.educodeApi.interfaces.StaticJsonProvider;
import com.educode.educodeApi.services.StaticJsonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/static")
public class StaticJsonController {

    private final StaticJsonService service;
    private static final Logger log = LoggerFactory.getLogger(StaticJsonController.class);

    public StaticJsonController(StaticJsonService service) {
        this.service = service;
    }

    @GetMapping("/{name:.+\\.json}")
    public ResponseEntity<String> get(
            @PathVariable String name,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) throws IOException {
        log.trace("If-None-Match header: {}", ifNoneMatch);

        StaticJsonProvider.CachedJson cached;
        try {
            cached = service.load(name);
        } catch (IOException e) {
            log.debug("Failed to load JSON file '{}'", name, e);
            return ResponseEntity.notFound().build();
        }

        if (ifNoneMatch != null && ifNoneMatch.equals(cached.getEtag())) {
            log.trace("ETag matched for '{}'. Returning 304", name);
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(ifNoneMatch).build();
        }

        log.trace("ETag mismatch or absent for '{}'. Returning 200", name);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .eTag(cached.getEtag())
                .body(cached.getJson());
    }
}


