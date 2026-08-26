package org.newagecoding.yitaptap;

import android.content.Context;
import android.content.SharedPreferences;

public final class TapSettings {
    private TapSettings() {}

    private static final String PREFS = "yitaptap_config";
    private static final String KEY_INTERVAL = "interval_ms";
    private static final String KEY_DURATION = "duration_ms";
    private static final String KEY_REPETITIONS = "repetitions";
    private static final String KEY_DELAY = "start_delay_ms";
    public static final String KEY_TARGET_X = "target_x";
    public static final String KEY_TARGET_Y = "target_y";

    public static final class Config {
        public final long intervalMs;
        public final long durationMs;
        public final int repetitions;
        public final long startDelayMs;

        public Config(long intervalMs, long durationMs, int repetitions, long startDelayMs) {
            this.intervalMs = clamp(intervalMs, 10L, 60_000L);
            this.durationMs = clamp(durationMs, 1L, 2_000L);
            this.repetitions = Math.max(0, repetitions);
            this.startDelayMs = clamp(startDelayMs, 0L, 60_000L);
        }
    }

    public static Config load(Context context) {
        SharedPreferences p = prefs(context);
        return new Config(
                p.getLong(KEY_INTERVAL, 100L),
                p.getLong(KEY_DURATION, 25L),
                p.getInt(KEY_REPETITIONS, 0),
                p.getLong(KEY_DELAY, 0L)
        );
    }

    public static void save(Context context, Config config) {
        prefs(context).edit()
                .putLong(KEY_INTERVAL, config.intervalMs)
                .putLong(KEY_DURATION, config.durationMs)
                .putInt(KEY_REPETITIONS, config.repetitions)
                .putLong(KEY_DELAY, config.startDelayMs)
                .apply();
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
