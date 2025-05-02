package io.github.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.AbstractSubCommand;
import org.apache.commons.io.FileUtils;
import org.bukkit.command.CommandSender;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadYtSubCommand extends AbstractSubCommand {
    private final CustomDiscs plugin = CustomDiscs.getPlugin();
    private static final String RAPID_API_HOST = "youtube-mp36.p.rapidapi.com";
    private static final String API_ENDPOINT = "https://" + RAPID_API_HOST + "/dl?id=";
    
    // YouTube URL patterns
    private static final Pattern YT_VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})"
    );

    public DownloadYtSubCommand() {
        super("downloadyt");

        this.withFullDescription(getDescription());
        this.withUsage(getSyntax());

        this.withArguments(new TextArgument("yt_link"));
        this.withArguments(new StringArgument("filename"));

        this.executes(this::execute);
    }

    @Override
    public String getDescription() {
        return plugin.getLanguage().string("command.downloadyt.description");
    }

    @Override
    public String getSyntax() {
        return plugin.getLanguage().string("command.downloadyt.syntax");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("customdiscs.downloadyt");
    }

    @Override
    public void execute(CommandSender sender, CommandArguments arguments) {
        if (!hasPermission(sender)) {
            CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.no-permission"));
            return;
        }

        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            try {
                String ytLink = getArgumentValue(arguments, "yt_link", String.class);
                String filename = getArgumentValue(arguments, "filename", String.class);

                // Validate filename to prevent directory traversal
                if (filename.contains("../") || filename.contains("..\\")) {
                    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.invalid-filename"));
                    return;
                }

                // Ensure filename ends with .mp3
                if (!filename.toLowerCase().endsWith(".mp3")) {
                    filename = filename + ".mp3";
                }

                // Create musicdata directory if it doesn't exist
                Path musicDataPath = Path.of(plugin.getDataFolder().getPath(), "musicdata");
                File musicDataDir = new File(musicDataPath.toUri());
                if (!musicDataDir.exists()) {
                    musicDataDir.mkdirs();
                }

                // Full path to save the file
                File downloadFile = new File(musicDataDir, filename);

                // Notify user that download is starting
                CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.downloadyt.messages.downloading"));

                // Extract video ID from URL
                String videoId = extractVideoId(ytLink);
                if (videoId == null) {
                    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.downloadyt.messages.error.invalid-url"));
                    return;
                }

                CustomDiscs.debug("Extracted video ID: " + videoId);

                // Make request to RapidAPI
                String mp3DownloadUrl = getMp3DownloadUrl(videoId);
                if (mp3DownloadUrl == null) {
                    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.downloadyt.messages.error.while-download"));
                    return;
                }
                
                CustomDiscs.debug("Got MP3 download URL: " + mp3DownloadUrl);
                
                // Wait a bit for the URL to be fully processed by the API provider
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Download the MP3 file
                boolean success = downloadMp3(mp3DownloadUrl, downloadFile);
                if (!success) {
                    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.downloadyt.messages.error.while-download"));
                    return;
                }

                // Download successful
                CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.downloadyt.messages.successfully"));
                CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.downloadyt.messages.create-tooltip",
                        plugin.getLanguage().string("command.create.syntax")));

            } catch (Throwable e) {
                CustomDiscs.error("Error while downloading YouTube music: ", e);
                CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.downloadyt.messages.error.while-download"));
            }
        });
    }

    /**
     * Extracts the YouTube video ID from various URL formats
     * @param ytLink The YouTube URL or ID
     * @return The video ID or null if not found
     */
    private String extractVideoId(String ytLink) {
        // Check if it's already just a video ID (11 characters)
        if (ytLink.matches("[a-zA-Z0-9_-]{11}")) {
            return ytLink;
        }

        // Try to extract ID using regex pattern
        Matcher matcher = YT_VIDEO_ID_PATTERN.matcher(ytLink);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Makes a request to RapidAPI to get the MP3 download URL
     * @param videoId The YouTube video ID
     * @return The direct MP3 download URL or null if failed
     */
    private String getMp3DownloadUrl(String videoId) {
        int retries = 0;
        String mp3DownloadUrl = null;

        while (retries < 100) { // Retry up to 3 times
            try {
                CustomDiscs.debug("Making RapidAPI request for video ID: " + videoId + " (Attempt " + (retries + 1) + ")");

                URL url = new URL(API_ENDPOINT + videoId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-RapidAPI-Key", plugin.getCDConfig().getRapidApiKey()); // Use API key from config
                conn.setRequestProperty("X-RapidAPI-Host", RAPID_API_HOST);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    CustomDiscs.error("RapidAPI request failed with response code: " + responseCode);
                    retries++;
                    Thread.sleep(2000); // Wait before retrying
                    continue; // Retry
                }

                // Read and parse the JSON response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                String responseStr = response.toString();
                CustomDiscs.debug("Raw API response: " + responseStr);

                JSONObject jsonResponse = new JSONObject(responseStr);
                CustomDiscs.debug("API JSON response: " + jsonResponse.toString(2));

                // Check if the response is valid
                if (!jsonResponse.has("status")) {
                    CustomDiscs.error("RapidAPI response missing status.");
                    retries++;
                    Thread.sleep(2000); // Wait before retrying
                    continue; // Retry
                }

                String status = jsonResponse.getString("status");
                if (status.equals("ok")) {
                    if (!jsonResponse.has("link")) {
                        CustomDiscs.error("RapidAPI response missing download URL.");
                        return null; // Fatal error, no link
                    }
                    mp3DownloadUrl = jsonResponse.getString("link");
                    break; // Success, exit loop
                } else if (status.equals("in process")) {
                    CustomDiscs.debug("RapidAPI response status: in process. Retrying...");
                    retries++;
                    Thread.sleep(2000); // Wait before retrying
                } else {
                    CustomDiscs.error("RapidAPI response status not ok: " +
                        jsonResponse.optString("msg", "Unknown error"));
                    return null; // Fatal error, status not ok or in process
                }

            } catch (Exception e) {
                CustomDiscs.error("Error getting MP3 download URL: ", e);
                retries++;
                try {
                    Thread.sleep(2000); // Wait before retrying on exception
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null; // Interrupted
                }
            }
        }

        return mp3DownloadUrl; // Returns null if all retries fail
    }

    /**
     * Downloads the MP3 file from the given URL
     * @param mp3Url The direct MP3 download URL
     * @param destination The file to save the MP3 to
     * @return true if successful, false otherwise
     */
    private boolean downloadMp3(String mp3Url, File destination) {
        try {
            // The API returns URLs with escaped slashes, need to unescape them
            mp3Url = mp3Url.replace("\\/", "/");
            
            // Properly encode the URL to handle special characters
            URL safeUrl = new URL(mp3Url);
            
            CustomDiscs.debug("Attempting to download from URL: " + mp3Url);
            
            HttpURLConnection downloadConn = (HttpURLConnection) safeUrl.openConnection();
            downloadConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            downloadConn.setRequestProperty("Accept", "*/*");
            downloadConn.setRequestProperty("Referer", "https://youtube-mp36.p.rapidapi.com/");
            downloadConn.setConnectTimeout(30000);
            downloadConn.setReadTimeout(60000); // Longer timeout for download
            downloadConn.setInstanceFollowRedirects(true);

            int status = downloadConn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                CustomDiscs.error("Download URL returned status: " + status);
                CustomDiscs.debug("Failed URL: " + mp3Url);
                return false;
            }

            // Download the file
            try (java.io.InputStream in = downloadConn.getInputStream()) {
                FileUtils.copyInputStreamToFile(in, destination);
                
                // Verify file was downloaded
                if (destination.exists() && destination.length() > 0) {
                    CustomDiscs.debug("Successfully downloaded MP3 file to: " + destination.getAbsolutePath());
                    return true;
                } else {
                    CustomDiscs.error("File download failed or produced empty file");
                    return false;
                }
            }
        } catch (Exception e) {
            CustomDiscs.error("Error downloading MP3 file: ", e);
            return false;
        }
    }
}