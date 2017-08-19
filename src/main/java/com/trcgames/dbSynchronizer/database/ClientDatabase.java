package com.trcgames.dbSynchronizer.database;

import java.util.ArrayList;

import com.trcgames.dbSynchronizer.DBSynchronizer;
import com.trcgames.dbSynchronizer.packets.PacketClientToServer;
import com.trcgames.dbSynchronizer.packets.PacketClientToServer.CtSPacketType;

public class ClientDatabase implements Database{
	
	//---------------------------------
	// STATIC
	//---------------------------------
	
	private static ArrayList<ClientDatabase> instances = new ArrayList<ClientDatabase>();
	
	public static void onClientLeave (){
		instances.clear ();
	}
	
	/** synchronized for avoid ConcurrentModificationException with instances */
	public synchronized static void addInstance (String modID){
		
		for (ClientDatabase instance : instances){
			if (instance.modID.equals (modID)) return;
		}
		
		instances.add (new ClientDatabase (modID));
	}
	
	/** synchronized for avoid ConcurrentModificationException with instances */
	public synchronized static ClientDatabase getInstance (String modID){
		
		for (ClientDatabase instance : instances){
			if (instance.modID.equals (modID)) return instance;
		}
		
		addInstance (modID);
		DBSynchronizer.network.sendToServer (new PacketClientToServer (CtSPacketType.ADD_MOD_ID, modID));
		
		return instances.get (instances.size()-1);
	}
	
	//---------------------------------
	// OBJECT
	//---------------------------------
	
	private String modID;
	private DBFolder persistentFolder, nonPersistentFolder;
	
	private ClientDatabase (String modID){
		
		this.modID = modID;
		persistentFolder = new DBFolder (modID, "persistent folder", null, null);
		nonPersistentFolder = new DBFolder (modID, "non-persistent folder", null, null);
	}
	
	@Override
	public DBFolder getPersistentFolder (){
		return persistentFolder;
	}
	
	@Override
	public DBFolder getNonPersistentFolder (){
		return nonPersistentFolder;
	}
}