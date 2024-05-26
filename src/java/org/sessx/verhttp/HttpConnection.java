package org.sessx.verhttp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class HttpConnection implements java.io.Closeable {

    private Socket socket;
    public  Socket getSocket() {
        return this.socket;
    }

    public HttpConnection(Socket socket) {
        this.socket = socket;
        Main.logger.info(
            "Accepted connection from " +
            this.socket.getInetAddress().getHostAddress() + ":" +
            this.socket.getPort()
        );
    }

    @Override
    public void close() throws IOException {
        if(this.socket.isClosed()) {
            return;
        }
        Main.logger.info(
            "Closing connection from " +
            this.socket.getInetAddress().getHostAddress() + ":" +
            this.socket.getPort()
        );
        this.socket.close();
    }
    
    private Request request;
    public  Request getRequest() {
        return this.request;
    }
    
    private Response response;
    public  Response getResponse() {
        return response;
    }

    public void process() throws IOException {
        if(this.socket.isClosed() ||
           this.request  != null  ||
           this.response != null) 
        {
            return;
        }
        while(true) {
            // parse request
            this.request = null;
            this.request = new Request(this);
            // print request (for debug)
            Main.logger.debug(this.request.getRequestLine());
            for(java.util.Map.Entry<String, String> e :
                this.request.getHeaderFieldsCopy().entrySet())
            {
                Main.logger.debug(e.getKey() + ": " + e.getValue());
            }
            Main.logger.debug("URI: " + request.getAbsURI());
            // send response
            this.response = null;
            this.response = new ResponseHelper(this).send();
            // keep-alive
            if((this.request.getHttpVersion().equals("1.1") &&
                "close".equals(this.request.getHeaderField("Connection"))) ||
               (this.request.getHttpVersion().equals("1.0") &&
                !"keep-alive".equals(this.request.getHeaderField("Connection"))) ||
               this.request.getHttpVersion().equals("0.9")) {
                    break;
            }
        }
    }

}
