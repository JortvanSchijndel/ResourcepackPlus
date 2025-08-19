package org.jortvanschijndel.resourcepackplus.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Helper for calculating SHA-1 hashes used by server.properties resource-pack-sha1.
 */
public final class HashUtil {
    private HashUtil() {}

    public static String sha1OfFile(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = fis.read(buf)) != -1) {
                digest.update(buf, 0, r);
            }
        }
        byte[] sha1 = digest.digest();
        return toHex(sha1);
    }

    private static String toHex(byte[] arr) {
        StringBuilder sb = new StringBuilder(arr.length * 2);
        for (byte b : arr) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit((b & 0xF), 16));
        }
        return sb.toString();
    }
}
