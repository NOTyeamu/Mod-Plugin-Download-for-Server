package my.yeamu.modsync.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Сервер → клиент, тот же канал modgate:handshake.
 * action = "request" (пришли рукопожатие) или "result" (итог проверки в text).
 */
public record ServerMessagePayload(String action, String text) implements CustomPayload {

    public static final CustomPayload.Id<ServerMessagePayload> ID = new CustomPayload.Id<>(HandshakePayload.CHANNEL);
    public static final PacketCodec<PacketByteBuf, ServerMessagePayload> CODEC =
            CustomPayload.codecOf(ServerMessagePayload::write, ServerMessagePayload::new);

    private ServerMessagePayload(PacketByteBuf buf) {
        this(buf.readString(), buf.readableBytes() > 0 ? buf.readString() : "");
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(action);
        buf.writeString(text);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
