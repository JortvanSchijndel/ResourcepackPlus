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
 * Extremely lightweight token storage in a tokens.yml-like properties file.
 * We attempt to reduce exposure by:
 *  - Keeping tokens in a separate file (tokens.properties) with restricted FS permissions (POSIX where supported).
 *  - NOT logging tokens.
 * NOTE: On Windows, POSIX permissions are not available; advise server admins to protect filesystem access.
 */
public class TokenStore {

    private final File file;
    private final Properties props;

    public TokenStore(File pluginFolder) {
        this.file = new File(pluginFolder, "tokens.properties");
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
            Bukkit.getLogger().warning("[RPP] Failed to initialize token store: " + e.getMessage());
        }
    }

    public boolean hasGithubToken() {
        return getGithubToken() != null && !getGithubToken().isBlank();
    }
    public boolean hasDropboxCredentials() {
        return getDropboxAppKey() != null && !getDropboxAppKey().isBlank()
                && getDropboxAppSecret() != null && !getDropboxAppSecret().isBlank()
                && getDropboxRefreshToken() != null && !getDropboxRefreshToken().isBlank();
    }


    public String getGithubToken() {
        return props.getProperty("githubToken");
    }

    public String getDropboxAppKey() {
        return props.getProperty("dropbox.appKey");
    }

    public String getDropboxAppSecret() {
        return props.getProperty("dropbox.appSecret");
    }

    public String getDropboxRefreshToken() {
        return props.getProperty("dropbox.refreshToken");
    }

    public void setGithubToken(String token) {
        props.setProperty("githubToken", token);
        save();
    }
    public void setDropboxCredentials(String appKey, String appSecret, String refreshToken) {
        props.setProperty("dropbox.appKey", appKey);
        props.setProperty("dropbox.appSecret", appSecret);
        props.setProperty("dropbox.refreshToken", refreshToken);
        save();
    }

    private synchronized void save() {
        try (FileWriter fw = new FileWriter(file)) {
            props.store(fw, "ResourcepackPlus tokens");
        } catch (Exception e) {
            Bukkit.getLogger().warning("[RPP] Failed to save tokens: " + e.getMessage());
        }
    }
}
