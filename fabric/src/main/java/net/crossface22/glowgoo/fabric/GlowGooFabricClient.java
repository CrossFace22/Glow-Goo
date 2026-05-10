package net.crossface22.glowgoo.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

public class GlowGooFabricClient implements ClientModInitializer {
    private static final Set<BlockPos> HIGHLIGHTED_BLOCKS = new HashSet<>();
    private static BlockPos lastPlayerPos;
    private static Item lastMainHandItem;
    private static Item lastOffHandItem;

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(GlowGooFabric.GLOW_GOO_BLOCK, RenderType.translucent());
        EntityRendererRegistry.register(GlowGooFabric.GLOW_GOO_PROJECTILE, ThrownItemRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tickHighlightScan());
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> renderHighlights(context.matrixStack(), context.camera().getPosition().x(), context.camera().getPosition().y(), context.camera().getPosition().z(), context.consumers()));
    }

    private static void tickHighlightScan() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            HIGHLIGHTED_BLOCKS.clear();
            return;
        }

        Player player = minecraft.player;
        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();
        Item mainHandItem = mainHandStack.getItem();
        Item offHandItem = offHandStack.getItem();
        boolean holdingGlowGoo = mainHandItem == GlowGooFabric.GLOW_GOO_ITEM || offHandItem == GlowGooFabric.GLOW_GOO_ITEM;
        BlockPos currentPlayerPos = player.blockPosition();

        if (!holdingGlowGoo) {
            HIGHLIGHTED_BLOCKS.clear();
            lastMainHandItem = mainHandItem;
            lastOffHandItem = offHandItem;
            lastPlayerPos = currentPlayerPos;
            return;
        }

        boolean needsRescan = lastMainHandItem != mainHandItem
                || lastOffHandItem != offHandItem
                || lastPlayerPos == null
                || !lastPlayerPos.equals(currentPlayerPos);
        if (!needsRescan) {
            return;
        }

        HIGHLIGHTED_BLOCKS.clear();
        int radius = 16;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = currentPlayerPos.offset(dx, dy, dz);
                    if (minecraft.level.getBlockState(pos).is(GlowGooFabric.GLOW_GOO_BLOCK)) {
                        HIGHLIGHTED_BLOCKS.add(pos.immutable());
                    }
                }
            }
        }

        lastMainHandItem = mainHandItem;
        lastOffHandItem = offHandItem;
        lastPlayerPos = currentPlayerPos;
    }

    private static void renderHighlights(PoseStack poseStack, double cameraX, double cameraY, double cameraZ, net.minecraft.client.renderer.MultiBufferSource consumers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || HIGHLIGHTED_BLOCKS.isEmpty() || poseStack == null || consumers == null) {
            return;
        }

        var buffer = consumers.getBuffer(RenderType.lines());
        for (BlockPos pos : HIGHLIGHTED_BLOCKS) {
            var state = minecraft.level.getBlockState(pos);
            VoxelShape shape = state.getShape(minecraft.level, pos, CollisionContext.empty());
            if (shape.isEmpty()) {
                continue;
            }

            LevelRenderer.renderVoxelShape(
                    poseStack,
                    buffer,
                    shape,
                    pos.getX() - cameraX,
                    pos.getY() - cameraY,
                    pos.getZ() - cameraZ,
                    1.0F,
                    1.0F,
                    0.0F,
                    1.0F,
                    false
            );
        }
    }
}
