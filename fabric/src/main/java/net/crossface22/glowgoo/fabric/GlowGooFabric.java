package net.crossface22.glowgoo.fabric;

import net.crossface22.glowgoo.GlowGoo;
import net.crossface22.glowgoo.GlowGooBlock;
import net.crossface22.glowgoo.GlowGooItem;
import net.crossface22.glowgoo.GlowGooProjectile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class GlowGooFabric implements ModInitializer {
    public static final Block GLOW_GOO_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            GlowGoo.id("glow_goo"),
            new GlowGooBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GLOW_LICHEN)
                    .noCollission()
                    .noOcclusion()
                    .replaceable()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .instabreak()
                    .sound(SoundType.SLIME_BLOCK)
                    .lightLevel(state -> 15))
    );

    public static final Item GLOW_GOO_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            GlowGoo.id("glow_goo"),
            new GlowGooItem(new Item.Properties())
    );

    public static final EntityType<GlowGooProjectile> GLOW_GOO_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            GlowGoo.id("glow_goo"),
            EntityType.Builder.<GlowGooProjectile>of(GlowGooProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(GlowGoo.id("glow_goo").toString())
    );

    @Override
    public void onInitialize() {
        GlowGoo.bootstrap(() -> GLOW_GOO_BLOCK, () -> GLOW_GOO_ITEM, () -> GLOW_GOO_PROJECTILE);
        GlowGoo.registerCommonBehaviors();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(GLOW_GOO_ITEM));
    }
}
