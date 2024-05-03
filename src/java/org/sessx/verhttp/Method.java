package org.sessx.verhttp;


public class Method {

    private static final String[] SUPPORT_METHOD_NAMES = {
        "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"
    };

    public static final Method GET     = new Method(SUPPORT_METHOD_NAMES[0]);
    public static final Method POST    = new Method(SUPPORT_METHOD_NAMES[1]);
    public static final Method HEAD    = new Method(SUPPORT_METHOD_NAMES[2]);
    public static final Method OPTIONS = new Method(SUPPORT_METHOD_NAMES[3]);
    public static final Method PUT     = new Method(SUPPORT_METHOD_NAMES[4]);
    public static final Method DELETE  = new Method(SUPPORT_METHOD_NAMES[5]);
    public static final Method TRACE   = new Method(SUPPORT_METHOD_NAMES[6]);

    private Method(String name) {
        this.name = name;
    }

    private String name;

    public static Method find(String name) {
        if(SUPPORT_METHOD_NAMES[0].equals(name)) {
            return GET;
        } else if(SUPPORT_METHOD_NAMES[1].equals(name)) {
            return POST;
        } else if(SUPPORT_METHOD_NAMES[2].equals(name)) {
            return HEAD;
        } else if(SUPPORT_METHOD_NAMES[3].equals(name)) {
            return OPTIONS;
        } else if(SUPPORT_METHOD_NAMES[4].equals(name)) {
            return PUT;
        } else if(SUPPORT_METHOD_NAMES[5].equals(name)) {
            return DELETE;
        } else if(SUPPORT_METHOD_NAMES[6].equals(name)) {
            return TRACE;
        } else {
            throw new MessageSyntaxException("method " + name);
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj instanceof Method) {
            Method m = (Method)obj;
            return m.name == this.name;
        }
        return false;
    }

}
