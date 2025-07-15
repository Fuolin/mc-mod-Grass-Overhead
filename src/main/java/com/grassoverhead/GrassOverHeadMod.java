package com.grassoverhead;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(GrassOverHeadMod.MOD_ID)
public class GrassOverHeadMod {
    public static final String MOD_ID = "grassoverhead";

    public GrassOverHeadMod(IEventBus modEventBus) {
        // 获取当前mod的ModContainer
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();

        // 注册客户端配置
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC, MOD_ID + "-client.toml");

        // 仅在客户端注册事件
        if (FMLEnvironment.dist.isClient()) {
            // 创建事件处理器实例
            ClientEventHandler clientHandler = new ClientEventHandler();

            // 将配置变更监听器注册到mod事件总线
            modEventBus.addListener(clientHandler::onConfigChanged);

            // 将渲染监听器注册到NeoForge事件总线
            NeoForge.EVENT_BUS.register(clientHandler);
        }
    }
}