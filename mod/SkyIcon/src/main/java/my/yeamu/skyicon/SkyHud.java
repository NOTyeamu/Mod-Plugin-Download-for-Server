package my.yeamu.skyicon;

import my.yeamu.skyicon.mixin.BossBarHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.World;

/** Следит за временем мира и рисует иконку с анимацией «собирается из пикселей». */
public final class SkyHud {

    private static final long IN_MS = 500, OUT_MS = 600;
    /** Сколько тиков ждать после входа: время с сервера приходит не сразу. */
    private static final int JOIN_DELAY = 40;

    private static Boolean lastNight;
    private static int joinTicks;
    private static boolean showNight;
    private static long shownAt = -1;

    private SkyHud() {}

    // ------------------------------------------------------------------ логика

    public static void tick(MinecraftClient mc) {
        ClientWorld w = mc.world;
        if (w == null || mc.player == null) {
            lastNight = null;
            joinTicks = 0;
            shownAt = -1;
            return;
        }
        SkyConfig cfg = SkyConfig.get();
        // Солнце и луна есть только в обычном мире
        if (!cfg.enabled || w.getRegistryKey() != World.OVERWORLD) {
            lastNight = null;
            joinTicks = 0;
            return;
        }
        if (joinTicks < JOIN_DELAY) {
            joinTicks++;
            return;
        }
        long tod = Math.floorMod(w.getTimeOfDay(), 24000L);
        boolean night = tod >= cfg.nightStart && tod < cfg.nightEnd;
        if (lastNight == null) {
            if (cfg.showOnJoin) show(night, false);
        } else if (night != lastNight) {
            show(night, true);
        }
        lastNight = night;
    }

    /** Показать иконку (можно вызывать из других модов). */
    public static void show(boolean night, boolean withSound) {
        showNight = night;
        shownAt = Util.getMeasuringTimeMs();
        if (withSound && SkyConfig.get().sound) {
            Identifier id = Identifier.of("minecraft", "block.amethyst_block.chime");
            MinecraftClient.getInstance().getSoundManager()
                    .play(PositionedSoundInstance.ui(SoundEvent.of(id), night ? 0.7f : 1.4f));
        }
    }

    // ------------------------------------------------------------------ отрисовка

    public static void render(DrawContext c, RenderTickCounter tickCounter) {
        if (shownAt < 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.options.hudHidden) return;
        SkyConfig cfg = SkyConfig.get();

        long hold = (long) (cfg.seconds * 1000);
        long el = Util.getMeasuringTimeMs() - shownAt;
        if (el >= IN_MS + hold + OUT_MS) {
            shownAt = -1;
            return;
        }

        // reveal — какая доля пикселей видна; drop — въезд сверху «ступеньками» по 1 пикселю
        float reveal;
        int drop = 0;
        if (el < IN_MS) {
            float t = el / (float) IN_MS;
            reveal = t;
            drop = -Math.round((1f - easeOut(t)) * 6f);
        } else if (el < IN_MS + hold) {
            reveal = 1f;
        } else {
            float t = (el - IN_MS - hold) / (float) OUT_MS;
            reveal = 1f - t;
            drop = -Math.round(easeIn(t) * 4f);
        }
        // Лёгкое покачивание, пока висит
        int bob = (int) Math.round(Math.sin(el / 520.0) * 1.0);

        int s = cfg.scale;
        int size = SkyIcons.SIZE * s;
        int x = (c.getScaledWindowWidth() - size) / 2;
        int y = cfg.y + bossOffset(mc, cfg, c.getScaledWindowHeight()) + drop + bob;

        String[] grid = showNight ? SkyIcons.MOON : SkyIcons.SUN;
        float half = (SkyIcons.SIZE - 1) / 2f;
        float maxDist = (float) Math.hypot(half, half);
        for (int row = 0; row < grid.length; row++) {
            String r = grid[row];
            for (int col = 0; col < r.length(); col++) {
                int color = SkyIcons.color(r.charAt(col), showNight);
                if (color == 0) continue;
                // Порог пикселя: немного от центра наружу + случайный разброс → «рассыпается» по пикселям
                float dist = (float) Math.hypot(col - half, row - half) / maxDist;
                float threshold = 0.55f * noise(col, row) + 0.45f * dist;
                if (threshold >= reveal) continue;
                int px = x + col * s, py = y + row * s;
                c.fill(px, py, px + s, py + s, color);
            }
        }
    }

    /** Насколько опустить иконку, чтобы она не легла на полоски боссов. */
    private static int bossOffset(MinecraftClient mc, SkyConfig cfg, int screenH) {
        if (!cfg.avoidBossBars) return 0;
        int n;
        try {
            n = ((BossBarHudAccessor) mc.inGameHud.getBossBarHud()).skyicon$bossBars().size();
        } catch (Throwable t) {
            return 0;
        }
        if (n <= 0) return 0;
        // Ванильные полоски: первая на y=12, шаг 19, высота 5, не ниже трети экрана
        int bottom = Math.min(17 + (n - 1) * 19, screenH / 3);
        return bottom + 2;
    }

    private static float noise(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65536f;
    }

    private static float easeOut(float t) {
        float u = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - u * u * u;
    }

    private static float easeIn(float t) {
        float u = Math.max(0f, Math.min(1f, t));
        return u * u * u;
    }
}
