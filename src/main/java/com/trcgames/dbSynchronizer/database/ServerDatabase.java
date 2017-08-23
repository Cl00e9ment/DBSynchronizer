package com.trcgames.dbSynchronizer.database;

import java.util.ArrayList;

import com.trcgames.dbSynchronizer.DBSynchronizer;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient.StCPacketType;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

public class ServerDatabase extends WorldSavedData implements Database{
	
	//---------------------------------
	// STATIC
	//---------------------------------
	
	private static ArrayList<ServerDatabase> instances = new ArrayList<ServerDatabase>();
	
	private static ServerDatabase initInstance (String modID){
		
		String key = DBSynchronizer.MOD_ID +'-'+ modID;
		
		MapStorage storage = DimensionManager.getWorlds()[0].getMapStorage();
		ServerDatabase instance = (ServerDatabase) storage.getOrLoadData (ServerDatabase.class, key);
		
		if (instance == null){
			
			DataSaver.getInstance().addAModID (modID);
			
			instance = new ServerDatabase (key);
			storage.setData (key, instance);
			
			instance.createThePersistentFolder ();
			instance.markDirty ();
		}
		
		instances.add (instance);
		return instance;
	}
	
	public static void onServerStarting (){
		
		for (String modID : DataSaver.getInstance().getModIDs()){
			initInstance (modID);
		}
	}
	
	public static void onServerStopping (){
		instances.clear();
	}
	
	/** synchronized for avoid ConcurrentModificationException with instances */
	public static synchronized ServerDatabase getInstance (String modID){
		
		for (ServerDatabase instance : instances){
			if (instance.modID.equals (modID)) return instance;
		}
		
		DBSynchronizer.network.sendToAll (new PacketServerToClient (StCPacketType.ADD_MOD_IDS, modID));
		return initInstance (modID);
	}
	
	public static boolean doesInstanceStored (String modID){
		
		for (ServerDatabase instance : instances){
			if (instance.modID.equals (modID)) return true;
		}
		
		return false;
	}
	
	//---------------------------------
	// OBJECT
	//---------------------------------
	
	private String modID;
	private DBFolder persistentFolder, nonPersistentFolder;
	
	public ServerDatabase (String key){
		
		super (key);
		
		modID = key.substring (DBSynchronizer.MOD_ID.length()+1);
		nonPersistentFolder = new DBFolder (modID, "non-persistent folder", null, null);
	}
	
	private void createThePersistentFolder (){
		persistentFolder = new DBFolder (modID, "persistent folder", null, null);
	}
	
	@Override
	public DBFolder getPersistentFolder (){
		return persistentFolder;
	}
	
	@Override
	public DBFolder getNonPersistentFolder (){
		return nonPersistentFolder;
	}
	
	@Override
	public void readFromNBT (NBTTagCompound compound){
		persistentFolder = new DBFolder (modID, "persistent folder", null, compound.getCompoundTag ("persistent folder"));
	}
	
	@Override
	public NBTTagCompound writeToNBT (NBTTagCompound compound){
		
		NBTTagCompound folderTag = new NBTTagCompound ();
		persistentFolder.saveInNBT (folderTag);
		compound.setTag ("persistent folder", folderTag);
		
		return compound;
	}
}