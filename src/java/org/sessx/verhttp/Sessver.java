package org.sessx.verhttp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;


public class Sessver implements java.io.Closeable {

    private List<ServerSocket> ssockets = new Vector<>();

    public Sessver(int port) throws IOException {
        this.listen(port);
    }

    public void listen(int port) throws IOException {
        ServerSocket ssocket = new ServerSocket(port);
        this.ssockets.add(ssocket);
        Main.logger.info(
            "Sessver is running at port " + ssocket.getLocalPort());
        this.run(ssocket);
    }

    @Override
    public void close() throws IOException {
        Main.logger.info(this.toString()+" is closing");
        for(HttpConnection conn : this.conns) conn.close();
        for(ServerSocket ssocket : this.ssockets) ssocket.close();
    }

    private void run(ServerSocket ssocket) {
        new Thread(() -> {
            while(!ssocket.isClosed()) {
                int counts = 0;
                try {
                    Socket socket = ssocket.accept();
                    new Thread(() -> {
                        try {
                            this.accept(socket);
                        } catch(Throwable e) {
                            Main.logger.err(Logger.xcpt2str(e));
                        }
                    }).start();
                } catch(Throwable t) {
                    Main.logger.fatal(Logger.xcpt2str(t));
                    if(++counts > 3) break;
                }
            }
        }, "Sessver-" + ssocket.getLocalPort()).start();
    }

    private void accept(Socket socket) throws Throwable {
        HttpConnection httpconn = new HttpConnection(socket);
        this.conns.add(httpconn);
        try {
            httpconn.process();
        } catch(Throwable e) {
            try {
                Throwable c = e.getCause();
                boolean expectException = false;
                if(c != null) {
                    boolean emptyReq  =
                        c instanceof MessageSyntaxException &&
                        c.getMessage().equals("empty request");
                    boolean connReset =
                        c instanceof java.net.SocketException &&
                        c.getMessage().equals("Connection reset");
                    expectException = emptyReq || connReset;
                }
                if(!expectException) {
                    Main.logger.err(Logger.xcpt2str(e));
                    new Response(httpconn, e);
                }
            } catch(IOException ioe) {
                Main.logger.err(Logger.xcpt2str(ioe));
            }
        }
        this.conns.remove(httpconn);
        httpconn.close();
    }

    private List<HttpConnection> conns = new Vector<>();

}
