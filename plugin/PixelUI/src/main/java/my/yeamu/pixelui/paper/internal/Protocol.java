package my.yeamu.pixelui.paper.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Кодирование пакетов: VarInt длина + UTF-8 JSON. */
public final class Protocol {

    /** Как MAX в клиентском PixelPayload. */
    public static final int MAX_CHARS = 262144;

    private Protocol() {}

    public static byte[] encode(JsonObject o) {
        String s = o.toString();
        if (s.length() > MAX_CHARS) throw new IllegalArgumentException("PixelUI: пакет слишком большой (" + s.length() + " символов)");
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(b.length + 5);
        int v = b.length;
        while ((v & ~0x7F) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v);
        out.write(b, 0, b.length);
        return out.toByteArray();
    }

    /** null — пакет битый. */
    public static JsonObject decode(byte[] data) {
        try {
            int pos = 0, len = 0, shift = 0;
            while (true) {
                if (pos >= data.length || shift > 28) return null;
                int b = data[pos++] & 0xFF;
                len |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
            }
            if (len < 0 || len > MAX_CHARS * 3 || pos + len > data.length) return null;
            JsonElement e = JsonParser.parseString(new String(data, pos, len, StandardCharsets.UTF_8));
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
