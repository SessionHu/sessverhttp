package org.sessx.verhttp;

import java.io.IOException;


public class Main {

    public static void main(String[] args) {
        // start
        argsParser(args);
        logger = new Logger();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clear();
        }));
        // run
        try {
            sessver = new Sessver(8848);
            sessver.acceptAll();
        } catch(IOException e) {
            logger.fatal(Logger.xcpt2str(e));
        }
    }

    private static void clear() {
        try {
            sessver.close();
            logger.close();
        } catch(IOException e) {
            logger.warn(Logger.xcpt2str(e));
        }
    }

    public static void printVersionInfo() {
        // TODO
    }

    public static void printHelpInfo() {
        // TODO
    }

    public static void argsParser(String[] args) {
        // TODO
    }
    
    public static Logger logger;
    public static Sessver sessver;

}
