package com.grassoverhead;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

@OnlyIn(Dist.CLIENT)
public class ClientEventHandler {
    private BlockState configuredBlockState = Blocks.GRASS_BLOCK.defaultBlockState();
    private double heightOffset = 1.5;
    private boolean configLoaded = false;

    // 平滑渲染变量
    private Vec3 lastRenderPosition = Vec3.ZERO;
    private long lastRenderTime = System.currentTimeMillis();
    private static final double SMOOTHING_FACTOR = 0.15; // 平滑系数 (0.0-1.0，值越大越平滑)

    public void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.CLIENT &&
                event.getConfig().getModId().equals(GrassOverHeadMod.MOD_ID)) {
            reloadConfig();
            configLoaded = true;
            // 重置平滑渲染状态
            lastRenderPosition = Vec3.ZERO;
            lastRenderTime = System.currentTimeMillis();
        }
    }

    private void reloadConfig() {
        String blockId = ClientConfig.BLOCK_TYPE.get();
        heightOffset = ClientConfig.HEIGHT_OFFSET.get();

        ResourceLocation location = ResourceLocation.tryParse(blockId);
        Block block = null;

        if (location != null) {
            block = BuiltInRegistries.BLOCK.get(location);
        }

        if (block == null || block == Blocks.AIR) {
            block = Blocks.GRASS_BLOCK;
        }

        configuredBlockState = block.defaultBlockState();
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (!configLoaded) {
            reloadConfig();
            configLoaded = true;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();

        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        TextureManager textureManager = mc.getTextureManager();

        textureManager.bindForSetup(InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);

        for (Player player : mc.level.players()) {
            renderBlockAbovePlayer(
                    player,
                    poseStack,
                    bufferSource,
                    blockRenderer,
                    configuredBlockState,
                    heightOffset,
                    partialTick
            );
        }

        bufferSource.endBatch();
    }

    private void renderBlockAbovePlayer(Player player, PoseStack poseStack,
                                        MultiBufferSource bufferSource,
                                        BlockRenderDispatcher blockRenderer,
                                        BlockState blockState,
                                        double heightOffset,
                                        float partialTick) {
        Minecraft mc = Minecraft.getInstance();

        // 计算当前帧的目标位置 - 精确位于玩家头顶正中央
        Vec3 targetPosition = new Vec3(
                player.getX(partialTick),
                player.getY(partialTick) + player.getBbHeight() + heightOffset,
                player.getZ(partialTick)
        );

        // 平滑渲染处理
        Vec3 renderPosition;
        if (lastRenderPosition.equals(Vec3.ZERO)) {
            // 第一次渲染，直接使用目标位置
            renderPosition = targetPosition;
        } else {
            // 计算时间差（毫秒）
            long currentTime = System.currentTimeMillis();
            double deltaTime = (currentTime - lastRenderTime) / 1000.0;
            lastRenderTime = currentTime;

            // 应用平滑插值
            double lerpFactor = Math.min(1.0, SMOOTHING_FACTOR * (60.0 * deltaTime));
            renderPosition = lastRenderPosition.lerp(targetPosition, lerpFactor);
        }

        // 保存当前渲染位置供下一帧使用
        lastRenderPosition = renderPosition;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);

        poseStack.pushPose();

        Vec3 cameraPos = mc.getEntityRenderDispatcher().camera.getPosition();

        // 添加0.5偏移使方块中心对齐玩家位置
        double centerOffset = 0.625;
        poseStack.translate(
                renderPosition.x - cameraPos.x - centerOffset,
                renderPosition.y - cameraPos.y,
                renderPosition.z - cameraPos.z - centerOffset
        );

        // 渲染方块
        renderBlockProperly(poseStack, bufferSource, blockRenderer, blockState);

        poseStack.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderBlockProperly(PoseStack poseStack, MultiBufferSource bufferSource,
                                     BlockRenderDispatcher blockRenderer,
                                     BlockState state) {
        int packedLight = LightTexture.FULL_BRIGHT;
        blockRenderer.renderSingleBlock(
                state,
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                RenderType.translucent()
        );
    }
}