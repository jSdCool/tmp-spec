package com.cbi.tmpspec;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import static net.minecraft.server.command.CommandManager.literal;

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
					ServerPlayerEntity player = context.getSource().getPlayer();
					if(player==null){
							context.getSource().sendError(Text.literal("this command can oly be run by a player"));
						return 0;
					}
					ServerPlayerInteractionManager interactionManager=player.interactionManager;

					GameMode gamemode = interactionManager.getGameMode();
					if(gamemode.equals(GameMode.SURVIVAL)) {
						if (player.isOnGround()) {
							Vec3d pos =player.getPos();
							playerPositions.put(player.getUuidAsString(),new PositionInfo(new Vec3d(pos.x,pos.y,pos.z),player.getWorld().getRegistryKey()));
							player.changeGameMode(GameMode.SPECTATOR);
							savePositions();
							LOGGER.info(pos.toString());
							context.getSource().sendFeedback(()->Text.literal("changed gamemode to spectator"),true);
						} else {
							context.getSource().sendError(Text.literal("you are not on the ground"));
							return 0;
						}
					}else if(gamemode.equals(GameMode.SPECTATOR)){
						PositionInfo position = playerPositions.get(player.getUuidAsString());
						if(position==null){
							context.getSource().sendError(Text.literal("no previous position found"));
							return 0;
						}

						player.teleport(context.getSource().getServer().getWorld(position.dimension),position.pos.x,position.pos.y,position.pos.z,player.getYaw(),player.getPitch());
						LOGGER.info(position.pos.toString());
						player.changeGameMode(GameMode.SURVIVAL);

						context.getSource().sendFeedback(()-> Text.literal("changed gamemode to survival"),true);
					}else{
						context.getSource().sendError(Text.literal("your gamemode is not supported"));
					}

					return 1;
				})));

		LOGGER.info("Hello Fabric world!");
		ServerLifecycleEvents.SERVER_STARTED.register(TmpSpec::loadPositions);
	}

	@SuppressWarnings("all")
	public static void savePositions(){
		String[] keys = playerPositions.keySet().toArray(new String[]{});
		new File("config").mkdirs();
		try {
			FileWriter output=new FileWriter("config/TmpSpecPlayerPositions.pos");
			for (String key : keys) {
				Vec3d pos = playerPositions.get(key).pos;
				RegistryKey<World> dim = playerPositions.get(key).dimension;
				output.write(key + " " + pos.x + " " + pos.y + " " + pos.z + " " + dim.getValue().toString() + "\n");
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
				Identifier dimRaw=new Identifier(input.next());
				RegistryKey<World> dimension = null;
				for(RegistryKey<World> world: server.getWorldRegistryKeys()){
					if(world.getValue().equals(dimRaw)){
						dimension=world;
					}
				}
				if(dimension==null){
					LOGGER.error("error while loading player positions: unknown dimension "+dimRaw.toString());
					continue;
				}
				PositionInfo playerPos=new PositionInfo(new Vec3d(x,y,z),dimension);
				playerPositions.put(uuid,playerPos);

			}
		} catch (FileNotFoundException e) {
			LOGGER.info("no previous positions exist");
		}
	}
}