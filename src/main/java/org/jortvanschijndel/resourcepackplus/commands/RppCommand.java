package org.jortvanschijndel.resourcepackplus.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jortvanschijndel.resourcepackplus.ResourcepackPlus;
import org.jortvanschijndel.resourcepackplus.service.DropboxService;
import org.jortvanschijndel.resourcepackplus.service.GitHubService;
import org.jortvanschijndel.resourcepackplus.storage.TokenStore;
import org.jortvanschijndel.resourcepackplus.util.HashUtil;
import org.jortvanschijndel.resourcepackplus.util.Messaging;
import org.jortvanschijndel.resourcepackplus.util.ServerPropertiesUtil;
import org.jortvanschijndel.resourcepackplus.util.ZipUtil;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;



public class RppCommand implements CommandExecutor, TabCompleter, Listener {

    private final ResourcepackPlus plugin;
    private final TokenStore tokens;


    public RppCommand(ResourcepackPlus plugin) {
        this.plugin = plugin;
        this.tokens = plugin.getTokenStore();
    }

    private boolean checkPerm(CommandSender sender) {
        if (!sender.hasPermission("resourcepackplus.use")) {
            Messaging.sendMini(sender, "<red>[ResourcepackPlus] You don't have permission (resourcepackplus.use).");
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!checkPerm(sender)) return true;

        if (args.length == 0) {
            help(sender, label);
            return true;
        }

        final String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "github" -> handleGithub(sender, args);
            case "dropbox" -> handleDropbox(sender, args);
            case "dropbox-path" -> handleDropboxPath(sender, args);
            case "update" -> handleUpdate(sender, args);
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender sender, String label) {
        Messaging.sendMini(sender, "<aqua>ResourcepackPlus commands:");
        Messaging.sendMini(sender, "<yellow>/" + label + " github <accesstoken> <gray>— Set or learn how to get a GitHub Personal Access Token.");
        Messaging.sendMini(sender, "<yellow>/" + label + " dropbox <appkey> <appsecret> <gray>— Set or learn how to get a Dropbox access token.");
        Messaging.sendMini(sender, "<yellow>/" + label + " dropbox-path <path> <gray>— Set Dropbox folder path for uploads.");
        Messaging.sendMini(sender, "<yellow>/" + label + " update <GitHub URL> <Branch> <gray>— Build & upload resourcepack, update server.properties, and restart.");
    }

    private void handleGithub(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messaging.sendMini(sender, "<gold>[RPP] How to get a GitHub Access Token:");
            Messaging.sendMini(sender, "<gray>1) Visit https://github.com/settings/tokens");
            Messaging.sendMini(sender, "<gray>2) Create a classic token with at least 'repo' scope for private repos.");
            Messaging.sendMini(sender, "<gray>Then run: <yellow>/rpp github <token>");
            return;
        }
        final String token = args[1].trim();
        if (token.isEmpty()) {
            Messaging.sendMini(sender, "<red>[RPP] Provided token is empty.");
            return;
        }

        Messaging.sendMini(sender, "<gray>[RPP] Validating GitHub token...");
        System.out.println("[DEBUG] /rpp github called with token length: " + token.length());

        CompletableFuture.runAsync(() -> {
                    System.out.println("[DEBUG] Async GitHub token validation started...");
                    try {
                        GitHubService gh = new GitHubService(token);
                        String login = gh.getLoginName();
                        tokens.setGithubToken(token);
                        Messaging.sendMini(sender, "<green>[RPP] GitHub authentication successful as: <yellow>" + login);
                        System.out.println("[DEBUG] Token validation completed successfully for user: " + login);
                    } catch (Exception e) {
                        System.out.println("[DEBUG] Token validation failed with exception: " + e);
                        e.printStackTrace();
                        Messaging.sendMini(sender, "<red>[RPP] GitHub token validation failed: " + e.getMessage());
                    }
                }).orTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    System.out.println("[DEBUG] Token validation task timed out or failed: " + ex);
                    Messaging.sendMini(sender, "<red>[RPP] GitHub token validation timed out.");
                    return null;
                });
    }

    private final Map<UUID, PendingDropboxSetup> pendingDropboxSetups = new HashMap<>();

    public record PendingDropboxSetup(String appKey, String appSecret) {}

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (pendingDropboxSetups.containsKey(uuid)) {
            event.setCancelled(true); // Prevent showing in chat
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            String code = message.trim();
            PendingDropboxSetup setup = pendingDropboxSetups.remove(uuid);

            Messaging.sendMini(event.getPlayer(), "<gray>[RPP] Processing your Dropbox code...");

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    String refreshToken = exchangeCodeForRefreshToken(setup.appKey, setup.appSecret, code);

                    // Save credentials permanently
                    tokens.setDropboxCredentials(setup.appKey, setup.appSecret, refreshToken);

                    // Create DropboxService
                    DropboxService dbx = new DropboxService(
                            setup.appKey,
                            setup.appSecret,
                            refreshToken,
                            "ResourcepackPlus/1.0"
                    );
                    String accountName;
                    try {
                        accountName = dbx.getAccountName();
                    } catch (Exception e) {
                        accountName = "Unknown Account";
                        e.printStackTrace();
                    }

                    String finalAccountName = accountName;
                    Bukkit.getScheduler().runTask(plugin, () ->
                            Messaging.sendMini(event.getPlayer(),
                                    "<green>[RPP] Dropbox linked successfully to account: <yellow>"
                                            + finalAccountName
                            )
                    );
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            Messaging.sendMini(event.getPlayer(), "<red>[RPP] Dropbox linking failed: " + e.getMessage())
                    );
                }
            });
        }
    }


    private String exchangeCodeForRefreshToken(String appKey, String appSecret, String code) throws IOException, URISyntaxException {
        String auth = Base64.getEncoder().encodeToString((appKey + ":" + appSecret).getBytes(StandardCharsets.UTF_8));

        HttpURLConnection conn = (HttpURLConnection) new URI("https://api.dropboxapi.com/oauth2/token").toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + auth);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        String body = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (status < 200 || status >= 300) {
            throw new IOException("Dropbox API error: " + json);
        }

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("refresh_token")) {
            throw new IOException("No refresh token in response: " + json);
        }
        return obj.get("refresh_token").getAsString();
    }


    private void handleDropbox(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Messaging.sendMini(sender, "<red>[RPP] Only players can link Dropbox from in-game.");
            return;
        }

        if (args.length < 3) {
            Messaging.sendMini(sender, "<gold>[RPP] Link Dropbox (permanent setup):");
            Messaging.sendMini(sender, ",gray>Usage: /rpp dropbox <appKey> <appSecret>");
            Messaging.sendMini(sender, "<gray>1) Create a Dropbox app at: https://www.dropbox.com/developers/apps");
            Messaging.sendMini(sender, "<gray>2) Add 'files.content.write', 'files.content.read', 'sharing.write', 'sharing.read' permissions.");
            Messaging.sendMini(sender, "<gray>3) Run this command with your app key & secret.");
            return;
        }

        String appKey = args[1].trim();
        String appSecret = args[2].trim();

        pendingDropboxSetups.put(player.getUniqueId(), new PendingDropboxSetup(appKey, appSecret));

        String link = "https://www.dropbox.com/oauth2/authorize?client_id=" + appKey +
                "&response_type=code&token_access_type=offline";

        Messaging.sendMini(sender, "<gold>[RPP] Click this link in your browser to authorize Dropbox:");
        Messaging.sendMini(sender, "<yellow>" + link);
        Messaging.sendMini(sender, "<gray>After approving, paste the code here (without a slash). This will be private.");
    }


    private void handleDropboxPath(CommandSender sender, String[] args) {
        if (!tokens.hasDropboxCredentials()) {
            Messaging.sendMini(sender, "<red>[RPP] You are not logged into Dropbox yet. Use /rpp dropbox <appkey> <appsecret>first.");
            return;
        }
        if (args.length < 2) {
            Messaging.sendMini(sender, "<yellow>[RPP] Usage: /rpp dropbox-path <path>");
            Messaging.sendMini(sender, "<gray>Warning: If you run multiple servers, DO NOT use the same Dropbox path for all of them.");
            return;
        }
        String path = args[1].trim();
        if (!path.startsWith("/")) path = "/" + path;
        plugin.getConfig().set("dropboxPath", path);
        plugin.saveConfig();

        Messaging.sendMini(sender, "<green>[RPP] Dropbox upload path set to: <yellow" + path);
        Messaging.sendMini(sender, "<red>Warning: If you run multiple servers, do NOT point to the same Dropbox path.");
    }

    private void handleUpdate(CommandSender sender, String[] args) {
        // Args: update <GitHub URL> <Branch>
        if (args.length < 3) {
            Messaging.sendMini(sender, "<red>[RPP] Usage: /rpp update <GitHub URL> <Branch>");
            return;
        }
        if (!tokens.hasGithubToken()) {
            Messaging.sendMini(sender, "<red>[RPP] Missing GitHub token. Use /rpp github <token> first.");
            return;
        }
        if (!tokens.hasDropboxCredentials()) {
            Messaging.sendMini(sender, "<red>[RPP] Missing Dropbox token. Use /rpp dropbox <appkey> <appsecret> first.");
            return;
        }
        String path = plugin.getConfig().getString("dropboxPath");
        if (path == null || path.isBlank()) {
            Messaging.sendMini(sender, "<red>[RPP] Missing Dropbox path. Use /rpp dropbox-path <path>.");
            return;
        }

        String ghUrl = args[1];
        String branch = args[2];
        Messaging.sendMini(sender, "<gray>[RPP] Starting update…");
        Messaging.sendMini(sender, "<gray> - Verifying credentials & inputs");

        // Heavy lifting async
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
// Step 1: Login services
                    Messaging.sendMini(sender, "<gray>[RPP] Logging into GitHub…");
                    GitHubService gh = new GitHubService(tokens.getGithubToken());
                    String repoSlug = gh.parseOwnerRepoFromUrl(ghUrl);
                    if (repoSlug == null) {
                        Messaging.sendMini(sender, "<red>[RPP] Could not parse GitHub URL. Expected like: https://github.com/<owner>/<repo>");
                        return;
                    }
                    Messaging.sendMini(sender, "<green>[RPP] GitHub repo detected: <yellow" + repoSlug + " <gray>(branch " + branch + ")");

                    Messaging.sendMini(sender, "<gray>[RPP] Logging into Dropbox…");
                    DropboxService dbx = new DropboxService(
                            tokens.getDropboxAppKey(),
                            tokens.getDropboxAppSecret(),
                            tokens.getDropboxRefreshToken(),
                            "ResourcepackPlus/1.0"
                    );
                    Messaging.sendMini(sender, "<green>[RPP] Dropbox login OK. Upload path: <yellow>" + path);

// Step 2: Download resourcepack archive from GitHub
                    Messaging.sendMini(sender, "<gray>[RPP] Downloading repository ZIP from GitHub…");

                    // Prepare workspace in plugin folder
                    File workDir = new File(plugin.getDataFolder(), "work");
                    if (!workDir.exists() && !workDir.mkdirs()) {
                        Messaging.sendMini(sender, "<red>[RPP] Failed to create working directory: " + workDir.getAbsolutePath());
                        return;
                    }

                    String timestamp = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss").format(new Date());
                    File targetDir = new File(workDir, "resourcepack-" + timestamp);
                    if (!targetDir.exists() && !targetDir.mkdirs()) {
                        Messaging.sendMini(sender, "<red>[RPP] Failed to create target directory: " + targetDir.getAbsolutePath());
                        return;
                    }

                    String tempZipName = "temp-download-" + timestamp + ".zip";
                    String finalZipName = "Resourcepack-" + timestamp + ".zip";
                    File tempZipFile = new File(workDir, tempZipName);
                    File finalZipFile = new File(workDir, finalZipName);

                    // Download GitHub zipball (with nested folder structure)
                    try (InputStream in = gh.getAuthenticatedStream(gh.getZipballUrl(repoSlug, branch));
                         FileOutputStream out = new FileOutputStream(tempZipFile)) {
                        in.transferTo(out);
                    }

                    Messaging.sendMini(sender, "<green>[RPP] Downloaded ZIP: <yellow" +
                            tempZipFile.getName() + " <gray>(" + tempZipFile.length() + " bytes)");

                    //Repackage ZIP to remove nested folder structure
                    Messaging.sendMini(sender, "<gray>[RPP] Repackaging ZIP to correct structure…");

                    try {
                        ZipUtil.repackageZipball(tempZipFile, finalZipFile);
                        Messaging.sendMini(sender, "<green>[RPP] Repackaged ZIP: <yellow>" + finalZipFile.getName());

                        // Clean up temporary download
                        tempZipFile.delete();
                    } catch (IOException e) {
                        Messaging.sendMini(sender, "<red>[RPP] Failed to repackage ZIP: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }

                    //Extract the properly structured ZIP
                    Messaging.sendMini(sender, "<gray>[RPP] Extracting ZIP…");

                    try {
                        ZipUtil.unzipToDirectory(finalZipFile, targetDir);
                        Messaging.sendMini(sender, "<green>[RPP] Resourcepack extracted to: <yellow>" + targetDir.getAbsolutePath());
                    } catch (IOException e) {
                        Messaging.sendMini(sender, "<red>[RPP] Failed to unzip resourcepack: " + e.getMessage());
                        e.printStackTrace();
                    }


                    // Step 3: Calculate SHA1 of the final zip
                    Messaging.sendMini(sender, "<gray>[RPP] Calculating SHA1…");
                    String sha1 = HashUtil.sha1OfFile(finalZipFile);
                    Messaging.sendMini(sender, "<green>[RPP] SHA1: <yellow>" + sha1);

                    // Step 4: Upload to Dropbox
                    Messaging.sendMini(sender, "<gray>[RPP] Cleaning Dropbox folder before upload…");
                    dbx.clearFolder(path); // This will delete old resource packs

                    Messaging.sendMini(sender, "<gray>[RPP] Uploading to Dropbox…");
                    String dropboxPath = path.endsWith("/") ? path + finalZipFile.getName() : path + "/" + finalZipFile.getName();
                    dbx.uploadFile(finalZipFile, dropboxPath);
                    Messaging.sendMini(sender, "<green>[RPP] Uploaded to Dropbox at <yellow>" + dropboxPath);

                    // Step 5: Create/obtain share link and force direct download (?dl=1)
                    Messaging.sendMini(sender, "<gray>[RPP] Creating Dropbox share link…");
                    String share = dbx.createOrGetSharedLink(dropboxPath);
                    String direct;
                    if (share.contains("dl=0")) {
                        direct = share.replaceAll("([?&])dl=0", "$1dl=1");
                    } else {
                        direct = share.contains("?") ? share + "&dl=1" : share + "?dl=1";
                    }
                    Messaging.sendMini(sender, "<green>[RPP] Share link: <yellow>" + direct);

                    // Step 6: Update server.properties (UTF-8)
                    Messaging.sendMini(sender, "<gray>[RPP] Updating server.properties…");
                    File serverRoot = plugin.getDataFolder().getParentFile().getParentFile();
                    File serverProps = new File(serverRoot, "server.properties");
                    if (!serverProps.exists()) {
                        Messaging.sendMini(sender, "<red>[RPP] server.properties not found at: " + serverProps.getAbsolutePath());
                        return;
                    }

                    Properties p = ServerPropertiesUtil.load(serverProps);
                    p.setProperty("resource-pack", direct);
                    p.setProperty("resource-pack-sha1", sha1);
                    plugin.setResourcePackUrl(direct);
                    plugin.setResourcePackSha1(sha1);
                    ServerPropertiesUtil.save(serverProps, p, StandardCharsets.UTF_8);
                    Messaging.sendMini(sender, "<green>[RPP] server.properties updated.");

                    // Step 7: Delete Work folder
                    try {
                        Files.deleteIfExists(finalZipFile.toPath());
                        FileUtils.deleteDirectory(targetDir); // Apache Commons IO
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    // Step 8: Announce restart and restart after delay
                    boolean announce = plugin.getConfig().getBoolean("announceRestart", true);
                    int delaySec = Math.max(1, plugin.getConfig().getInt("restartDelaySeconds", 10));

                    if (announce) {
                        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<gold>[RPP] Server will restart in " + delaySec + " seconds to apply the new resource pack…")));
                    }

                    Messaging.sendMini(sender, "<green>[RPP] All steps complete. Scheduling restart in " + delaySec + "s…");

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        for (Player pl : Bukkit.getOnlinePlayers()) {
                            Messaging.sendMini(pl, "<gold>[RPP] Restarting now to apply the new resource pack…");
                        }
                        Bukkit.shutdown();
                    }, delaySec * 20L);

                } catch (Exception ex) {
                    Messaging.sendMini(sender, "<red>[RPP] Update failed: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("resourcepackplus.use")) return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("github", "dropbox", "dropbox-path", "update");
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "github" -> {
                    return List.of("Please enter GitHub Access Token");
                }
                case "dropbox" -> {
                    return List.of("Please enter Dropbox App Key");
                }
                case "dropbox-path" -> {
                    return List.of("Please enter Dropbox Path");
                }
                case "update" -> {
                    // Pull from config: githubRepositories list
                    List<String> repos = plugin.getConfig().getStringList("githubRepositories");
                    return repos.isEmpty() ? List.of("<GitHub Repo URL>") : repos;
                }
            }
        }

        if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "dropbox" -> {
                    return List.of("Please enter Dropbox App Secret");
                }
                case "update" -> {
                    // Pull from config: branches list
                    List<String> branches = plugin.getConfig().getStringList("branches");
                    return branches.isEmpty() ? List.of("main", "master") : branches;
                }
            }
        }

        return Collections.emptyList();
    }
}

