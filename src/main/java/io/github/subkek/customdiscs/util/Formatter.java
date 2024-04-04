package io.github.subkek.customdiscs.util;

import io.github.subkek.customdiscs.CustomDiscs;

public class Formatter {
  public static String format(String str, String... replace) {
    for (int i = 0; i <= replace.length - 1; i++) {
      str = str.replace("{%d}".formatted(i), replace[i]);
    }
    return str;
  }

  public static String format(String str, boolean prefix, String... replace) {
    if (prefix) {
      str = CustomDiscs.getInstance().language.get("prefix") + format(str, replace);
    } else {
      str = format(str, replace);
    }
    return str;
  }
}