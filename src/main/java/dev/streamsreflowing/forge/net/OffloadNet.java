package dev.streamsreflowing.forge.net;

import dev.streamsreflowing.offload.OffloadClient;
import dev.streamsreflowing.offload.PlateOffload;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.simple.SimpleChannel;

public final class OffloadNet {
   private static final String PROTOCOL = "1";
   public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation("streamsreflowing", "regions"), () -> "1", NetworkRegistry.acceptMissingOr("1"), NetworkRegistry.acceptMissingOr("1")
   );

   private OffloadNet() {
   }

   public static void register() {
      CHANNEL.registerMessage(
         0,
         OffloadNet.ToClient.class,
         (m, buf) -> buf.writeByteArray(m.data()),
         buf -> new OffloadNet.ToClient(buf.readByteArray()),
         OffloadNet::onClient,
         Optional.of(NetworkDirection.PLAY_TO_CLIENT)
      );
      CHANNEL.registerMessage(
         1,
         OffloadNet.ToServer.class,
         (m, buf) -> buf.writeByteArray(m.data()),
         buf -> new OffloadNet.ToServer(buf.readByteArray()),
         OffloadNet::onServer,
         Optional.of(NetworkDirection.PLAY_TO_SERVER)
      );
   }

   private static void onClient(OffloadNet.ToClient m, Supplier<Context> from) {
      Context ctx = from.get();
      ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> OffloadClient.onReceive(m.data())));
      ctx.setPacketHandled(true);
   }

   private static void onServer(OffloadNet.ToServer m, Supplier<Context> from) {
      Context ctx = from.get();
      ctx.enqueueWork(() -> {
         ServerPlayer player = ctx.getSender();
         if (player != null) {
            PlateOffload.onServerReceive(player, m.data());
         }
      });
      ctx.setPacketHandled(true);
   }

   public static boolean toClient(ServerPlayer player, byte[] frame) {
      if (!CHANNEL.isRemotePresent(player.connection.connection)) {
         return false;
      }

      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OffloadNet.ToClient(frame));
      return true;
   }

   public static void toServer(byte[] frame) {
      CHANNEL.sendToServer(new OffloadNet.ToServer(frame));
   }

   public record ToClient(byte[] data) {
   }

   public record ToServer(byte[] data) {
   }
}
