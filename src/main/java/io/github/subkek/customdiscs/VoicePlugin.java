package io.github.subkek.customdiscs;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.Level;

public class VoicePlugin implements VoicechatPlugin {
  public static String MUSIC_DISC_CATEGORY = "music_discs";

  public static VoicechatApi voicechatApi;
  public static VoicechatServerApi voicechatServerApi;
  public static VolumeCategory musicDiscs;

  @Override
  public String getPluginId() {
    return CustomDiscs.PLUGIN_ID;
  }

  @Override
  public void initialize(VoicechatApi api) {
    voicechatApi = api;
  }

  @Override
  public void registerEvents(EventRegistration registration) {
    registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
  }

  public void onServerStarted(VoicechatServerStartedEvent event) {
    voicechatServerApi = event.getVoicechat();

    musicDiscs = voicechatServerApi.volumeCategoryBuilder()
        .setId(MUSIC_DISC_CATEGORY)
        .setName("Music Discs")
        .setIcon(getMusicDiscIcon())
        .build();
    voicechatServerApi.registerVolumeCategory(musicDiscs);

  }

  private int[][] getMusicDiscIcon() {
    try {
      Enumeration<URL> resources = this.getClass().getClassLoader().getResources("music_disc_category.png");

      while (resources.hasMoreElements()) {
        BufferedImage bufferedImage = ImageIO.read(resources.nextElement().openStream());
        if (bufferedImage.getWidth() != 16) {
          continue;
        }
        if (bufferedImage.getHeight() != 16) {
          continue;
        }
        int[][] image = new int[16][16];
        for (int x = 0; x < bufferedImage.getWidth(); x++) {
          for (int y = 0; y < bufferedImage.getHeight(); y++) {
            image[x][y] = bufferedImage.getRGB(x, y);
          }
        }
        return image;
      }
    } catch (Throwable e) {
      CustomDiscs.getInstance().getLogger().log(Level.SEVERE, "Error getting music discs icon");
    }
    return null;
  }
}
