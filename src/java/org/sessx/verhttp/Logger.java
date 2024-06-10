package org.sessx.verhttp;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;



public class Logger implements Closeable {

    private Writer out;

    public Logger(File f) {
        try {
            this.out = new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8
            );
        } catch(IOException e) {
            this.out = new Writer() {
                public void close() {}
                public void flush() {}
                public void write(char[] cbuf, int off, int length) {}
            };
            this.err(xcpt2str(e));
        }
    }
    
    public Logger() {
        this(checkParentDir(new File(
            Main.SVR_ROOT + "/logs/svr-" +
            LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss")) +
            ".log"
        )));
    }

    public static File checkParentDir(File f) {
        File parentDir = f.getParentFile();
        if(parentDir!=null && !parentDir.exists()) {
            // check again
            checkParentDir(parentDir);
            // mkdir
            parentDir.mkdir();
            // hide directory when using windows
            if(parentDir.getName().startsWith(".") && 
               System.getProperty("os.name").toLowerCase().contains("windows"))
            {
                // get path
                String prtpath;
                try {
                    prtpath = parentDir.getCanonicalPath();
                } catch(IOException e) {
                    prtpath = parentDir.getAbsolutePath();
                }
                // run
                String[] cmd = {"attrib", "+H", prtpath};
                try {
                    Runtime.getRuntime().exec(cmd);
                } catch(IOException e) {
                    // do nothing...
                }
            }
        }
        return f;
    }

    private void writeln(String oneline) {
        synchronized(this.out) {
            try {
                this.out.write(oneline.replace("\r","").replace("\n",""));
                this.out.write('\n');
            } catch(IOException e) {
                // do nothing...
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.out.close();
    }
    
    public static String xcpt2str(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString().replace("\t", "    ");
    }
    
    public static String[] LEVELS = {"DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
    
    private String[] format(int level, String logs) {
        // time
        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        // get StackTrace
        StackTraceElement st = Thread.currentThread().getStackTrace()[4];
        // get class name
        String className; {
            String fcn = st.getClassName();
            className = fcn.substring(fcn.lastIndexOf('.') + 1);
        }
        // lines
        String[] lines = logs.replace("\r\n", "\n")
                             .replace("\r", "\n")
                             .replace("\t", "    ")
                             .split("\n");
        // format
        for(int i = 0; i < lines.length; i++) {
            lines[i] = String.format(
                    "[%s %s][%s.%s] %s",
                    time, LEVELS[level], className, st.getMethodName(), lines[i]
            );
        }
        // return
        return lines;
    }
    
    public static String getRFCDate() {
        // FIXME: java.time.temporal.UnsupportedTemporalTypeException
        try {
            return LocalDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch(Exception e) {
            Main.logger.err(xcpt2str(e));
            return null;
        }
    }
    
    public static String getRFCDate(LocalDateTime ldt) {
        return ldt.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
    
    public static String getRFCDate(long millis) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
    
    public static LocalDateTime parseRFCDate(String text) {
        return LocalDateTime.parse(
                text, DateTimeFormatter.RFC_1123_DATE_TIME
        );
    }
    
    private void log(int level, String logs) {
        // is null?
        if(logs == null) {
            logs = "";
        }
        // log level
        if(level < 0 || level >= LEVELS.length) {
            level = 0;
        }
        // format
        String[] lines = this.format(level, logs);
        // print & write
        synchronized(this) {
            for(String line : lines) {
                // print
                if(level < 3) {
                    System.out.println(line);
                } else {
                    System.err.println(line);
                }
                // write
                this.writeln(line);
            }
        }
    }
    
    public void debug(String logs) {
        this.log(0, logs);
    }
    
    public void info(String logs) {
        this.log(1, logs);
    }
    
    public void warn(String logs) {
        this.log(2, logs);
    }
    
    public void err(String logs) {
        this.log(3, logs);
    }
    
    public void fatal(String logs) {
        this.log(4, logs);
    }
    
}
