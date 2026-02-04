package com.mydomain.ws.mock;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/response")
public class StatusCheckController {

    @GetMapping("/200")
    ResponseEntity<String> statusCheck200() {
        return ResponseEntity.ok().body("200");
    }

    @GetMapping("/500")
    ResponseEntity<String> statusCheck500() {
        return ResponseEntity.status(500).body("500");
    }
}
