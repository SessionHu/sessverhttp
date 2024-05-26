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
    
    public static final char[] TCHAR = {
        '!', '#', '$', '%', '&', '\'', '*', '+', '-', '.', '^', '_', '`', '|',
        '~',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    };
    
    public static void checkToken(String token) {
        if(token == null) {
            throw new MessageSyntaxException("invaild token " + token);
        }
        char c;
        boolean v = false;
        for(int i = 0; i < token.length(); i++) {
            c = token.charAt(i);
            for(int j = 0; j < TCHAR.length; j++) {
                if(c == TCHAR[j]) {
                    v = true;
                    break;
                }
            }
            if(!v) {
                throw new MessageSyntaxException("invaild token " + token);
            } else {
                v = false;
            }
        }
    }

}
