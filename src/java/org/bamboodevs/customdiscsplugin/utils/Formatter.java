package org.bamboodevs.customdiscsplugin.utils;

import org.bamboodevs.customdiscsplugin.CustomDiscs;

public class Formatter {
    public static String format(String str, String... replacers) {
        for (int i = 0; i <= replacers.length-1; i++) {
            str = str.replace("{"+i+"}", replacers[i]);
        }
        return str;
    }

    public static String format(String str, boolean prefix, String... replacers) {
        if (prefix) {
            str = CustomDiscs.getInstance().language.get("prefix")+format(str, replacers);
        } else {
            str = format(str, replacers);
        }
        return str;
    }
}