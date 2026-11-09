package my.yeamu.modgate.gate;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Состояние проверки одного игрока. Меняется только из главного потока.
 */
public final class PlayerState {

    /** Итог проверки. */
    public enum Status { PENDING, OK, FAILED, BYPASS, EXEMPT, BEDROCK, DISABLED }

    /** Причина отказа. */
    public enum Reason { NO_MOD, OUTDATED_MOD, OUTDATED_MANIFEST, FOREIGN_MODS, BAD_HANDSHAKE }

    final UUID uuid;
    final String name;
    final long joinedAt = System.currentTimeMillis();
    Status status = Status.PENDING;
    Reason reason;
    HandshakeData handshake;
    /** Манифест уже перепроверяли для этого входа. */
    boolean rechecked;
    /** Идёт перекачка манифеста — новые рукопожатия только запоминаем. */
    boolean rechecking;
    long quarantineStartedAt;
    BukkitTask timeoutTask;
    BukkitTask kickTask;
    BukkitTask reminderTask;

    PlayerState(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    void cancelTasks() {
        if (timeoutTask != null) { timeoutTask.cancel(); timeoutTask = null; }
        if (kickTask != null) { kickTask.cancel(); kickTask = null; }
        if (reminderTask != null) { reminderTask.cancel(); reminderTask = null; }
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public Status status() { return status; }
    public Reason reason() { return reason; }
    public HandshakeData handshake() { return handshake; }
    public long joinedAt() { return joinedAt; }
}
