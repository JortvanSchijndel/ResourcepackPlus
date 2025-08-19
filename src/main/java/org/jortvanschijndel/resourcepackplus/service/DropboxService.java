package org.jortvanschijndel.resourcepackplus.service;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.dropbox.core.v2.sharing.SharedLinkSettings;

import java.io.File;
import java.io.FileInputStream;

public class DropboxService {

    private final DbxClientV2 client;

    public DropboxService(String appKey, String appSecret, String refreshToken, String userAgent) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder(userAgent)
                .withHttpRequestor(StandardHttpRequestor.INSTANCE)
                .build();

        DbxCredential credential = new DbxCredential(
                "", // No initial access token
                -1L,
                refreshToken,
                appKey,
                appSecret
        );

        this.client = new DbxClientV2(config, credential);
    }

    public String getAccountName() throws Exception {
        return client.users().getCurrentAccount().getName().getDisplayName();
    }

    public void uploadFile(File localFile, String dropboxDestPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(localFile)) {
            client.files()
                    .uploadBuilder(dropboxDestPath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(fis);
        }
    }

    public String createOrGetSharedLink(String dropboxPath) throws Exception {
        try {
            SharedLinkMetadata meta = client.sharing()
                    .createSharedLinkWithSettings(dropboxPath, SharedLinkSettings.newBuilder().build());
            return meta.getUrl();
        } catch (com.dropbox.core.v2.sharing.CreateSharedLinkWithSettingsErrorException e) {
            var links = client.sharing().listSharedLinksBuilder()
                    .withPath(dropboxPath)
                    .withDirectOnly(true)
                    .start()
                    .getLinks();
            if (!links.isEmpty()) {
                return links.get(0).getUrl();
            }
            throw e;
        }
    }

    public void clearFolder(String folderPath) throws Exception {
        if (!folderPath.startsWith("/")) {
            folderPath = "/" + folderPath;
        }

        try {
            ListFolderResult result = client.files().listFolder(folderPath);
            while (true) {
                for (Metadata md : result.getEntries()) {
                    try {
                        client.files().deleteV2(md.getPathLower());
                    } catch (DeleteErrorException e) {
                        System.err.println("Failed to delete " + md.getPathLower() + ": " + e.getMessage());
                    }
                }
                if (!result.getHasMore()) {
                    break;
                }
                result = client.files().listFolderContinue(result.getCursor());
            }
        } catch (ListFolderErrorException e) {
            // Folder might not exist, ignore
            System.out.println("Dropbox folder does not exist yet: " + folderPath);
        }
    }
}
