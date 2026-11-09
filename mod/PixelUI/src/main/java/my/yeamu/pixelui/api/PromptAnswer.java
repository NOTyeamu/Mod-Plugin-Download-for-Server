package my.yeamu.pixelui.api;

import java.util.Locale;

/** Ответ на вопрос-плашку. */
public enum PromptAnswer {
    /** Нажата клавиша «Да». */ YES,
    /** Нажата клавиша «Нет». */ NO,
    /** Время вышло. */ TIMEOUT,
    /** Вопрос убран (заменён, отменён, выход с сервера). */ CANCELLED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PromptAnswer parse(String s) {
        if (s == null) return CANCELLED;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CANCELLED;
        }
    }
}
