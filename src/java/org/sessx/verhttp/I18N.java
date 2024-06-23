package org.sessx.verhttp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;


public class I18N {

    private Properties props = new Properties();

    public I18N() {
        // get JVM default locale
        Locale locale = Locale.getDefault();
        // all languages can be used
        String[] langs = {
            "en", locale.getLanguage(), locale.getLanguage() + '_' + locale.getCountry()
        };
        // special language
        if(System.getenv("LANGUAGE") != null &&
           System.getenv("LANGUAGE").contains("zh_XX"))
        {
            langs[2] = "zh_XX";
        }
        // read props of each langs
        for(String l : langs) {
            InputStream in = this.getClass().getResourceAsStream("/assets/lang/" + l + ".properties");
            if(in != null) {
                try(Reader wtr = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    this.props.load(wtr);
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String get(String k, Object... o) {
        return String.format(this.props.getProperty(k, k), o);
    }

}