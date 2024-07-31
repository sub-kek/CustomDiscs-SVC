package io.github.subkek.customdiscs.util;

public class Formatter {
  public static String format(String str, String... replace) {
    for (int i = 0; i <= replace.length - 1; i++) {
      str = str.replace("{%d}".formatted(i), replace[i]);
    }
    return str;
  }
}