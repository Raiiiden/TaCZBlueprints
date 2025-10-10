package com.raiiiden.taczblueprints.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.raiiiden.taczblueprints.capability.GunUnlocksProvider;
import com.raiiiden.taczblueprints.capability.IGunUnlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;

public class BlueprintCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("blueprints")
                // List unlocked guns
                .then(Commands.literal("list")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            LazyOptional<IGunUnlocks> unlocksOpt = GunUnlocksProvider.get(player);

                            if (unlocksOpt.isPresent()) {
                                IGunUnlocks unlocks = unlocksOpt.orElseThrow(() ->
                                        new IllegalStateException("Player does not have GunUnlocks capability!")
                                );

                                if (unlocks.getUnlockedGuns().isEmpty()) {
                                    player.sendSystemMessage(Component.literal("§eYou have no unlocked blueprints."));
                                } else {
                                    player.sendSystemMessage(Component.literal("§aUnlocked blueprints:"));
                                    for (String gunId : unlocks.getUnlockedGuns()) {
                                        player.sendSystemMessage(Component.literal(" - " + gunId));
                                    }
                                }
                            } else {
                                player.sendSystemMessage(Component.literal("§cCould not access your blueprints capability."));
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                // Clear unlocked guns
                .then(Commands.literal("clear")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            LazyOptional<IGunUnlocks> unlocksOpt = GunUnlocksProvider.get(player);

                            if (unlocksOpt.isPresent()) {
                                IGunUnlocks unlocks = unlocksOpt.orElseThrow(() ->
                                        new IllegalStateException("Player does not have GunUnlocks capability!")
                                );

                                unlocks.clearAll(); // clear server-side
                                // Sync to client
                                com.raiiiden.taczblueprints.network.ModNetworking.sendToPlayer(
                                        player,
                                        new com.raiiiden.taczblueprints.network.SyncUnlockedGunsPacket(unlocks.getUnlockedGuns())
                                );

                                player.sendSystemMessage(Component.literal("§cAll unlocked blueprints cleared."));
                            } else {
                                player.sendSystemMessage(Component.literal("§cCould not access your blueprints capability."));
                            }

                            return Command.SINGLE_SUCCESS;
                        }));
    }
}
