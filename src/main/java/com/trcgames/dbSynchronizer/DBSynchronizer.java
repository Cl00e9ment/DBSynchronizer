package com.trcgames.dbSynchronizer;

import com.trcgames.dbSynchronizer.database.DataSaver;
import com.trcgames.dbSynchronizer.database.ServerDatabase;
import com.trcgames.dbSynchronizer.packets.PacketClientToServer;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod (modid = DBSynchronizer.MOD_ID, version = DBSynchronizer.VERSION)
public class DBSynchronizer{
	
	@Instance (DBSynchronizer.MOD_ID)
	public static DBSynchronizer instance;
	
	public static final String MOD_ID = "dbsynchronizer";
	public static final String VERSION = "1.5.0.1";
	public static SimpleNetworkWrapper network;
	public static boolean worldLoaded;
	
	@EventHandler
	public void preInit (FMLPreInitializationEvent event){
		
		MinecraftForge.EVENT_BUS.register (new EventListener ());
		
		network = NetworkRegistry.INSTANCE.newSimpleChannel (DBSynchronizer.MOD_ID);
		network.registerMessage (PacketServerToClient.Handler.class, PacketServerToClient.class, 0, Side.CLIENT);
		network.registerMessage (PacketClientToServer.Handler.class, PacketClientToServer.class, 1, Side.SERVER);
	}
	
	@EventHandler
	public void onServerStarting (FMLServerStartingEvent event){
		
		worldLoaded = true;
		ServerDatabase.onServerStarting();
	}
	
	@EventHandler
	public void onServerStopping (FMLServerStoppingEvent event){
		
		worldLoaded = false;
		ServerDatabase.onServerStopping();
		DataSaver.killInstance ();
	}
}