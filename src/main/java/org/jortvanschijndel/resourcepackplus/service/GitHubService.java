package org.jortvanschijndel.resourcepackplus.service;

import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubService {
    private final GitHub gh;
    private final String token;

    public GitHubService(String token) throws Exception {
        this.token = token;
        this.gh = new GitHubBuilder()
                .withOAuthToken(token)
                .build();

        GHRateLimit limit = gh.getRateLimit();

        if (limit == null) {
            throw new IllegalStateException("Unable to read GitHub rate limit.");
        }
    }

    public String getLoginName() throws Exception {
        String login = gh.getMyself().getLogin();
        return login;
    }

    public String parseOwnerRepoFromUrl(String url) {
        if (url == null) return null;

        Pattern p1 = Pattern.compile("github\\.com[:/]+([^/]+)/([^/.]+)(?:\\.git)?/?$");
        Matcher m1 = p1.matcher(url.trim());
        if (m1.find()) {
            return m1.group(1) + "/" + m1.group(2);
        }

        Pattern p2 = Pattern.compile("git@github\\.com:([^/]+)/([^/.]+)(?:\\.git)?$");
        Matcher m2 = p2.matcher(url.trim());
        if (m2.find()) {
            return m2.group(1) + "/" + m2.group(2);
        }

        return null;
    }

    /**
     * Gets the GitHub API URL for downloading repository zipball
     */
    public URL getZipballUrl(String ownerRepo, String branch) throws IOException {

        // GitHub API endpoint for zipball downloads
        String urlString = String.format("https://api.github.com/repos/%s/zipball/%s", ownerRepo, branch);
        return new URL(urlString);
    }

    /**
     * Creates an authenticated InputStream for downloading from GitHub API
     * This handles the authentication and follows redirects properly
     */
    public InputStream getAuthenticatedStream(URL url) throws IOException {

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set authentication header
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "ResourcepackPlus/1.0");

        // GitHub API returns 302 redirect to actual download URL
        connection.setInstanceFollowRedirects(true);

        // Connect and handle response
        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        } else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
            // Handle redirect manually if needed
            String redirectUrl = connection.getHeaderField("Location");
            connection.disconnect();
            return getAuthenticatedStream(new URL(redirectUrl));
        } else {
            String errorMsg = "HTTP " + responseCode + ": " + connection.getResponseMessage();
            connection.disconnect();
            throw new IOException("Failed to download zipball: " + errorMsg);
        }
    }
}