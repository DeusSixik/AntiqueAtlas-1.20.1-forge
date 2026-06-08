package hunternif.mc.impl.atlas.network.packet.s2c.play;

import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;
import hunternif.mc.impl.atlas.AntiqueAtlas;
import hunternif.mc.impl.atlas.identity.AtlasIdentityService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class SyncAtlasNameS2CPacket extends ClientboundUnionPacket {
    public static final ResourceLocation ID = AntiqueAtlas.id("packet", "s2c", "atlas", "name", "sync");

    private final int atlasId;
    private final String atlasName;

    public SyncAtlasNameS2CPacket(int atlasId, String atlasName) {
        super(AntiqueAtlas.instance.channel);
        this.atlasId = atlasId;
        this.atlasName = atlasName;
    }

    public SyncAtlasNameS2CPacket(FriendlyByteBuf byteBuf) {
        super(byteBuf, AntiqueAtlas.instance.channel);
        this.atlasId = byteBuf.readVarInt();
        this.atlasName = byteBuf.readBoolean() ? byteBuf.readUtf() : null;
    }

    @Override
    public void encode(FriendlyByteBuf byteBuf) {
        byteBuf.writeVarInt(atlasId);
        byteBuf.writeBoolean(atlasName != null);
        if (atlasName != null) {
            byteBuf.writeUtf(atlasName);
        }
    }

    @Override
    public boolean runOnClient(Player sender) {
        AtlasIdentityService.setClientAtlasName(atlasId, atlasName);
        AtlasIdentityService.refreshClientAtlasStacks(sender, atlasId);
        return true;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
