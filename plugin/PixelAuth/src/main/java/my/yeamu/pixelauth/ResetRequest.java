package my.yeamu.pixelauth;

/**
 * Запрос сброса пароля.
 *
 * @param name     ник в нижнем регистре
 * @param realName ник как есть
 * @param ip       IP, с которого пришёл запрос (новый пароль можно задать только с него)
 * @param created  когда создан (мс)
 * @param status   pending — ждёт решения, approved — одобрен, denied — отклонён
 * @param decided  когда админ принял решение (мс), 0 — ещё нет
 */
public record ResetRequest(String name, String realName, String ip, long created, String status, long decided) {

    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String DENIED = "denied";

    public boolean pending() { return PENDING.equals(status); }

    public boolean approved() { return APPROVED.equals(status); }

    public boolean denied() { return DENIED.equals(status); }

    public ResetRequest decide(String newStatus, long time) {
        return new ResetRequest(name, realName, ip, created, newStatus, time);
    }
}
