package io.github.subkek.customdiscs.utils;

public enum Languages {
  RUSSIAN("ru_RU"),
  ENGLISH("en_US");

  private String title;

  Languages(String title) {
    this.title = title;
  }

  @Override
  public String toString() {
    return title;
  }

  public static boolean languageExists(String title) {
    for (Languages lang : Languages.values()) {
      if (lang.toString().equalsIgnoreCase(title)) return true;
    }
    return false;
  }
}
