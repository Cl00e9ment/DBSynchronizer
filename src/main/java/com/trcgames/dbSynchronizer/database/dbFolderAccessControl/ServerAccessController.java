package com.trcgames.dbSynchronizer.database.dbFolderAccessControl;

import java.util.ArrayList;
import java.util.List;

import com.trcgames.dbSynchronizer.DBSynchronizer;
import com.trcgames.dbSynchronizer.database.DBFolder;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient.StCPacketType;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class ServerAccessController extends AccessController{
	
	private enum AccessByDefault {ALLOW, FORBID}
	private AccessByDefault accessByDefault = AccessByDefault.FORBID;
	private ArrayList<String> accessExceptions = new ArrayList<String>();
	
	public ServerAccessController (DBFolder folder){
		super (folder);
	}
	
	@Override
	public boolean canAccessToData (){
		return true; // because the server can access to all data
	}
	
	/**
	 * Retrieves true if accessByDefault == ALLOW else retrieves false.
	 * If the player name is contained in the accessExceptions list, then the return boolean will be inverted.
	 */
	public boolean canPlayerAccessToData (String playerName){
		
		for (String accessException : accessExceptions){
			if (accessException.equals (playerName)) return accessByDefault == AccessByDefault.FORBID;
		}
		
		return accessByDefault == AccessByDefault.ALLOW;
	}
	
	private EntityPlayerMP getAPlayer (String name){
		return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername (name);
	}
	
	private List<EntityPlayerMP> getAllPlayers (){
		return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
	}
	
	private String [] getArgsForPacketSending (boolean allowAccess){
		
		String [] hierarchy = folder.getHierarchy();
		
		String [] args = new String [hierarchy.length+2];
		args [0] = folder.getModID ();
		for (int i=0 ; i<hierarchy.length ; i++) args [i+1] = hierarchy [i];
		args [args.length-1] = allowAccess ? "allow" : "forbid";
		
		return args;
	}
	
	public void allowAccessToAll (){
		
		String [] args = getArgsForPacketSending (true);
		
		switch (accessByDefault){
		
			case ALLOW :
				
				for (String accessException : accessExceptions){
					DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), getAPlayer (accessException));
				}
				
				break;
				
			case FORBID :
				
				for (EntityPlayerMP player : getAllPlayers()){
					if (!accessExceptions.contains (player.getName())){
						DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), player);
					}
				}
				
				break;
		}
		
		accessByDefault = AccessByDefault.ALLOW;
		accessExceptions.clear();
	}
	
	public void allowAccessTo (String playerName){
		
		boolean needToSendAPacket = false;
		
		switch (accessByDefault){
		
			case ALLOW :
				
				if (accessExceptions.contains (playerName)){
					
					needToSendAPacket = true;
					accessExceptions.remove (playerName);
				}
				
				break;
				
			case FORBID :
				
				if (!accessExceptions.contains (playerName)){
					
					needToSendAPacket = true;
					accessExceptions.add (playerName);
				}
				
				break;
			}
		
		if (needToSendAPacket){
			DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, getArgsForPacketSending (true)), getAPlayer (playerName));
		}
	}
	
	public void allowAccessToAllExcept (String playerName) {
		
		String [] args = getArgsForPacketSending (true);
		boolean needToSendAPacket = true;
		
		switch (accessByDefault){
		
			case ALLOW :
				
				for (String accessException : accessExceptions){
					
					if (accessException.equals (playerName)){
						needToSendAPacket = false;
					}else{
						DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), getAPlayer (accessException));
					}
				}
				
				break;
				
			case FORBID :
				
				for (EntityPlayerMP player : getAllPlayers()){
					if (!accessExceptions.contains (player.getName())){
						
						if (playerName.equals (player.getName())){
							needToSendAPacket = false;
						}else{
							DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), player);
						}
					}
				}
				
				break;
		}
				
		if (needToSendAPacket){
			
			args [args.length-1] = "forbid";
			DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), getAPlayer (playerName));
		}
		
		accessByDefault = AccessByDefault.ALLOW;
		accessExceptions.clear();
		accessExceptions.add (playerName);
	}
	
	public void forbidAccessToAll (){
		
		String [] args = getArgsForPacketSending (false);
		
		switch (accessByDefault){
		
			case ALLOW :
				
				for (EntityPlayerMP player : getAllPlayers()){
					if (!accessExceptions.contains (player.getName())){
						DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), player);
					}
				}
				
				break;
				
			case FORBID :
				
				for (String accessException : accessExceptions){
					DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), getAPlayer (accessException));
				}
				
				break;
		}
		
		accessByDefault = AccessByDefault.FORBID;
		accessExceptions.clear();
	}
	
	public void forbidAccessTo (String playerName){
		
		boolean needToSendAPacket = false;
		
		switch (accessByDefault){
		
			case ALLOW :
				
				if (!accessExceptions.contains (playerName)){
					
					needToSendAPacket = true;
					accessExceptions.add (playerName);
				}
				
				break;
				
			case FORBID :
				
				if (accessExceptions.contains (playerName)){
					
					needToSendAPacket = true;
					accessExceptions.remove (playerName);
				}
				
				break;
			}
		
		if (needToSendAPacket){
			DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, getArgsForPacketSending (false)), getAPlayer (playerName));
		}
	}
	
	public void forbidAccessToAllExcept (String playerName){
		
		String [] args = getArgsForPacketSending (false);
		boolean needToSendAPacket = true;
		
		switch (accessByDefault){
		
			case ALLOW :
				
				for (EntityPlayerMP player : getAllPlayers()){
					if (!accessExceptions.contains (player.getName())){
						
						if (playerName.equals (player.getName())){
							needToSendAPacket = false;
						}else{
							DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), player);
						}
					}
				}
				
				break;
				
			case FORBID :
				
				for (String accessException : accessExceptions){
					
					if (accessException.equals (playerName)){
						needToSendAPacket = false;
					}else{
						DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), getAPlayer (accessException));
					}
				}
				
				break;
		}
				
		if (needToSendAPacket){
			
			args [args.length-1] = "allow";
			DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), getAPlayer (playerName));
		}
		
		accessByDefault = AccessByDefault.FORBID;
		accessExceptions.clear();
		accessExceptions.add (playerName);
	}
}