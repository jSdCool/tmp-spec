package com.cbi.tmpspec;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Scanner;

import static net.minecraft.commands.Commands.literal;

public class TmpSpec implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("tmp-spec");

	public static HashMap<String,PositionInfo> playerPositions =new HashMap<>();
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("specSwap")
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayer();
					if(player==null){
							context.getSource().sendFailure(Component.literal("this command can oly be run by a player"));
						return 0;
					}
					ServerPlayerGameMode interactionManager=player.gameMode;

					GameType gamemode = interactionManager.getGameModeForPlayer();
					if(gamemode.equals(GameType.SURVIVAL)) {
						if (player.onGround()) {
							Vec3 pos =player.position();
							playerPositions.put(player.getStringUUID(),new PositionInfo(new Vec3(pos.x,pos.y,pos.z),player.level().dimension()));
							player.setGameMode(GameType.SPECTATOR);
							savePositions();
							//opLOGGER.info(pos.toString());
							context.getSource().sendSuccess(()->Component.literal("changed gamemode to spectator"),true);
						} else {
							context.getSource().sendFailure(Component.literal("you are not on the ground"));
							return 0;
						}
					}else if(gamemode.equals(GameType.SPECTATOR)){
						PositionInfo position = playerPositions.get(player.getStringUUID());
						if(position==null){
							context.getSource().sendFailure(Component.literal("no previous position found"));
							return 0;
						}

						player.teleportTo(context.getSource().getServer().getLevel(position.dimension),position.pos.x,position.pos.y,position.pos.z, EnumSet.noneOf(Relative.class), player.getYRot(),player.getXRot(),false);
						LOGGER.info(position.pos.toString());
						player.setGameMode(GameType.SURVIVAL);

						context.getSource().sendSuccess(()-> Component.literal("changed gamemode to survival"),true);
					}else{
						context.getSource().sendFailure(Component.literal("your gamemode is not supported"));
					}

					return 1;
				})));

		//LOGGER.info("Hello Fabric world!");
		ServerLifecycleEvents.SERVER_STARTED.register(TmpSpec::loadPositions);
	}

	@SuppressWarnings("all")
	public static void savePositions(){
		String[] keys = playerPositions.keySet().toArray(new String[]{});
		new File("config").mkdirs();
		try {
			FileWriter output=new FileWriter("config/TmpSpecPlayerPositions.pos");
			for (String key : keys) {
				Vec3 pos = playerPositions.get(key).pos;
				ResourceKey<Level> dim = playerPositions.get(key).dimension;
				output.write(key + " " + pos.x + " " + pos.y + " " + pos.z + " " + dim.location().toString() + "\n");
			}
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	public static void loadPositions(MinecraftServer server){
		LOGGER.info("loading previous player positions");
		try {
			Scanner input = new Scanner(new File("config/TmpSpecPlayerPositions.pos"));
			while(input.hasNext()){
				String uuid=input.next();
				double x = input.nextDouble(),y=input.nextDouble(),z=input.nextDouble();
				ResourceLocation dimRaw=ResourceLocation.parse(input.next());
				ResourceKey<Level> dimension = null;
				for(ResourceKey<Level> world: server.levelKeys()){
					if(world.location().equals(dimRaw)){
						dimension=world;
					}
				}
				if(dimension==null){
					LOGGER.error("error while loading player positions: unknown dimension "+dimRaw);
					continue;
				}
				PositionInfo playerPos=new PositionInfo(new Vec3(x,y,z),dimension);
				playerPositions.put(uuid,playerPos);

			}
		} catch (FileNotFoundException e) {
			LOGGER.info("no previous positions exist");
		}
	}
}