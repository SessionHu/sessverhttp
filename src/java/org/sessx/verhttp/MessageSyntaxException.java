package org.sessx.verhttp;


public class MessageSyntaxException extends RuntimeException {
    
    public MessageSyntaxException() {
        super();
    }

    public MessageSyntaxException(String message) {
        super(message);
    }

    public MessageSyntaxException(String message, Throwable cause) {
        super(message, cause);
    }

}
