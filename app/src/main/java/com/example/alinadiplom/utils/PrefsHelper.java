package com.example.alinadiplom.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsHelper {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_PUSH_ENABLED = "push_enabled";
    private static final String KEY_LAST_ENABLED_TS = "last_enabled_ts";

    /** Включены ли пуши */
    public static boolean isPushEnabled(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PUSH_ENABLED, true);
    }

    /** Сохраняем состояние пушей */
    public static void setPushEnabled(Context ctx, boolean enabled) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit()
                .putBoolean(KEY_PUSH_ENABLED, enabled);
        if (enabled) {
            // запоминаем время включения
            e.putLong(KEY_LAST_ENABLED_TS, System.currentTimeMillis());
        }
        e.apply();
    }

    /** Когда последний раз включили пуши (ms) */
    public static long getLastEnabledTime(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_ENABLED_TS, 0L);
    }
}
