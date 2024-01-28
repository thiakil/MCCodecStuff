package com.thiakil.codecs.datagen;

import com.thiakil.codecs.CodecMod;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod.EventBusSubscriber(modid = CodecMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CodecDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        DataProvider provider = gen.addProvider(event.includeServer(), new BasicDataProvider(output));
    }
}
