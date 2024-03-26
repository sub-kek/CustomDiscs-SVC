package io.github.subkek.customdiscs.libs;

import io.github.subkek.customdiscs.utils.HTTPRequestUtils;
import io.github.subkek.customdiscs.utils.TriConsumer;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class LibraryDownloadManager {
  public static final String LIBS_DATA_URL = "https://pastebin.com/raw/D4pws17f";

  private File libsFolder;
  private JSONObject data;

  public LibraryDownloadManager(File libsFolder) {
    this.libsFolder = libsFolder;
    this.data = null;
  }

  private void ensureData() {
    if (data == null) {
      data = HTTPRequestUtils.getJSONResponse(LIBS_DATA_URL);
    }
  }

  public String getHash() {
    ensureData();
    return data.get("hash").toString();
  }

  public synchronized void downloadLibraries(TriConsumer<Boolean, String, Double> progressListener) {
    ensureData();
    try {
      JSONObject libs = (JSONObject) data.get("libs");
      Set<String> jarNames = new HashSet<>();
      double total = libs.keySet().size();
      double current = 0.0;
      for (Object key : libs.keySet()) {
        String jarName = (String) key;
        jarNames.add(jarName);
        JSONObject details = (JSONObject) libs.get(jarName);
        String url = (String) details.get("url");
        File jarFile = new File(libsFolder, jarName);
        current += 1.0;
        if (HTTPRequestUtils.download(jarFile, url)) {
          progressListener.accept(true, jarName, (current / total) * 100);
        } else {
          progressListener.accept(false, jarName, (current / total) * 100);
        }
      }
      for (File jarFile : libsFolder.listFiles()) {
        if (!jarNames.contains(jarFile.getName())) {
          jarFile.delete();
        }
      }
    } catch (Exception e) {

      e.printStackTrace();
    }
  }
}
