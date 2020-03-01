package uk.me.desert_island.rer.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.me.desert_island.rer.client.ClientLootCache;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
    @Shadow private PacketListener packetListener;

    @Inject(method = "channelInactive", at = @At("RETURN"))
    private void onChannelInactive(ChannelHandlerContext context, CallbackInfo ci) {
        ClientLootCache.ID_TO_LOOT.clear();
    }
}
