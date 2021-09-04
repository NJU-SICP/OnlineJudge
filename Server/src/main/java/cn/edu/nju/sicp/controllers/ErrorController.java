package cn.edu.nju.sicp.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ErrorController {

    @RestControllerAdvice
    public static class ExceptionHandlerImpl {

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ExceptionResponse> onException(Exception e) {
            HttpStatus status;
            ExceptionResponse response = new ExceptionResponse();
            if (e instanceof ResponseStatusException) {
                ResponseStatusException rse = (ResponseStatusException) e;
                status = rse.getStatus();
                response.setStatus(status.value());
                response.setMessage(rse.getReason());
            } else if (e instanceof AccessDeniedException) {
                status = HttpStatus.FORBIDDEN;
                response.setStatus(status.value());
                response.setMessage(null);
            } else {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                response.setStatus(status.value());
                response.setMessage("uncaught exception");
            }
            return new ResponseEntity<>(response, status);
        }

        public static class ExceptionResponse {

            private int status;
            private String message;

            public int getStatus() {
                return status;
            }

            public void setStatus(int status) {
                this.status = status;
            }

            public String getMessage() {
                return message;
            }

            public void setMessage(String message) {
                this.message = message;
            }

        }

    }

    @RestController
    public static class ErrorControllerImpl implements org.springframework.boot.web.servlet.error.ErrorController {

        public ErrorControllerImpl() {
        }

        @RequestMapping("/error")
        public String error(HttpServletRequest request, HttpServletResponse response) {
            HttpStatus status = HttpStatus.resolve(response.getStatus());
            if (status == null) {
                return "Unknown Error";
            } else if (!status.is5xxServerError()) {
                return String.format("{status=%d,message=\"%s\"}", status.value(), status.getReasonPhrase());
            } else {
                return String.format("{status=%d,message=\"%s\"}", status.value(), status.getReasonPhrase());
            }
        }

    }

}
