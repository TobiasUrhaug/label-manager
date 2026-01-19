package org.omt.labelmanager;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public String handleNotFound(
            ResponseStatusException exception,
            Model model,
            HttpServletResponse response) {
        if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("message", exception.getReason());
            return "error/404";
        }
        throw exception;
    }
}

