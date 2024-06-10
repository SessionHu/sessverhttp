package org.sessx.verhttp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Sessver implements java.io.Closeable {

    private ServerSocket ssocket;

    public Sessver(int port) throws IOException {
        this.ssocket = new ServerSocket(port);
        Main.logger.info("Sessver is running at port "+port);
    }

    @Override
    public void close() throws IOException {
        Main.logger.info(this.toString()+" is closing");
        for(HttpConnection conn : this.conns) {
            conn.close();
        }
        ssocket.close();
    }

    @Override
    public String toString() {
        return "Sessver@" +
            this.ssocket.getInetAddress().getHostAddress() + ":" +
            this.ssocket.getLocalPort();
    }

    public void acceptAll() {
        while(true) {
            Socket socket;
            try {
                socket = this.ssocket.accept();
            } catch(IOException e) {
                Main.logger.err(Logger.xcpt2str(e));
                continue;
            }
            new Thread(() -> {
                try {
                    HttpConnection httpconn = new HttpConnection(socket);
                    this.conns.add(httpconn);
                    try {
                        httpconn.process();
                    } catch(Throwable e) {
                        try {
                            Main.logger.err(Logger.xcpt2str(e));
                            new Response(httpconn, e);
                        } catch(IOException ioe) {
                            Main.logger.err(Logger.xcpt2str(ioe));
                        }
                    }
                    this.conns.remove(httpconn);
                    httpconn.close();
                } catch(Throwable t) {
                    Main.logger.err(Logger.xcpt2str(t));
                }
            }).start();
        }
    }

    private List<HttpConnection> conns =
            Collections.synchronizedList(new ArrayList<>());

}
