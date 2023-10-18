package org.bamboodevs.customdiscsplugin.utils;

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
}
