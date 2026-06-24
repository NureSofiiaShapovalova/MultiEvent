package nure.com.eventflow.shared.web;

import nure.com.eventflow.shared.exception.AccessDeniedDomainException;
import nure.com.eventflow.shared.exception.BusinessRuleViolationException;
import nure.com.eventflow.shared.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String handleNotFound(NotFoundException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedDomainException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String handleAccessDenied(AccessDeniedDomainException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/403";
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    String handleBusinessRuleViolation(BusinessRuleViolationException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/422";
    }
}
