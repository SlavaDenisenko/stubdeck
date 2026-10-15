package com.stubdeck.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<@NonNull Void> handleNoResource(NoResourceFoundException e) {
        log.debug("No resource: {}", e.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(HttpServletRequest req) {
        if (isApiRequest(req)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
        }
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.FORBIDDEN);
        mav.addObject("message", "You don't have permission to perform this action.");
        mav.addObject("path", req.getRequestURI());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public Object handle(HttpServletRequest req, Exception e) {
        log.error("Unhandled error on {}: {}", req.getRequestURI(), e.getMessage(), e);

        HttpStatus status = e instanceof IllegalArgumentException
                ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR;
        if (isApiRequest(req)) {
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }

        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("message", e.getMessage());
        mav.addObject("path", req.getRequestURI());
        return mav;
    }

    private boolean isApiRequest(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String accept = req.getHeader("Accept");
        return uri.startsWith("/api/")
                || (accept != null && accept.contains("application/json") && !accept.contains("text/html"));
    }
}
