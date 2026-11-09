package my.yeamu.pixelui.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Окно ввода PixelUI: заголовок, текст, одно или несколько полей (обычные или пароль)
 * и кнопки «Отправить» / «Отмена». Приходит от сервера пакетом {@code input}.
 */
public final class InputForm {

    /** Одно поле ввода. */
    public record Field(String label, String placeholder, String value, boolean password, int maxLength) { }

    public String id = "input";
    public String title = "";
    public String icon = "";
    public Theme theme = Theme.DARK;
    public int width = 240;
    public final List<String> lines = new ArrayList<>();
    public final List<Field> fields = new ArrayList<>();
    public String submit = "Готово";
    public String submitIcon = "";
    /** null — кнопки отмены нет. */
    public String cancel = "Отмена";
    public boolean closeable = true;
    /** Закрыть окно сразу после отправки (иначе ждём ответа сервера). */
    public boolean closeOnSubmit = true;
    /** Секунд до конца (только для отображения таймера), 0 — без таймера. */
    public int timer;
    public String error = "";

    private static String str(JsonObject o, String k, String def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }

    private static boolean bool(JsonObject o, String k, boolean def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsBoolean() : def;
    }

    private static int num(JsonObject o, String k, int def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : def;
    }

    public static InputForm fromJson(JsonObject o) {
        InputForm f = new InputForm();
        f.id = str(o, "id", "input");
        f.title = str(o, "title", "");
        f.icon = str(o, "icon", "");
        if (o.has("theme")) f.theme = Theme.fromJson(o.get("theme"));
        f.width = Math.max(160, Math.min(400, num(o, "width", 240)));
        if (o.has("lines") && o.get("lines").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("lines")) f.lines.add(e.isJsonNull() ? "" : e.getAsString());
        }
        if (o.has("fields") && o.get("fields").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("fields");
            for (JsonElement e : arr) {
                if (!e.isJsonObject() || f.fields.size() >= 5) continue;
                JsonObject fo = e.getAsJsonObject();
                f.fields.add(new Field(str(fo, "label", ""), str(fo, "placeholder", ""), str(fo, "value", ""),
                        bool(fo, "password", false), Math.max(1, Math.min(256, num(fo, "maxLength", 64)))));
            }
        }
        if (f.fields.isEmpty()) f.fields.add(new Field("", "", "", false, 64));
        f.submit = str(o, "submit", "Готово");
        f.submitIcon = str(o, "submitIcon", "");
        f.cancel = o.has("cancel") && !o.get("cancel").isJsonNull() ? o.get("cancel").getAsString() : null;
        if (f.cancel != null && f.cancel.isEmpty()) f.cancel = null;
        f.closeable = bool(o, "closeable", true);
        f.closeOnSubmit = bool(o, "closeOnSubmit", true);
        f.timer = Math.max(0, num(o, "timer", 0));
        f.error = str(o, "error", "");
        return f;
    }
}
