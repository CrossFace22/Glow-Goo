package net.crossface22.glowgoo.neoforge;

import net.crossface22.glowgoo.GlowGoo;
import net.crossface22.glowgoo.GlowGooBlock;
import net.crossface22.glowgoo.GlowGooItem;
import net.crossface22.glowgoo.GlowGooProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GlowGoo.MOD_ID)
public class GlowGooNeoForge {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, GlowGoo.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, GlowGoo.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, GlowGoo.MOD_ID);

    public static final DeferredHolder<Block, GlowGooBlock> GLOW_GOO_BLOCK = BLOCKS.register(
            "glow_goo",
            () -> new GlowGooBlock(BlockBehaviour.Properties.of()
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

    public static final DeferredHolder<Item, GlowGooItem> GLOW_GOO_ITEM = ITEMS.register(
            "glow_goo",
            () -> new GlowGooItem(new Item.Properties())
    );

    public static final DeferredHolder<EntityType<?>, EntityType<GlowGooProjectile>> GLOW_GOO_PROJECTILE = ENTITY_TYPES.register(
            "glow_goo",
            () -> EntityType.Builder.<GlowGooProjectile>of(GlowGooProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(GlowGoo.id("glow_goo").toString())
    );

    public GlowGooNeoForge(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::buildCreativeTab);

        GlowGoo.bootstrap(GLOW_GOO_BLOCK::get, GLOW_GOO_ITEM::get, GLOW_GOO_PROJECTILE::get);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(GlowGoo::registerCommonBehaviors);
    }

    private void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GLOW_GOO_ITEM.get());
        }
    }
}
