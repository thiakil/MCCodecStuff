package com.thiakil.codecs.blockstate;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.*;
import java.util.function.Predicate;

public class BlockstatePredicate implements Predicate<BlockState> {
    private final Block block;
    private final Map<String, Property.Value<?>> propertyMap;

    public static Codec<BlockstatePredicate> CODEC = BuiltInRegistries.BLOCK.byNameCodec()
            .dispatch("block",//dispatch based on the value of "block" (resource location)
                    pred -> pred.block,//get if from the block field
                    blockType->blockPropertiesOf(blockType)//generate a property map codec from the block
                            .xmap(propMap->new BlockstatePredicate(blockType, propMap), pred->pred.propertyMap)//turn the map and the block type into a predicate
                            .optionalFieldOf("properties") //wrap it in a MapCodec, so we can set the value's key
                            .xmap(//decide if it's empty or not
                                    op->op.orElseGet(()->new BlockstatePredicate(blockType, Collections.emptyMap())),//unwrap optional, constructing a bare predicate when empty
                                    pred-> pred.propertyMap.isEmpty() ? Optional.empty() : Optional.of(pred) //wrap in an optional, handling empty props
                            ).codec()//package it back up in a regular codec
            );

    BlockstatePredicate(Block block, Map<String, Property.Value<?>> propertyMap) {
        this.block = block;
        this.propertyMap = propertyMap;
    }

    public BlockstatePredicate(BlockState stateTemplate, Property<?>... propertiesToInclude) {
        this(stateTemplate.getBlock(), new HashMap<>());
        for (Property<?> property : propertiesToInclude) {
            this.propertyMap.put(property.getName(), property.value(stateTemplate));
        }
    }

    public Block getBlock() {
        return block;
    }

    public Map<String, Property.Value<?>> getPropertyMap() {
        return propertyMap;
    }

    @Override
    public boolean test(BlockState blockState) {
        if (blockState.getBlock() != this.block) {
            return false;
        }
        for (Map.Entry<String, Property.Value<?>> propEntry : this.propertyMap.entrySet()) {
            Property.Value<?> value = propEntry.getValue();
            if (!Objects.equals(value.value(), value.property().value(blockState).value())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})//properties are a terrible place. Mostly copied from net.minecraft.world.level.block.state.StateDefinition.appendPropertyCodec
    static Codec<Map<String, Property.Value<?>>> blockPropertiesOf(Block block) {
        MapCodec<Map<String, Property.Value<?>>> mapcodec = MapCodec.of(Encoder.empty(), Decoder.unit(HashMap::new));
        BlockState baseState = block.defaultBlockState();
        for (Property<?> property : baseState.getProperties()) {
            mapcodec = Codec.mapPair(mapcodec, property.valueCodec().optionalFieldOf(property.getName()))
                    .xmap(
                            thePair -> {
                                Optional<? extends Property.Value<?>> optionalProperty = thePair.getSecond();
                                if (optionalProperty.isPresent()) {
                                    Property.Value<?> value = optionalProperty.get();
                                    thePair.getFirst().put(value.property().getName(), value);
                                }
                                return thePair.getFirst();
                            }, theMap -> Pair.of(theMap, (Optional)Optional.ofNullable(theMap.get(property.getName())))
                    );
        }
        return mapcodec.codec();
    }

}
