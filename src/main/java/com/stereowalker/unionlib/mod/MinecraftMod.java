package com.stereowalker.unionlib.mod;

import com.stereowalker.unionlib.api.collectors.ConfigCollector;
import com.stereowalker.unionlib.api.collectors.InsertCollector;
import com.stereowalker.unionlib.api.collectors.PacketCollector;
import com.stereowalker.unionlib.api.collectors.ReloadListeners;
import com.stereowalker.unionlib.api.creativetabs.CreativeTabPopulator;
import com.stereowalker.unionlib.api.keymaps.KeyMappingCollector;
import com.stereowalker.unionlib.api.registries.RegistryCollector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class MinecraftMod {
    private static final String PROTOCOL = "1";

    protected final String modId;
    public final SimpleChannel channel;
    protected final ClientSegment clientSegment;
    protected final ServerSegment serverSegment;

    private final InsertCollector insertCollector = new InsertCollector();
    private final ReloadListeners clientReloadListeners = new ReloadListeners();
    private final ReloadListeners serverReloadListeners = new ReloadListeners();
    private final KeyMappingCollector keyMappingCollector = new KeyMappingCollector();

    public MinecraftMod(String modId, Supplier<ClientSegment> clientFactory, Supplier<ServerSegment> serverFactory) {
        this.modId = modId;
        this.channel = NetworkRegistry.newSimpleChannel(
                ResourceLocation.tryBuild(modId, "main"),
                () -> PROTOCOL,
                PROTOCOL::equals,
                PROTOCOL::equals
        );
        this.clientSegment = FMLEnvironment.dist.isClient() ? clientFactory.get() : null;
        this.serverSegment = serverFactory.get();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        setupConfigs(new ConfigCollector(modId));
        RegistryCollector registryCollector = new RegistryCollector(modBus, modId);
        setupRegistries(registryCollector);
        registryCollector.registerAll();
        registerPackets(new PacketCollector(channel));
        registerInserts(insertCollector);
        registerServerRelaodableResources(serverReloadListeners);

        if (clientSegment != null) {
            registerClientRelaodableResources(clientReloadListeners);
            clientSegment.setupKeymappings(keyMappingCollector);
            clientSegment.registerInserts(insertCollector);
            modBus.addListener(this::onRegisterKeyMappings);
            modBus.addListener(this::onRegisterClientReloadListeners);
        }

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onBuildCreativeTab);

        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelLoad);
        MinecraftForge.EVENT_BUS.addListener(this::onServerReloadListeners);
        if (clientSegment != null) {
            MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
        }
    }

    protected void onModStartup() {
    }

    public void registerInserts(InsertCollector collector) {
    }

    public void registerClientRelaodableResources(ReloadListeners reloadListener) {
    }

    public void registerServerRelaodableResources(ReloadListeners reloadListener) {
    }

    public void populateCreativeTabs(CreativeTabPopulator populator) {
    }

    public void setupConfigs(ConfigCollector collector) {
    }

    public void setupRegistries(RegistryCollector collector) {
        collector.registerAll();
    }

    public void registerPackets(PacketCollector collector) {
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(this::onModStartup);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        populateCreativeTabs(new CreativeTabPopulator(event));
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        keyMappingCollector.registerAll(event);
    }

    private void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        for (var listener : clientReloadListeners.listeners()) {
            event.registerReloadListener(listener);
        }
    }

    private void onServerReloadListeners(AddReloadListenerEvent event) {
        for (var listener : serverReloadListeners.listeners()) {
            event.addListener(listener);
        }
    }

    private void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        for (var handler : insertCollector.levelLoadHandlers()) {
            handler.accept(level);
        }
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        for (var handler : insertCollector.loginHandlers()) {
            handler.accept(serverPlayer);
        }
    }

    private void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (var handler : insertCollector.livingTickHandlers()) {
            handler.accept(event.player);
        }
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (Runnable handler : insertCollector.clientTickFinishHandlers()) {
            handler.run();
        }
    }
}
