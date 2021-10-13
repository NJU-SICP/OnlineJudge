package cn.edu.nju.sicp.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/misc")
public class MiscController {

    @GetMapping("/time")
    public ResponseEntity<Date> getServerTime() {
        return new ResponseEntity<>(new Date(), HttpStatus.OK);
    }

    @GetMapping("/ok-client/version")
    public ResponseEntity<String> getOkClientVersion() {
        return new ResponseEntity<>("NJU-SICP-v1.5.0", HttpStatus.OK);
    }

}
