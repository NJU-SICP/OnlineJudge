package cn.edu.nju.sicp.controllers;

import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/misc")
public class MiscController {

    private final BuildProperties buildProperties;

    public MiscController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping("/time")
    public ResponseEntity<Date> getServerTime() {
        return ResponseEntity.ok(new Date());
    }

    @GetMapping("/version")
    public ResponseEntity<String> getServerVersion() {
        return ResponseEntity.ok(buildProperties.getVersion());
    }

    @GetMapping("/ok-client/version")
    public ResponseEntity<String> getOkClientVersion() {
        return ResponseEntity.ok("NJU-SICP-v1.7.0u2");
    }

}
