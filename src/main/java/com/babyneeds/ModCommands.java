package com.babyneeds;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Operator tooling: read an animal's state, force a stage while testing balance, top a herd back
 * up, and switch the mod off cleanly before removing the jar.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    private static final double DEFAULT_RADIUS = 8.0;

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        var stage = Commands.literal("stage");
        for (GrowthStage value : GrowthStage.values()) {
            stage = stage.then(Commands.literal(value.key()).executes(ctx -> forEachNearby(ctx.getSource(), animal -> {
                AnimalState.setStage(animal, value);
                AnimalState.applyStageToAge(animal, value);
                ModMessages.syncToTracking(animal);
            }, "babyneeds.cmd.stage_set")));
        }

        event.getDispatcher().register(
                Commands.literal("babyneeds")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                        .then(stage)
                        .then(Commands.literal("feed").executes(ctx -> forEachNearby(ctx.getSource(), animal -> {
                            AnimalState.setNutrition(animal, HungerCalculator.MAX_NUTRITION);
                            AnimalState.setStarvingTicks(animal, 0);
                            if (AnimalState.isLactating(animal)) {
                                AnimalState.setMilk(animal,
                                        LactationCalculator.capacity(AnimalState.dependents(animal)));
                            }
                        }, "babyneeds.cmd.fed")))
                        // Test tooling: starvation otherwise takes the better part of an hour to
                        // reach, which makes the whole failure path impractical to check by hand.
                        .then(Commands.literal("drain").executes(ctx -> forEachNearby(ctx.getSource(), animal -> {
                            AnimalState.setNutrition(animal, 0.0);
                            AnimalState.setMilk(animal, 0.0);
                            AnimalState.setStarvingTicks(animal, Config.STARVATION_GRACE_TICKS.get());
                        }, "babyneeds.cmd.drained")))
                        .then(Commands.literal("uninstall")
                                .executes(ctx -> setUninstall(ctx.getSource(), true))
                                .then(Commands.literal("cancel")
                                        .executes(ctx -> setUninstall(ctx.getSource(), false)))));
    }

    private static int info(CommandSourceStack source) {
        List<Animal> animals = nearby(source);
        if (animals.isEmpty()) {
            source.sendFailure(Component.translatable("babyneeds.cmd.none_nearby"));
            return 0;
        }
        for (Animal animal : animals) {
            GrowthStage stage = AnimalState.stage(animal);
            source.sendSuccess(() -> Component.translatable("babyneeds.status.line",
                            animal.getType().getDescription(),
                            Component.translatable("babyneeds.stage." + stage.key()),
                            (int) Math.round(AnimalState.nutrition(animal)),
                            (int) Math.round(AnimalState.milk(animal)))
                    .withStyle(ChatFormatting.AQUA), false);
        }
        return animals.size();
    }

    private static int forEachNearby(CommandSourceStack source, Consumer<Animal> action, String messageKey) {
        List<Animal> animals = nearby(source);
        animals.forEach(action);
        int count = animals.size();
        source.sendSuccess(() -> Component.translatable(messageKey, count).withStyle(ChatFormatting.GREEN), true);
        return count;
    }

    private static int setUninstall(CommandSourceStack source, boolean enabled) {
        Config.UNINSTALL_MODE.set(enabled);
        int cleaned = 0;
        if (enabled) {
            for (Animal animal : nearby(source)) {
                AnimalEvents.stripModData(animal);
                cleaned++;
            }
        }
        int total = cleaned;
        source.sendSuccess(() -> Component.translatable(
                        enabled ? "babyneeds.cmd.uninstall_on" : "babyneeds.cmd.uninstall_off", total)
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        return total;
    }

    private static List<Animal> nearby(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        AABB box = AABB.ofSize(source.getPosition(), DEFAULT_RADIUS * 2, DEFAULT_RADIUS * 2, DEFAULT_RADIUS * 2);
        return level.getEntitiesOfClass(Animal.class, box, AnimalState::isSupported);
    }
}
