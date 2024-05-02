package org.sessx.verhttp;


public class NotImplException extends RuntimeException {
    
    public NotImplException() {
        super();
    }

    public NotImplException(String message) {
        super(message);
    }

    public NotImplException(String message, Throwable cause) {
        super(message, cause);
    }

}