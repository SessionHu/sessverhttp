package org.sessx.verhttp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;


public class Response {

    private HttpConnection conn;
    private Socket         socket;

    private String version;
    private int    code;
    private String reason;

    private OutputStream out;

    public Response(HttpConnection conn, String version, int code)
            throws IOException
    {
        this.conn   = conn;
        this.socket = this.conn.getSocket();
        this.out    = this.socket.getOutputStream();
        this.setHttpVersion(version);
        this.setStatusCode(code);
        this.setBaseHeaderFields();
    }
    
    public Response(HttpConnection conn, Throwable t) throws IOException {
        this.conn    = conn;
        this.socket  = this.conn.getSocket();
        this.out     = this.socket.getOutputStream();
        Request r = this.conn.getRequest();
        this.version = r != null ? r.getHttpVersion() : "1.1";
        if(t instanceof MessageSyntaxException ||
           t instanceof NotImplException)
        {
            try {
                String msg = t.getMessage();
                this.code = Integer.parseInt(msg.substring(0, 2));
                if(this.code > 99 && this.code < 1000) {
                    this.reason = msg.substring(4, msg.length() - 1);
                } else {
                    throw new NumberFormatException(
                        "invalid http status code " + this.code
                    );
                }
            } catch(NumberFormatException | StringIndexOutOfBoundsException |
                    NullPointerException e)
            {
                this.code = t instanceof MessageSyntaxException ? 400 : 501;
            }
        } else {
            this.code = 500;
        }
        this.reason = HTTP_CODE_AND_REASON.get(this.code);
        this.setBaseHeaderFields();
        byte[] body = simpleStackTraceBody(this.code + " " + this.reason, t);
        this.setHeaderField("Content-Type", "text/plain; charset=utf-8");
        this.setHeaderField("Content-Length", String.valueOf(body.length));
        this.sendBody(body);
        this.socket.close();
    }
    
    public String getHttpVersion() {
        return this.version;
    }

    public int getStatusCode() {
        return this.code;
    }

    private String statusLine;

    public String getStatusLine() {
        if(this.statusLine != null) {
            return this.statusLine;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/");
        sb.append(this.version);
        sb.append(' ');
        sb.append(this.code);
        sb.append(' ');
        sb.append(this.reason);
        return this.statusLine = sb.toString();
    }

    // header fields
    private Map<String, String> headerFields;
    private Map<String, String> nameCase;

    public Map<String, String> getHeaderFieldsCopy() {
        Map<String, String> map = new HashMap<>();
        for(Map.Entry<String, String> e : this.headerFields.entrySet()) {
            map.put(
                this.nameCase.getOrDefault(e.getKey(), e.getKey()),
                e.getValue()
            );
        }
        return Collections.unmodifiableMap(map);
    }

    public String setHeaderField(String key, String val) {
        MessageSyntaxException.checkToken(key);
        synchronized(this) {
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
        return this.headerFields.get(key.toLowerCase(Locale.ROOT));
    }

    /**
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">
     *      HTTP response status codes</a>
     */
    public static final Map<Integer, String> HTTP_CODE_AND_REASON;
    static {
        // new
        Map<Integer, String> map = new HashMap<>();
        // 1XX
        map.put(100, "Continue");
        map.put(101, "Switching Protocol");
        map.put(102, "Processing");
        map.put(103, "Early Hints");
        // 2XX
        map.put(200, "OK");
        map.put(201, "Created");
        map.put(202, "Accepted");
        map.put(203, "Non-Authoritative Information");
        map.put(204, "No Content");
        map.put(205, "Reset Content");
        map.put(206, "Partial Content");
        map.put(207, "Multi-Status");
        map.put(208, "Already Reported");
        map.put(226, "IM Used");
        // 3XX
        map.put(300, "Multiple Choices");
        map.put(301, "Moved Permanently");
        map.put(302, "Found");
        map.put(303, "See Other");
        map.put(304, "Not Modified");
        map.put(307, "Temporary Redirect");
        map.put(308, "Permanent Redirect");
        // 4XX
        map.put(400, "Bad Request");
        map.put(401, "Unauthorized");
        map.put(402, "Payment Required");
        map.put(403, "Forbidden");
        map.put(404, "Not Found");
        map.put(405, "Method Not Allowed");
        map.put(406, "Not Acceptable");
        map.put(407, "Proxy Authentication Required");
        map.put(408, "Request Timeout");
        map.put(409, "Conflict");
        map.put(410, "Gone");
        map.put(411, "Length Required");
        map.put(412, "Precondition Failed");
        map.put(413, "Content Too Large");
        map.put(414, "URI Too Long");
        map.put(415, "Unsupported Media Type");
        map.put(416, "Range Not Satisfiable");
        map.put(417, "Expectation Failed");
        map.put(418, "I'm a teapot");
        map.put(421, "Misdirected Request");
        map.put(422, "Unprocessable Entity");
        map.put(423, "Locked");
        map.put(424, "Failed Dependency");
        map.put(425, "Too Early");
        map.put(426, "Upgrade Required");
        map.put(428, "Precondition Required");
        map.put(429, "Too Many Requests");
        map.put(431, "Request Header Fields Too Large");
        map.put(451, "Unavailable For Legal Reasons");
        // 5XX
        map.put(500, "Internal Server Error");
        map.put(501, "Not Implemented");
        map.put(502, "Bad Gateway");
        map.put(503, "Service Unavailable");
        map.put(504, "Gateway Timeout");
        map.put(505, "HTTP Version Not Supported");
        map.put(506, "Variant Also Negotiates");
        map.put(507, "Insufficient Storage");
        map.put(508, "Loop Detected");
        map.put(510, "Not Extended");
        map.put(511, "Network Authentication Required");
        // set
        HTTP_CODE_AND_REASON = Collections.unmodifiableMap(map);;
    }

    private String setHttpVersion(String v) {
        if(!v.equals("1.1") && !v.equals("1.0") && !v.equals("0.9")) {
            throw new NotImplException("HTTP version " + v);
        }
        if(v.charAt(0) != this.conn.getRequest().getHttpVersion().charAt(0)) {
            throw new MessageSyntaxException(
                "HTTP/" + v + " is unacceptable for HTTP/" +
                this.conn.getRequest().getHttpVersion()
            );
        }
        return this.version = v;
    }

    private String setStatusCode(int code) {
        if(code < 100 || code > 999) {
            throw new NumberFormatException("invalid HTTP status code " + code);
        } else {
            return this.reason = HTTP_CODE_AND_REASON.get(this.code = code);
        }
    }

    private void setBaseHeaderFields() {
        this.headerFields = new Hashtable<>();
        this.nameCase     = new Hashtable<>();
        this.setHeaderField("Server", "Sessver");
    }

    private int stage = 0;
    
    private void sendStatusLine() throws IOException {
        if(this.stage < 1) {
            if(!this.version.equals("0.9")) {
                this.out.write(
                    this.getStatusLine().getBytes(StandardCharsets.UTF_8)
                );
                this.out.write(ChunkedOutputStream.CRLF);
            }
            this.stage = 1;
        }
    }

    private void sendHeaderFields() throws IOException {
        if(this.stage < 1) {
            this.sendStatusLine();
        } else if(this.stage > 1 || this.version.equals("0.9")) {
            return;
        }
        this.headerFields = Collections.unmodifiableMap(this.headerFields);
        this.nameCase     = Collections.unmodifiableMap(this.nameCase);
        StringBuilder sb;
        for(Map.Entry<String, String> e : this.headerFields.entrySet()) {
            sb = new StringBuilder();
            sb.append(this.nameCase.getOrDefault(e.getKey(), e.getKey()));
            sb.append(": ");
            sb.append(e.getValue());
            sb.append("\r\n");
            this.out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            this.out.flush();
        }
        this.out.write(ChunkedOutputStream.CRLF);
        this.stage = 2;
        this.out.flush();
    }

    private void checkBodySendable() throws IOException {
        if(this.stage < 2) {
            this.sendHeaderFields();
        } else if(this.stage > 3) {
            throw new MessageSyntaxException("body already sent");
        }
    }

    public void sendBody(byte[] body) throws IOException {
        if("chunked".equals(this.getHeaderField("Transfer-Encoding")))
        {
            this.sendChunkedBody(body);
            this.sendChunkedBody(new byte[0]);
        }
        this.setHeaderField("Transfer-Encoding", null);
        this.setHeaderField("Content-Encoding" , null);
        this.setHeaderField("Content-Length"   , String.valueOf(body.length));
        this.checkHeaderFields();
        this.checkBodySendable();
        this.stage = 3;
        this.out.write(body);
        this.stage = 4;
        this.out.flush();
        this.setNullOutputStream();
    }

    private void checkHeaderFields() {
        String te = this.getHeaderField("Transfer-Encoding");
        String ce = this.getHeaderField("Content-Encoding");
        String cl = this.getHeaderField("Content-Length");
        String ct = this.getHeaderField("Content-Type");
        if(ce != null) {
            ce = this.setHeaderField("Content-Encoding", null);
        }
        if(te != null && cl != null) {
            cl = this.setHeaderField("Content-Length", null);
        }
        if(ct == null) {
            ct = this.setHeaderField("Content-Type", ResponseHelper.MIME_OCTET);
        }
        if(this.version.equals("0.9") && (te != null || ce != null)) {
            throw new MessageSyntaxException(
                "HTTP/0.9 does not support any encodings");
        }
        this.setHeaderField("Date", Logger.getRFCDate());
    }

    public void sendChunkedBody(byte[] chunk) throws IOException {
        if(this.stage < 3) {
            this.setHeaderField("Transfer-Encoding", "chunked");
            this.setHeaderField("Content-Encoding" , null);
            this.setHeaderField("Content-Length"   , null);
            this.checkHeaderFields();
            this.checkBodySendable();
            String[] te = this.getHeaderField("Transfer-Encoding")
                              .split(",\\s*");
            for(int i = te.length - 1; i > -1; i--)
                if(te[i].equals("chunked"))
                    this.out = new ChunkedOutputStream(this.out);
            this.stage = 3;
        }
        this.out.write(chunk);
        this.out.flush();
        if(chunk.length == 0) {
            this.setNullOutputStream();
            this.stage = 4;
        }
    }

    private void setNullOutputStream() {
        this.out = new OutputStream() {
            @Override public void write(int b) {}
            @Override public void write(byte b[], int off, int len) {}
            @Override public void close() {}
        };
    }

    public static byte[] simpleStackTraceBody(String title, Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        sb.append("\n\n");
        sb.append(Logger.xcpt2str(t));
        sb.append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

}
