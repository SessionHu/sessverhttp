package org.sessx.verhttp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main {

    public static void main(String[] args) {
        // start
        args = argsParser(args);
        logger = new Logger();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clear();
        }));
        // run
        try {
            for(int i = 0; i < args.length; i++) {
                if(args[i].equals("--port")) {
                    if(sessver == null) {
                        sessver = new Sessver(Integer.parseInt(args[++i]));
                    } else {
                        sessver.listen(Integer.parseInt(args[++i]));
                    }
                }
            }
            if(sessver == null) sessver = new Sessver(8848);
        } catch(IOException e) {
            logger.fatal(Logger.xcpt2str(e));
        }
    }

    private static void clear() {
        try {
            if(sessver != null) sessver.close();
            if(logger  != null) logger.close();
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

    public static String[] argsParser(String[] args) {
        List<String> params = new ArrayList<>();
        Map<Character, String> aliasMap = new HashMap<>();
        aliasMap.put('p', "--port");
        for(int i = 0 ; i < args.length; i++) {
            if(args[i] != null && args[i].length() > 1 &&
               args[i].startsWith("-") && !args[i].startsWith("--"))
            {
                for(int j = 1; j < args[i].length(); j++) {
                    params.add(aliasMap.getOrDefault(
                        args[i].charAt(j), "--" + args[i].charAt(j)
                    ));
                }
            } else {
                params.add(args[i]);
            }
        }
        return args = params.toArray(new String[0]);
    }
    
    public static Logger logger;
    public static Sessver sessver;

    public static final String SVR_ROOT = System.getProperty("user.home") +
                                            "/.sessx/verhttp";

}
