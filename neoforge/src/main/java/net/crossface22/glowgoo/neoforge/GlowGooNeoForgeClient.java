package net.crossface22.glowgoo.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.crossface22.glowgoo.GlowGoo;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid = GlowGoo.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GlowGooNeoForgeClient {
    private static final Set<BlockPos> HIGHLIGHTED_BLOCKS = new HashSet<>();
    private static BlockPos lastPlayerPos;
    private static Item lastMainHandItem;
    private static Item lastOffHandItem;

    private GlowGooNeoForgeClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GlowGooNeoForge.GLOW_GOO_PROJECTILE.get(), ThrownItemRenderer::new);
    }

    @EventBusSubscriber(modid = GlowGoo.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
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
            boolean holdingGlowGoo = mainHandItem == GlowGooNeoForge.GLOW_GOO_ITEM.get() || offHandItem == GlowGooNeoForge.GLOW_GOO_ITEM.get();
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
                        if (minecraft.level.getBlockState(pos).is(GlowGooNeoForge.GLOW_GOO_BLOCK.get())) {
                            HIGHLIGHTED_BLOCKS.add(pos.immutable());
                        }
                    }
                }
            }

            lastMainHandItem = mainHandItem;
            lastOffHandItem = offHandItem;
            lastPlayerPos = currentPlayerPos;
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null || HIGHLIGHTED_BLOCKS.isEmpty()) {
                return;
            }

            PoseStack poseStack = event.getPoseStack();
            var cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();
            var bufferSource = minecraft.renderBuffers().bufferSource();
            var buffer = bufferSource.getBuffer(RenderType.lines());

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
                        pos.getX() - cameraPos.x(),
                        pos.getY() - cameraPos.y(),
                        pos.getZ() - cameraPos.z(),
                        1.0F,
                        1.0F,
                        0.0F,
                        1.0F,
                        false
                );
            }

            bufferSource.endBatch(RenderType.lines());
        }
    }
}
