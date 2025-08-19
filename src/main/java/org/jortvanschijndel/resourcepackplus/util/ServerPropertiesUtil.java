package org.jortvanschijndel.resourcepackplus.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Loads/saves server.properties using UTF-8 to be explicit and avoid platform-default pitfalls.
 */
public final class ServerPropertiesUtil {
    private ServerPropertiesUtil() {}

    public static Properties load(File serverProperties) throws IOException {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(serverProperties), StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    public static void save(File serverProperties, Properties p, Charset charset) throws IOException {
        // java.util.Properties#store uses ISO-8859-1 by default; we want UTF-8, so write manually.
        try (Writer w = new OutputStreamWriter(new FileOutputStream(serverProperties), charset)) {
            p.store(w, "Updated by ResourcepackPlus");
        }
    }
}
