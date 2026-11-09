package my.yeamu.pixelui.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Пакет канала pixelui:main в обе стороны. Внутри одна строка JSON:
 * {"type":"notify", ...}. Строка записана как VarInt-длина + UTF-8 (стандартный writeString).
 */
public record PixelPayload(String json) implements CustomPayload {

    public static final Identifier CHANNEL = Identifier.of("pixelui", "main");
    public static final int MAX = 262144;
    public static final CustomPayload.Id<PixelPayload> ID = new CustomPayload.Id<>(CHANNEL);
    public static final PacketCodec<PacketByteBuf, PixelPayload> CODEC =
            CustomPayload.codecOf(PixelPayload::write, PixelPayload::new);

    private PixelPayload(PacketByteBuf buf) {
        this(buf.readString(MAX));
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(json, MAX);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
