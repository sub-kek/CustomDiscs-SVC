package io.github.subkek.customdiscs.libs;

import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class LibraryLoader {

  private static final URLClassLoaderAccess LOADER_ACCESS = URLClassLoaderAccess.create((URLClassLoader) LibraryLoader.class.getClassLoader());
  private static final BiConsumer<File, Throwable> NOOP_LISTENER = (file, e) -> {};
  private static final List<Relocation> RELOCATION_RULES = new ArrayList<>();
  private static final List<String> DONT_RELOCATE = new ArrayList<>();

  static {
    RELOCATION_RULES.add(new Relocation(dot("net{}kyori"), "io.github.subkek.customdiscs.libs.net.kyori"));

    DONT_RELOCATE.add("jackson-core");
  }

  private static String dot(String str) {
    return str.replace("{}", ".");
  }

  public static void loadLibraries(File libsFolder) {
    loadLibraries(libsFolder, NOOP_LISTENER, NOOP_LISTENER);
  }

  public static void loadLibraries(File libsFolder, BiConsumer<File, Throwable> remapListener, BiConsumer<File, Throwable> loadListener) {
    libsFolder.mkdirs();
    for (File jarFile : libsFolder.listFiles()) {
      String jarName = jarFile.getName();
      if (jarName.endsWith(".jar")) {
        String rawName = jarName.substring(0, jarName.length() - 4);
        if (!rawName.endsWith("-remapped")) {
          File remappedFile = new File(libsFolder, rawName + "-remapped.jar");
          if (remappedFile.exists()) {
            continue;
          }
          if (!dontRelocate(jarName)) {
            JarRelocator relocator = new JarRelocator(jarFile, remappedFile, RELOCATION_RULES);
            try {
              relocator.run();
              remapListener.accept(jarFile, null);
            } catch (Throwable e) {
              remapListener.accept(jarFile, e);
            }
          } else {
            try {
              Files.copy(jarFile.toPath(), remappedFile.toPath());
              remapListener.accept(jarFile, null);
            } catch (Throwable e) {
              remapListener.accept(jarFile, e);
            }
          }
        }
      }
    }
    for (File jarFile : libsFolder.listFiles()) {
      String jarName = jarFile.getName();
      if (jarName.endsWith(".jar")) {
        String rawName = jarName.substring(0, jarName.length() - 4);
        if (rawName.endsWith("-remapped")) {
          try {
            LOADER_ACCESS.addURL(jarFile.toURI().toURL());
            loadListener.accept(jarFile, null);
          } catch (Throwable e) {
            loadListener.accept(jarFile, e);
          }
        }
      }
    }
  }

  private static boolean dontRelocate(String jarName) {
    for (String dontRelocateRule : DONT_RELOCATE) {
      return jarName.contains(dontRelocateRule);
    }

    return false;
  }
}