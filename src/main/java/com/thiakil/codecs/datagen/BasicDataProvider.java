package com.thiakil.codecs.datagen;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import com.thiakil.codecs.CodecMod;
import com.thiakil.codecs.blockstate.BlockstatePredicate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BasicDataProvider implements DataProvider {
    private final PackOutput packOutput;
    private final PackOutput.PathProvider predicatesPath;

    public BasicDataProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
        predicatesPath = packOutput.createPathProvider(PackOutput.Target.DATA_PACK, "predicates");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        return CompletableFuture.allOf(
                saveBlockstate(pOutput, new BlockstatePredicate(Blocks.CHEST.defaultBlockState(), ChestBlock.TYPE), "chest_type"),
                saveBlockstate(pOutput, new BlockstatePredicate(Blocks.CHEST.defaultBlockState()), "just_chest")
        );
    }

    @NotNull
    private CompletableFuture<?> saveBlockstate(CachedOutput pOutput, BlockstatePredicate pred, String name) {
        JsonElement jsonelement = Util.getOrThrow(BlockstatePredicate.CODEC.encodeStart(JsonOps.INSTANCE, pred), IllegalStateException::new);
        BlockstatePredicate decoded = BlockstatePredicate.CODEC.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(false, s -> {
        });
        if (decoded.getBlock() != pred.getBlock() || !Objects.equals(pred.getPropertyMap(), decoded.getPropertyMap())) {
            throw new IllegalStateException("decode failed");
        }
        return DataProvider.saveStable(pOutput,  jsonelement, predicatesPath.json(new ResourceLocation(CodecMod.MODID, name)));
    }

    @Override
    public String getName() {
        return CodecMod.MODID+":generic";
    }
}
