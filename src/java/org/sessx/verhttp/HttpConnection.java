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

    private InputStream in;
    private OutputStream out;

    public HttpConnection(Socket socket) throws IOException {
        this.socket = socket;
        Main.logger.info(
            "Accepted connection from " +
            this.socket.getInetAddress().getHostAddress() + ":" +
            this.socket.getPort()
        );
        this.in = this.socket.getInputStream();
        this.out = this.socket.getOutputStream();
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

    public void process() throws IOException {
        String b;
        // read
        BufferedReader in = new BufferedReader(new InputStreamReader(this.in));
        while((b = in.readLine()) != null) {
            Main.logger.debug(b);
            if(b.isEmpty()) {
                break;
            }
        }
        in = null;
        // write
        StringBuilder sb = new StringBuilder("501 Not Implemented\n");
        sb.append('\n');
        sb.append(Logger.xcpt2str(new NotImplException()));
        sb.append('\n');
        OutputStreamWriter out = new OutputStreamWriter(this.out);
        out.write("HTTP/1.1 501 Not Implemented\r\n");
        out.write("Server: Sessver\r\n");
        out.write("Content-Type: text/plain\r\n");
        out.write("Content-Length: "+sb.length()+"\r\n");
        out.write("\r\n");
        out.write(sb.toString());
        out.flush();
        out = null;
    }

}