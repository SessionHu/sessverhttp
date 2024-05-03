package org.sessx.verhttp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class Request {

    private HttpConnection conn;
    private Socket         socket;
    private InputStream    in;

    // request line
    private Method method;  // eg. GET
    private URI    uri;     // eg. /hello.txt
    private String version; // eg. 1.1

    public String getRequestLine() {
        return this.method + " " + this.uri + " HTTP/" + this.version;
    }
    
    // header fields
    private String userAgent;      // User-Agent     eg. sessverhttp/0.0.0
    private String host;           // Host           eg. www.example.com
    private String acceptLanguage; // Acept-Language eg. zh, en
    private String referer;        // Referer        eg. http://bilibili.com/
    /* ... */

    public Request(HttpConnection conn) throws IOException {
        this.conn   = conn;
        this.socket = this.conn.getSocket();
        this.in     = this.socket.getInputStream();
        this.requestLineParser();
        this.headerFieldsParser();
    }
    
    private void requestLineParser() throws IOException {
        // read line
        String line; {
            byte[] buffer = new byte[65536];
            Arrays.fill(buffer, (byte)0);
            int c;
            for(int i = 0;
                i < buffer.length - 1 && (c = this.in.read()) != -1;
                i++)
            {
                if(c == '\r') {
                    if((c = this.in.read()) == '\n') {
                        break;
                    } else if(c == -1) {
                        throw new MessageSyntaxException(
                            "EOF after '\r' in request line at index " + i
                        );
                    } else {
                        buffer[i++] = (byte)'\r';
                    }
                }
                buffer[i] = (byte)c;
            }
            line = new String(buffer, StandardCharsets.UTF_8);
        }
        String[] pieces = line.split(" ");
        // method & uri
        if(pieces.length >= 2) { // HTTP/0.9 & HTTP/1.x
            // method
            this.method = Method.find(pieces[0]);
            // uri
            try {
                this.uri = new URI(pieces[1]);
            } catch(java.net.URISyntaxException e) {
                throw new MessageSyntaxException("invalid URI", e);
            }
        } else { // invalid
            throw new MessageSyntaxException("invalid request line: " + line);
        }
        // version
        if(pieces.length >= 3) { // HTTP/1.x , but more friendly
            String vstr = pieces[2].trim();
            if(vstr.startsWith("HTTP/")) { // is HTTP?
                if(vstr.length() == 8) {
                    if(vstr.endsWith("1.1")) {
                        this.version = "1.1";
                    } else if(vstr.endsWith("1.0")) {
                        this.version = "1.0";
                    } else {
                        throw new NotImplException(vstr);
                    }
                } else {
                    throw new NotImplException(vstr);
                }
            } else {
                throw new MessageSyntaxException("unknown protocol " + vstr);
            }
        } else if(pieces.length == 2) { // HTTP/0.9
            if(this.method.equals(Method.GET)) {
                this.version = "0.9";
            } else {
                throw new MessageSyntaxException(
                    "HTTP/0.9 does not support " + this.method
                );
            }
        }
    }

    private void headerFieldsParser() throws IOException {
        // TODO: here is for debug
        String b;
        BufferedReader in = new BufferedReader(new InputStreamReader(this.in));
        while((b = in.readLine()) != null) {
            Main.logger.debug(b);
            if(b.isEmpty()) {
                break;
            }
        }
        in = null;
    }

}
