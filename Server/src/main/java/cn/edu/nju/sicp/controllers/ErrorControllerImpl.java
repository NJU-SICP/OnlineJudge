package cn.edu.nju.sicp.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@RestController
public class ErrorControllerImpl implements ErrorController {

    private final Logger logger;

    public ErrorControllerImpl() {
        this.logger = LoggerFactory.getLogger(getClass());
    }

    @RequestMapping("/error")
    public String error(HttpServletRequest request, HttpServletResponse response) {
        HttpStatus status = HttpStatus.resolve(response.getStatus());
        if (status == null) {
            return "Unknown Error";
        } else if (!status.is5xxServerError()) {
            return status.toString();
        } else {
            String id = UUID.randomUUID().toString();
            logger.error(String.format("Error status=%d request=%s", status.value(), id));
            return String.format("%s %s", status, id);
        }
    }

}
