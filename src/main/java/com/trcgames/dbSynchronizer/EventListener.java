package com.trcgames.dbSynchronizer;

import com.trcgames.dbSynchronizer.database.ClientDatabase;
import com.trcgames.dbSynchronizer.database.Database;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient.StCPacketType;

import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class EventListener{
	
	/** Triggered on <u>Server thread</u>. */
	@SubscribeEvent
	public void onClientJoin (PlayerLoggedInEvent event){
		DBSynchronizer.network.sendToAll (new PacketServerToClient (StCPacketType.PLAYER_LOGGED_IN, event.player.getName()));
	}

	/** Triggered on thread <u>Netty Client IO</u>. */
	@SubscribeEvent (priority = EventPriority.LOW)
	public void onClientLeave (ClientDisconnectionFromServerEvent event){
		
		DBSynchronizer.worldLoaded = false;
		ClientDatabase.onClientLeave ();
	}
	
	// TODO remove below :
	
	@SubscribeEvent
	public void onClientChat (ClientChatReceivedEvent event){
		
		//event.setMessage (new TextComponentString ("" + DatabaseGetter.getInstance ("test").getPersistentFolder().canModifyDataInside()));
		
		//System.out.println ("SET : " + DatabaseGetter.getInstance ("test").getPersistentFolder().setInt ("test int", 10));
		//System.out.println ("TEST CLIENT: " + DatabaseGetter.getInstance ("test").getPersistentFolder().getInt ("test int"));
		/*
		Database db = DatabaseGetter.getInstance ("test");
		System.out.println ("test");*/
	}
	
	@SubscribeEvent
	public void onServerChat (ServerChatEvent event){
		/*
		Database db = DatabaseGetter.getInstance ("test");
		db.getPersistentFolder().getDBFolder("subf").setInt ("test int", 42);
		System.out.println ("test");*/
		
		/*
		String [] hierarchy = DatabaseGetter.getInstance("test").getPersistentFolder().getDBFolder("subf").getHierarchy();
		String hierarchyStr = hierarchy[0] +":"+ hierarchy[1];
		System.out.println(hierarchyStr);*/
		
		//System.out.println ("SET : " + DatabaseGetter.getInstance ("test").getPersistentFolder().getDBFolder("subf").setInt ("test int", 42));
		/*
		try {
			DatabaseGetter.getInstance("test").getPersistentFolder().allowAccessToAll();
		}catch (IllegalAccessException e){
			e.printStackTrace();
		}*/
		
		//System.out.println ("TEST SERVER : " + DatabaseGetter.getInstance ("test").getPersistentFolder().getDBFolder("subf").getInt ("test int"));
	}
}