package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {

    private static SimpleChannel INSTANCE;

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(CrushingWheelRecipeSelector.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Client -> Server packets
        net.messageBuilder(RequestOpenGuiPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RequestOpenGuiPacket::new)
                .encoder(RequestOpenGuiPacket::toBytes)
                .consumerMainThread(RequestOpenGuiPacket::handle)
                .add();

        net.messageBuilder(SelectRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SelectRecipePacket::new)
                .encoder(SelectRecipePacket::toBytes)
                .consumerMainThread(SelectRecipePacket::handle)
                .add();

        net.messageBuilder(ClearRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ClearRecipePacket::new)
                .encoder(ClearRecipePacket::toBytes)
                .consumerMainThread(ClearRecipePacket::handle)
                .add();

        // Server -> Client packets
        net.messageBuilder(SyncSelectionsPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncSelectionsPacket::new)
                .encoder(SyncSelectionsPacket::toBytes)
                .consumerMainThread(SyncSelectionsPacket::handle)
                .add();

        net.messageBuilder(StartLinkingPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(StartLinkingPacket::new)
                .encoder(StartLinkingPacket::toBytes)
                .consumerMainThread(StartLinkingPacket::handle)
                .add();

        net.messageBuilder(CancelLinkingPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CancelLinkingPacket::new)
                .encoder(CancelLinkingPacket::toBytes)
                .consumerMainThread(CancelLinkingPacket::handle)
                .add();

        net.messageBuilder(LinkResultPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(LinkResultPacket::new)
                .encoder(LinkResultPacket::toBytes)
                .consumerMainThread(LinkResultPacket::handle)
                .add();

        CrushingWheelRecipeSelector.LOGGER.info("Network packets registered");
    }

    public static void sendToServer(Object msg) {
        INSTANCE.sendToServer(msg);
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAllPlayers(Object msg) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }
}
