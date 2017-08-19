package com.trcgames.dbSynchronizer;

import com.trcgames.dbSynchronizer.database.ClientDatabase;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient.StCPacketType;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class EventListener{
	
	/** Triggered on <u>Server thread</u>. */
	@SubscribeEvent (priority = EventPriority.HIGHEST)
	public void onClientJoin (PlayerLoggedInEvent event){
		DBSynchronizer.network.sendToAll (new PacketServerToClient (StCPacketType.PLAYER_LOGGED_IN, event.player.getDisplayName()));
	}

	/** Triggered on thread <u>Netty Client IO</u>. */
	@SubscribeEvent (priority = EventPriority.LOW)
	public void onClientLeave (ClientDisconnectionFromServerEvent event){
		ClientDatabase.onClientLeave ();
	}
}