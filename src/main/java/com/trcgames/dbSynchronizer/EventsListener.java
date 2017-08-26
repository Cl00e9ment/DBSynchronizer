package com.trcgames.dbSynchronizer;

import com.trcgames.dbSynchronizer.database.ClientDatabase;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient.StCPacketType;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class EventsListener{
	
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
	public void onClientChat (ClientChatReceivedEvent event){}
	
	@SubscribeEvent
	public void onServerChat (ServerChatEvent event){}
}