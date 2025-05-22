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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class DownloadYtSubCommand extends AbstractSubCommand {
    private final CustomDiscs plugin = CustomDiscs.getPlugin();
    private static final String CUSTOM_API_ENDPOINT = "http://130.250.191.235:9000/";
    
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
                
                CustomDiscs.debug("Processing YouTube link: " + ytLink);
                
                // Get download URL from custom API
                String mp3DownloadUrl = getDownloadUrlFromCustomApi(ytLink);
                if (mp3DownloadUrl == null) {
                    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.downloadyt.messages.error.while-download"));
                    return;
                }
                
                CustomDiscs.debug("Got MP3 download URL: " + mp3DownloadUrl);

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
     * Makes a request to custom API to get the MP3 download URL
     * @param ytLink The full YouTube URL
     * @return The direct MP3 download URL or null if failed
     */
    private String getDownloadUrlFromCustomApi(String ytLink) {
        try {
            CustomDiscs.debug("Making API request for YouTube URL: " + ytLink);
            
            // Prepare the connection
            URL url = new URL(CUSTOM_API_ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            
            // Prepare the request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("url", ytLink);
            requestBody.put("audioFormat", "mp3");
            requestBody.put("downloadMode", "audio");
            
            // Send the request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                CustomDiscs.error("API request failed with response code: " + responseCode);
                return null;
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
            
            // Check if the response status is valid
            if (!jsonResponse.has("status") || !jsonResponse.getString("status").equals("tunnel")) {
                CustomDiscs.error("Invalid API response status");
                return null;
            }

            if (!jsonResponse.has("url")) {
                CustomDiscs.error("API response missing download URL.");
                return null;
            }

            // Return the direct download link
            return jsonResponse.getString("url");

        } catch (Exception e) {
            CustomDiscs.error("Error getting MP3 download URL from API: ", e);
            return null;
        }
    }

    /**
     * Downloads the MP3 file from the given URL
     * @param mp3Url The direct MP3 download URL
     * @param destination The file to save the MP3 to
     * @return true if successful, false otherwise
     */
        private boolean downloadMp3(String mp3Url, File destination) {
        try {
            CustomDiscs.debug("Attempting to download from URL: " + mp3Url);
            
            // Create URL object
            URL safeUrl = new URL(mp3Url);
            
            // Set up connection
            HttpURLConnection downloadConn = (HttpURLConnection) safeUrl.openConnection();
            downloadConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            downloadConn.setRequestProperty("Accept", "*/*");
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
