package org.sessx.verhttp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Socket;


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
        Main.logger.info(
            "Closing connection from " +
            this.socket.getInetAddress().getHostAddress() + ":" +
            this.socket.getPort()
        );
        this.socket.close();
    }

    private Request request;

    public void process() throws IOException {
        this.request = new Request(this);
        // TODO: here is for debug
        // print request
        Main.logger.debug(this.request.getRequestLine());
        // write
        trace("501 Not Implemented", new NotImplException());
    }

    public void trace(String codeAndMsg, Throwable t) throws IOException {
        StringBuilder sb = new StringBuilder(codeAndMsg);
        sb.append("\n\n");
        sb.append(Logger.xcpt2str(t));
        sb.append('\n');
        OutputStreamWriter out = new OutputStreamWriter(
            this.socket.getOutputStream()
        );
        out.write("HTTP/1.1 " + codeAndMsg + "\r\n");
        out.write("Server: Sessver\r\n");
        out.write("Content-Type: text/plain\r\n");
        out.write("Content-Length: " + sb.length() + "\r\n");
        out.write("\r\n");
        out.write(sb.toString());
        out.flush();
        out.close();
    }

}
