package net.crossface22.glowgoo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.function.Supplier;

public final class GlowGoo {
    public static final String MOD_ID = "glowgoo";

    private static Supplier<Block> glowGooBlock = missingSupplier("glow_goo");
    private static Supplier<Item> glowGooItem = missingSupplier("glow_goo item");
    private static Supplier<EntityType<GlowGooProjectile>> glowGooEntityType = missingSupplier("glow_goo");
    private static boolean commonBehaviorsRegistered;

    private GlowGoo() {
    }

    public static void bootstrap(
            Supplier<Block> blockSupplier,
            Supplier<Item> itemSupplier,
            Supplier<EntityType<GlowGooProjectile>> entityTypeSupplier
    ) {
        glowGooBlock = Objects.requireNonNull(blockSupplier);
        glowGooItem = Objects.requireNonNull(itemSupplier);
        glowGooEntityType = Objects.requireNonNull(entityTypeSupplier);
    }

    public static void registerCommonBehaviors() {
        if (!commonBehaviorsRegistered) {
            GlowGooItem.registerDispenserBehavior(glowGooItem.get());
            commonBehaviorsRegistered = true;
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Block glowGooBlock() {
        return glowGooBlock.get();
    }

    public static Item glowGooItem() {
        return glowGooItem.get();
    }

    public static EntityType<GlowGooProjectile> glowGooEntityType() {
        return glowGooEntityType.get();
    }

    private static <T> Supplier<T> missingSupplier(String name) {
        return () -> {
            throw new IllegalStateException("Tried to access uninitialized " + name);
        };
    }
}
