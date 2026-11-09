package my.yeamu.modsync.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Клиент → сервер, канал modgate:handshake.
 * Формат: String версия мода, VarInt версия манифеста, String хеш модов,
 * String версия игры, VarInt N и N строк "sha256:файл".
 */
public record HandshakePayload(String modVersion, int manifestVersion, String modsHash,
                               String gameVersion, List<String> files) implements CustomPayload {

    public static final Identifier CHANNEL = Identifier.of("modgate", "handshake");
    public static final CustomPayload.Id<HandshakePayload> ID = new CustomPayload.Id<>(CHANNEL);
    public static final PacketCodec<PacketByteBuf, HandshakePayload> CODEC =
            CustomPayload.codecOf(HandshakePayload::write, HandshakePayload::new);

    private HandshakePayload(PacketByteBuf buf) {
        this(buf.readString(), buf.readVarInt(), buf.readString(), buf.readString(), readFiles(buf));
    }

    private static List<String> readFiles(PacketByteBuf buf) {
        List<String> out = new ArrayList<>();
        if (buf.readableBytes() == 0) return out;
        int n = Math.min(buf.readVarInt(), 512);
        for (int i = 0; i < n; i++) out.add(buf.readString());
        return out;
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(modVersion);
        buf.writeVarInt(manifestVersion);
        buf.writeString(modsHash);
        buf.writeString(gameVersion);
        buf.writeVarInt(files.size());
        for (String f : files) buf.writeString(f);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
