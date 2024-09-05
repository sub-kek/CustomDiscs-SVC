package io.github.subkek.customdiscs.language;

import lombok.Getter;

import java.util.ArrayList;

@Getter
public enum Language {
  RUSSIAN("ru_RU"),
  ENGLISH("en_US");

  private final String label;

  Language(String title) {
    this.label = title;
  }

  public static String getAllSeparatedComma() {
    ArrayList<String> labels = new ArrayList<>();
    for (Language language : values()) {
      labels.add(language.getLabel());
    }
    return String.join(", ", labels);
  }
}
