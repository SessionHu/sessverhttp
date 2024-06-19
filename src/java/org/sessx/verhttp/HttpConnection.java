package org.sessx.verhttp;

import java.io.IOException;
import java.net.Socket;


public class HttpConnection implements java.io.Closeable {

    private Socket socket;
    public  Socket getSocket() {
        return this.socket;
    }

    public HttpConnection(Socket socket) {
        this.socket = socket;
        //Main.logger.debug(
        //    "Accepted connection from " +
        //    this.socket.getInetAddress().getHostAddress() + ":" +
        //    this.socket.getPort()
        //);
    }

    @Override
    public void close() throws IOException {
        if(this.socket.isClosed()) {
            return;
        }
        //Main.logger.debug(
        //    "Closing connection from " +
        //    this.socket.getInetAddress().getHostAddress() + ":" +
        //    this.socket.getPort()
        //);
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
        if(this.request  != null  ||
           this.response != null) 
        {
            return;
        }
        while(true) {
            if(this.socket.isClosed()) break;
            // parse request
            this.request = null;
            this.request = new Request(this);
            // print request (for debug)
            Main.logger.info(this.request.getRequestLine());
            //for(java.util.Map.Entry<String, String> e :
            //    this.request.getHeaderFieldsCopy().entrySet())
            //{
            //    Main.logger.debug(e.getKey() + ": " + e.getValue());
            //}
            //Main.logger.debug("URI: " + request.getAbsURI());
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
