package com.work.total_app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BasePathController {

    @GetMapping("/")
    public ResponseEntity<String> blockDefault() {
        return ResponseEntity.status(403).build();
    }
}
