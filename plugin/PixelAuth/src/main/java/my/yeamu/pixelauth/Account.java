package my.yeamu.pixelauth;

/**
 * Запись аккаунта.
 * @param name      ник в нижнем регистре (ключ)
 * @param realName  ник как при регистрации
 * @param uuid      UUID игрока (может быть null у импортированных)
 * @param hash      хеш пароля (см. {@link Passwords})
 * @param lastLogin время последнего входа, мс (0 — сессия сброшена)
 */
public record Account(String name, String realName, String uuid, String hash,
                      String regIp, long regDate, String lastIp, long lastLogin) {

    public Account withHash(String h) {
        return new Account(name, realName, uuid, h, regIp, regDate, lastIp, lastLogin);
    }

    public Account withLogin(String ip, long time, String newUuid) {
        return new Account(name, realName, newUuid == null ? uuid : newUuid, hash, regIp, regDate, ip, time);
    }
}
