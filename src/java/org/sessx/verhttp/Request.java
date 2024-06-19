package org.sessx.verhttp;

import java.io.InputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class Request {

    private HttpConnection conn;
    private Socket         socket;
    private int            index = -1;

    // request line
    private Method method;  // eg. GET
    private URI    uri;     // eg. /hello.txt
    private String version; // eg. 1.1
    
    public Method getMethod() {
        return this.method;
    }
    
    public URI getURI() {
        return this.uri;
    }
    
    public String getHttpVersion() {
        return this.version;
    }

    private String reRequestLine;

    public String getRequestLine() {
        if(this.reRequestLine != null) {
            return this.reRequestLine;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.method);
        sb.append(' ');
        sb.append(this.uri);
        if(!this.version.equals("0.9")) {
            sb.append(" HTTP/");
            sb.append(this.version);
        }
        return this.reRequestLine = sb.toString();
    }
    
    private URI absUri;
    public URI getAbsURI() {
        return this.absUri;
    }
    
    // header fields
    private Map<String, String> headerFields;
    private Map<String, String> nameCase;

    public Request(HttpConnection conn) throws IOException {
        this.conn   = conn;
        this.socket = this.conn.getSocket();
        this.ignoreWhitespaces();
        this.requestLineParser();
        this.headerFieldsParser();
        this.absUriParser();
    }
    
    private String readChars(InputStream in, char[] ends, int max)
            throws IOException
    {
        StringBuilder sb = new StringBuilder();
        int c;
        for(int i = 0; i < max; i++) {
            // i tried to support HTTP/0.9, but failed
            //if(in.available() == 0 && !reader.ready()) {
            //    try {
            //        Thread.sleep(5000);
            //    } catch(InterruptedException e) {
            //        Main.logger.warn(Logger.xcpt2str(e));
            //    }
            //    if(in.available() == 0 && !reader.ready()) {
            //        return sb.toString();
            //    }
            //}
            if((c = in.read()) == -1) break;
            this.index++;
            if(c != '\r') {
                sb.append((char)c);
            } else {
                continue;
            }
            for(int j = 0; j < ends.length; j++) {
                if(ends[j] == c) {
                    //Main.logger.debug(sb.toString());
                    return sb.toString();
                }
            }
        }
        if(this.index != -1) {
            throw new IOException(
                "failed to reach end sign before index " + this.index
            );
        } else {
            throw new MessageSyntaxException("empty request");
        }
    }
    
    private static final char SP   = 0x20;
    private static final char HTAB = '\t';
    private static final char VT   = 0x0B;
    private static final char FF   = 0x0C;
    private static final char CR   = '\r';
    private static final char[] ALL_WHITESPACE = {SP, HTAB, VT, FF, CR};
    
    private static final String BAD_REQUEST  = "400 Bad Request";
    private static final String URI_TOO_LONG = "414 URI Too Long";
    private static final String VER_NOT_SPT  = "505 HTTP Version Not Supported";

    private void ignoreWhitespaces() throws IOException {
        InputStream in = this.socket.getInputStream();
        if(in.markSupported()) {
            int c;
            in.mark(2);
            while((c = in.read()) != -1) {
                if(c > 0x20) {
                    in.reset();
                }
                in.mark(2);
            }
        }
    }

    private void requestLineParser() throws IOException {
        InputStream in = this.socket.getInputStream();
        String b;
        // method
        try {
            b = this.readChars(in, ALL_WHITESPACE, 12).trim();
            MessageSyntaxException.checkToken(b);
            this.method = Method.find(b);
        } catch(IOException | MessageSyntaxException e) {
            throw new MessageSyntaxException(BAD_REQUEST, e);
        }
        // uri
        try {
            b = this.readChars(in, ALL_WHITESPACE, 65536);
            this.uri = new URI(b.trim());
        } catch(java.net.URISyntaxException e) {
            throw new MessageSyntaxException(BAD_REQUEST, e);
        } catch(IOException e) {
            throw new MessageSyntaxException(URI_TOO_LONG, e);
        }
        // version
        try {
            if(b.charAt(b.length() - 1) == SP   ||
               b.charAt(b.length() - 1) == HTAB ||
               b.charAt(b.length() - 1) == VT   ||
               b.charAt(b.length() - 1) == FF)
            { // HTTP/1.x
                b = this.readChars(in, new char[]{'\n'}, 12).trim();
                if(b.startsWith("HTTP/")) { // is HTTP?
                    if(b.length() == 8) {
                        if(b.endsWith("1.1")) {
                            this.version = "1.1";
                        } else if(b.endsWith("1.0")) {
                            this.version = "1.0";
                        } else {
                            throw new NotImplException(
                                VER_NOT_SPT,
                                new MessageSyntaxException(b.trim())
                            );
                        }
                    } else {
                        throw new NotImplException(
                            VER_NOT_SPT,
                            new MessageSyntaxException(b.trim())
                        );
                    }
                } else {
                    throw new MessageSyntaxException(
                        BAD_REQUEST,
                        new MessageSyntaxException(
                            "unknown protocol " + b.trim()
                        )
                    );
                }
            } else if((b.charAt(b.length() - 1) != SP   &&
                       b.charAt(b.length() - 1) != HTAB &&
                       b.charAt(b.length() - 1) != VT   &&
                       b.charAt(b.length() - 1) != FF)  ||
                      (b.charAt(b.length() - 1) == '\n'))
            { // HTTP/0.9
                if(this.method.equals(Method.GET)) {
                    this.version = "0.9";
                } else {
                    throw new MessageSyntaxException(
                        BAD_REQUEST,
                        new MessageSyntaxException("HTTP/0.9 only supports GET")
                    );
                }
            }
        } catch(IOException e) {
            throw new MessageSyntaxException(BAD_REQUEST, e);
        }
    }

    private void headerFieldsParser() {
        // init
        this.headerFields = new HashMap<>();
        this.nameCase     = new HashMap<>();
        // read
        try {
            InputStream in = this.socket.getInputStream();
            String        line;
            StringBuilder key, val;
            char          c;
            int           i;
            while(!(line = this.readChars(
                    in, new char[]{'\n'}, 32768)).trim().isEmpty())
            {
                // no obs-fold
                if(line.charAt(0) == SP ||
                   line.charAt(0) == HTAB)
                {
                    throw new MessageSyntaxException(
                       "obsolete line folding is unacceptable according to " +
                       "RFC9112 Section 5.2, but found at the line before " +
                       "index " + (this.index - 1)
                    );
                }
                // variables
                line = line.trim();
                key  = new StringBuilder();
                val  = new StringBuilder();
                // key
                for(i = 0; i < line.length() - 1; i++) {
                    if((c = line.charAt(i)) != ':') {
                        key.append(c);
                    } else {
                        break;
                    }
                }
                MessageSyntaxException.checkToken(key.toString());
                // val
                if(i == line.length() - 1) {
                    throw new MessageSyntaxException(
                        "':' not found at the line before index " +
                        (this.index - 1)
                    );
                }
                for(i++ ; i < line.length(); i++) {
                    val.append(c = line.charAt(i));
                }
                // put
                this.nameCase.put(
                    key.toString().toLowerCase(Locale.ROOT), key.toString()
                );
                this.headerFields.put(
                    key.toString().toLowerCase(Locale.ROOT),
                    val.toString().trim()
                );
            }
        } catch(IOException | MessageSyntaxException e) {
            throw new MessageSyntaxException(BAD_REQUEST, e);
        }
    }
    
    public Map<String, String> getHeaderFieldsCopy() {
        Map<String, String> map = new HashMap<>();
        synchronized(this.headerFields) {
            for(Map.Entry<String, String> e : this.headerFields.entrySet()) {
                map.put(
                    this.nameCase.getOrDefault(e.getKey(), e.getKey()),
                    e.getValue()
                );
            }
        }
        return Collections.unmodifiableMap(map);
    }

    public String setHeaderField(String key, String val) {
        MessageSyntaxException.checkToken(key);
        synchronized(this.headerFields) {
            if(val == null) {
                this.headerFields.remove(key.toLowerCase(Locale.ROOT));
                return null;
            }
            if(!this.nameCase.containsKey(key.toLowerCase(Locale.ROOT))) {
                this.nameCase.put(key.toLowerCase(Locale.ROOT), key);
            }
            return this.headerFields.put(key.toLowerCase(Locale.ROOT), val);
        }
    }
    
    public String getHeaderField(String key) {
        synchronized(this.headerFields) {
            return this.headerFields.get(key.toLowerCase(Locale.ROOT));
        }
    }

    private void absUriParser() {
        if(this.uri.getScheme() != null) {
            this.absUri = uri;
            return;
        }
        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(this.version.equals("0.9") ?
                        "0.0.0.0" : this.getHeaderField("Host"));
            sb.append(this.uri.getPath());
            this.absUri = new URI(sb.toString());
        } catch(java.net.URISyntaxException e) {
            throw new MessageSyntaxException(BAD_REQUEST, e);
        }
    }

}
