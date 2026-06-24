package nure.com.eventflow.shared.exception;

public class AccessDeniedDomainException extends RuntimeException {

    public AccessDeniedDomainException(String message) {
        super(message);
    }
}
