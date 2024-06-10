package org.sessx.verhttp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ResponseHelper {

    public static final String MIME_OCTET = "application/octet-stream";

    private HttpConnection conn;
    private Request  request;
    private Response response;

    private URI uri;

    public ResponseHelper(HttpConnection conn) {
        this.conn    = conn;
        this.request = this.conn.getRequest();
        this.uri     = this.request.getAbsURI();
    }

    public static final String WWWROOT_PATH = Main.SVR_ROOT + "/wwwroot";
    static {
        File f = new File(WWWROOT_PATH);
        if(!f.exists()) f.mkdirs();
    }

    public Response send() throws IOException {
        // find file
        File file = null; {
            String path = this.uri.getPath();
            if(path != null && !(path = path.trim()).isEmpty()) {
                if(path.endsWith("/")) { // need index file
                    // try to find index.* file
                    File[] ls = new File(WWWROOT_PATH + path)
                                    .listFiles();
                    for(File f : ls) {
                        if(f.getName().startsWith("index.")) {
                            file = f;
                            break;
                        }
                    }
                    if(file == null) file = new File(path);
                } else { // normal
                    file = new File(WWWROOT_PATH + path);
                }
            }
        }
        // read file
        try(InputStream in = new FileInputStream(file)) {
            this.response = new Response(
                    this.conn, this.request.getHttpVersion(), 200);
            this.response.setHeaderField(
                    "Last-Modified", Logger.getRFCDate(file.lastModified()));
            // read
            long length = file.length();
            if(length < 1048576) {
                // less than 1MiB
                byte[] body = new byte[(int)length];
                in.read(body);
                this.setContentType(body, file.getName());
                this.response.sendBody(body);
            } else if(length < Integer.MAX_VALUE) {
                // more than 1MiB but less than 2^31-1
                byte[] body = new byte[(int)length];
                in.read(body);
                this.setContentType(body, file.getName());
                this.response.sendGzipBody(body);
            } else {
                // chunked
                this.response.setGzip(true);
                int bufferSize = 0;
                byte[] buffer = new byte[65536];
                while((bufferSize = in.read(buffer,0,1024)) != -1) {
                    byte[] chunk = new byte[bufferSize];
                    for(int i = 0; i < bufferSize; i++) {
                        chunk[i] = buffer[i];
                        if(i == 0) this.setContentType(chunk, file.getName());
                    }
                    this.response.sendChunkedBody(chunk);
                }
            } 
        } catch(java.io.FileNotFoundException e) {
            Main.logger.warn(Logger.xcpt2str(e));
            this.response = new Response(
                    conn, this.request.getHttpVersion(), 404);
            byte[] body = Response.simpleStackTraceBody(
                    "404 " + Response.HTTP_CODE_AND_REASON.get(404), e);
            this.response.setHeaderField(
                    "Content-Type", "text/plain; charset=utf-8");
            this.response.sendBody(body);
        } catch(IOException e) {
            Main.logger.warn(Logger.xcpt2str(e));
            throw e;
        }
        return this.response;
    }

    private void setContentType(byte[] magics, String fname) {
        // pre-process
        if(magics.length > 32) {
            byte[] m = new byte[32];
            for(int i = 0; i < 32; i++) {
                m[i] = magics[i];
            }
            magics = m;
        }
        // suffix
        this.response.setHeaderField(
                "Content-Type",
                SUFFIX_MIME.get(fname.substring(
                        fname.indexOf(".") + 1, fname.length() - 1))
        );
        // magics
        for(Map.Entry<byte[], String> e : MAGIC_MIME.entrySet()) {
            byte[] k = e.getKey();
            if(magics.length < k.length) continue;
            for(int i = 0; i < k.length; i++) {
                if(k[i] != magics[i])
                    continue;
                if(i == k.length - 1)
                    this.response.setHeaderField("Content-Type", e.getValue());
            }
        }
    }

    public static final Map<String, String> SUFFIX_MIME;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("css", "text/css");
        map.put("html", "text/html");
        map.put("htm", "text/html");
        map.put("js", "text/javascript");
        SUFFIX_MIME = Collections.unmodifiableMap(map);
    }

    public static final Map<byte[], String> MAGIC_MIME;
    static {
        Map<byte[], String> map = new HashMap<>();
        map.put(
            "<!DOCTYPE html>".getBytes(StandardCharsets.UTF_8), "text/html");
        MAGIC_MIME = Collections.unmodifiableMap(map);
    }

}
