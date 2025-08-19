package org.jortvanschijndel.resourcepackplus.storage;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

/**
 * Lightweight storage for resource pack settings (URL + SHA1).
 * Uses properties file (resourcepack.properties) with restricted FS permissions (POSIX where supported).
 * NOTE: On Windows, POSIX permissions are not available; advise server admins to protect filesystem access.
 */
public class PackStore {

    private final File file;
    private final Properties props;

    public PackStore(File pluginFolder) {
        this.file = new File(pluginFolder, "pack.properties");
        this.props = new Properties();
        try {
            if (!pluginFolder.exists()) pluginFolder.mkdirs();
            if (file.exists()) {
                try (FileReader fr = new FileReader(file)) {
                    props.load(fr);
                }
            } else {
                save();
            }
            // Attempt to set restrictive permissions on POSIX filesystems
            try {
                Set<PosixFilePermission> perms = EnumSet.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE
                );
                Files.setPosixFilePermissions(file.toPath(), perms);
            } catch (UnsupportedOperationException ignored) {
                // Not a POSIX FS (likely Windows). We'll continue.
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RPP] Failed to initialize resourcepack store: " + e.getMessage());
        }
    }

    // Getters
    public String getUrl() {
        return props.getProperty("resourcepack.url");
    }

    public String getSha1() {
        return props.getProperty("resourcepack.sha1");
    }

    public boolean hasUrlAndSha1() {
        return getUrl() != null && !getUrl().isBlank()
                && getSha1() != null && !getSha1().isBlank();
    }

    // Setters
    public void setUrl(String url) {
        props.setProperty("resourcepack.url", url);
        save();
    }

    public void setSha1(String sha1) {
        props.setProperty("resourcepack.sha1", sha1);
        save();
    }

    public void setUrlAndSha1(String url, String sha1) {
        props.setProperty("resourcepack.url", url);
        props.setProperty("resourcepack.sha1", sha1);
        save();
    }

    private synchronized void save() {
        try (FileWriter fw = new FileWriter(file)) {
            props.store(fw, "ResourcepackPlus resourcepack settings");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RPP] Failed to save resourcepack settings: " + e.getMessage());
        }
    }
}
