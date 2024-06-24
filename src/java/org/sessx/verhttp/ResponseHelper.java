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
        File file = new File(WWWROOT_PATH + this.uri.getPath()); {
            if(file.exists()) {
                // try to find index.* file
                File[] ls = file.listFiles();
                if(ls != null) for(File f : ls) {
                    if(f.getName().startsWith("index.")) {
                        file = f;
                        break;
                    }
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
            if(length < 16777216) {
                // less than 16MiB
                byte[] body = new byte[(int)length];
                in.read(body);
                this.setContentType(body, file.getName());
                this.response.sendBody(body);
            } else {
                // chunked
                int bufferSize = 0;
                byte[] buffer = new byte[65536];
                while((bufferSize = in.read(buffer,0,1024)) != -1) {
                    byte[] chunk = new byte[bufferSize];
                    for(int i = 0; i < bufferSize; i++) {
                        chunk[i] = buffer[i];
                        if(this.response.getHeaderField("Content-Type") == null)
                            this.setContentType(chunk, file.getName());
                    }
                    this.response.sendChunkedBody(chunk);
                }
                this.response.sendChunkedBody(new byte[0]);
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
                        fname.lastIndexOf(".") + 1, fname.length()))
        );
        if(this.response.getHeaderField("Content-Type") != null) {
            return;
        }
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
        map.put("css",   "text/css");
        map.put("html",  "text/html");
        map.put("htm",   "text/html");
        map.put("ico",   "image/vnd.microsoft.icon");
        map.put("js",    "text/javascript");
        map.put("json",  "application/json");
        map.put("png",   "image/png");
        map.put("svg",   "image/svg+xml");
        map.put("ttf",   "font/ttf");
        map.put("woff2", "font/woff2");
        SUFFIX_MIME = Collections.unmodifiableMap(map);
    }

    public static final Map<byte[], String> MAGIC_MIME;
    static {
        Map<byte[], String> map = new HashMap<>();
        map.put(
            "<!DOCTYPE html>".getBytes(StandardCharsets.UTF_8), "text/html");
        map.put(
            new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            "image/png");
        map.put(
            new byte[]{0x00, 0x00, 0x01, 0x00}, "image/vnd.microsoft.icon");
        map.put(new byte[]{0x3C, 0x3F, 0x78, 0x6D, 0x6C}, "application/xml");
        MAGIC_MIME = Collections.unmodifiableMap(map);
    }

}
