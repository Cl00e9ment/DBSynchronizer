package com.trcgames.dbSynchronizer;

import com.trcgames.dbSynchronizer.database.DataSaver;
import com.trcgames.dbSynchronizer.database.ServerDatabase;
import com.trcgames.dbSynchronizer.packets.PacketClientToServer;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient.StCPacketType;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

@Mod (modid = DBSynchronizer.MOD_ID, version = DBSynchronizer.VERSION)
public class DBSynchronizer{
	
	@Instance (DBSynchronizer.MOD_ID)
	public static DBSynchronizer instance;
	
	public static final String MOD_ID = "dbsynchronizer";
	public static final String VERSION = "1.0.0";
	public static SimpleNetworkWrapper network;
	public static boolean worldLoaded;
	
	@EventHandler
	public void preInit (FMLPreInitializationEvent event){
		
		FMLCommonHandler.instance().bus().register (new EventListener ());
		
		network = NetworkRegistry.INSTANCE.newSimpleChannel (DBSynchronizer.MOD_ID);
		network.registerMessage (PacketServerToClient.Handler.class, PacketServerToClient.class, 0, Side.CLIENT);
		network.registerMessage (PacketClientToServer.Handler.class, PacketClientToServer.class, 1, Side.SERVER);
	}
	
	@EventHandler
	public void onServerStarting (FMLServerStartingEvent event){
		
		worldLoaded = true;
		network.sendToAll (new PacketServerToClient (StCPacketType.UPDATE_WORLD_LOADING_STATE, "start"));
		ServerDatabase.onServerStarting();
	}
	
	@EventHandler
	public void onServerStopping (FMLServerStoppingEvent event){
		
		worldLoaded = false;
		network.sendToAll (new PacketServerToClient (StCPacketType.UPDATE_WORLD_LOADING_STATE, "stop"));
		ServerDatabase.onServerStopping();
		DataSaver.killInstance ();
	}
}