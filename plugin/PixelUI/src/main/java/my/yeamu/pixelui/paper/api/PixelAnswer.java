package my.yeamu.pixelui.paper.api;

import java.util.Locale;

/** Ответ игрока на вопрос {@link PixelPrompt}. */
public enum PixelAnswer {
    /** Нажал «Да» (клавиша Y или кнопка в чате). */ YES,
    /** Нажал «Нет» (клавиша N или кнопка в чате). */ NO,
    /** Время на ответ вышло. */ TIMEOUT,
    /** Вопрос отменён: заменён новым с тем же id, cancelPrompt, clear, выход игрока. */ CANCELLED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** null, если строка не распознана. */
    public static PixelAnswer parse(String s) {
        if (s == null) return null;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
