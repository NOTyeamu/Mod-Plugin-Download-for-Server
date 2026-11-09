package my.yeamu.modgate.gate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Данные рукопожатия от клиента.
 *
 * Формат пакета (как PacketByteBuf в Minecraft):
 *   String  modVersion        (VarInt длина + UTF-8)
 *   VarInt  manifestVersion
 *   String  modsHash          (hex SHA-256)
 *   String  gameVersion
 *   VarInt  количество файлов, затем для каждого: String "sha256:имя_файла"
 */
public record HandshakeData(String modVersion, int manifestVersion, String modsHash,
                            String gameVersion, List<ModFile> files) {

    public record ModFile(String sha256, String fileName) {}

    private static final int MAX_STRING_BYTES = 1024;
    private static final int MAX_FILES = 512;

    public static HandshakeData decode(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        String modVersion = readString(in);
        int manifestVersion = readVarInt(in);
        String hash = readString(in);
        String game = readString(in);
        List<ModFile> files = new ArrayList<>();
        if (in.available() > 0) {
            int count = readVarInt(in);
            if (count < 0 || count > MAX_FILES) throw new IOException("слишком много файлов: " + count);
            for (int i = 0; i < count; i++) {
                String entry = readString(in);
                int idx = entry.indexOf(':');
                if (idx > 0) files.add(new ModFile(entry.substring(0, idx), entry.substring(idx + 1)));
            }
        }
        return new HandshakeData(modVersion, manifestVersion, hash, game, List.copyOf(files));
    }

    static int readVarInt(DataInputStream in) throws IOException {
        int value = 0, position = 0;
        while (true) {
            byte b = in.readByte();
            value |= (b & 0x7F) << position;
            if ((b & 0x80) == 0) return value;
            position += 7;
            if (position >= 32) throw new IOException("VarInt слишком длинный");
        }
    }

    static String readString(DataInputStream in) throws IOException {
        int len = readVarInt(in);
        if (len < 0 || len > MAX_STRING_BYTES) throw new IOException("некорректная длина строки");
        byte[] buf = new byte[len];
        in.readFully(buf);
        return new String(buf, StandardCharsets.UTF_8);
    }

    /** Пакет сервер → клиент: String action, String text. */
    public static byte[] encodeServerMessage(String action, String text) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, action);
        writeString(out, text);
        return out.toByteArray();
    }

    private static void writeString(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, b.length);
        out.writeBytes(b);
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }
}
