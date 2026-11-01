package com.skyfall.airstrike.registry;

import com.skyfall.airstrike.SkyfallMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SkyfallMod.MOD_ID);

    public static final RegistryObject<SoundEvent> DRONE_ENGINE = registerSound("drone_engine");
    public static final RegistryObject<SoundEvent> DRONE_APPROACH = registerSound("drone_approach");
    public static final RegistryObject<SoundEvent> TARGET_LOCK = registerSound("target_lock");
    public static final RegistryObject<SoundEvent> AIRSTRIKE_WARNING = registerSound("airstrike_warning");
    public static final RegistryObject<SoundEvent> DRONE_WHISTLE = registerSound("drone_whistle");
    public static final RegistryObject<SoundEvent> EXPLOSION_BLAST = registerSound("explosion_blast");
    public static final RegistryObject<SoundEvent> EXPLOSION_BASS = registerSound("explosion_bass");
    public static final RegistryObject<SoundEvent> EXPLOSION_RUMBLE = registerSound("explosion_rumble");
    public static final RegistryObject<SoundEvent> DEBRIS_FALL = registerSound("debris_fall");
    public static final RegistryObject<SoundEvent> FIRE_AMBIENCE = registerSound("fire_ambience");
    public static final RegistryObject<SoundEvent> EMP_BLAST = registerSound("emp_blast");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation(SkyfallMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
