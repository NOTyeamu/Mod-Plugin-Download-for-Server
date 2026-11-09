package my.yeamu.modsync;

import java.util.Set;

/**
 * Зашитые в мод настройки. Меняются только пересборкой мода —
 * так их нельзя подменить через конфиг-файл.
 */
public final class ModSyncConfig {

    private ModSyncConfig() {}

    /** Адрес манифеста (только HTTPS). Подпись лежит рядом: manifest.json.sig */
    public static final String MANIFEST_URL = "https://yeamu.my.id/manifest.json";

    /** Публичный ключ Ed25519 (X.509 DER, Base64), которым подписан манифест. */
    public static final String PUBLIC_KEY = "MCowBQYDK2VwAyEASH15CNNP+UybzhC9J0VyjZ/QEUBFTy/Fbum3wYcYsIM=";

    /** Домены, с которых разрешено скачивать моды (включая редиректы). */
    public static final Set<String> DOWNLOAD_WHITELIST = Set.of(
            "yeamu.my.id",
            "github.com",
            "objects.githubusercontent.com",
            "release-assets.githubusercontent.com",
            "raw.githubusercontent.com",
            "cdn.modrinth.com",
            "maven.fabricmc.net"
    );

    /** Максимальный размер одного мода (защита от бесконечной загрузки). */
    public static final long MAX_MOD_SIZE = 256L * 1024 * 1024;

    /** Таймауты сети в секундах. */
    public static final int CONNECT_TIMEOUT = 10;
    public static final int REQUEST_TIMEOUT = 120;
}
