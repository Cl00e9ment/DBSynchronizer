package com.trcgames.dbSynchronizer.database;

import java.util.ArrayList;

import com.trcgames.dbSynchronizer.DBSynchronizer;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;
import net.minecraftforge.common.DimensionManager;

public class DataSaver extends WorldSavedData{
	
	//---------------------------------
	// STATIC
	//---------------------------------
	
	private static DataSaver instance;
	
	public static DataSaver getInstance (){
		
		if (instance == null) initInstance ();
		return instance;
	}
	
	private static void initInstance (){
		
		String key = DBSynchronizer.MOD_ID;
		
		MapStorage storage = DimensionManager.getWorlds()[0].getMapStorage();
		instance = (DataSaver) storage.loadData (DataSaver.class, key);
		
		if (instance == null){
			
			instance = new DataSaver (key);
			storage.setData (key, instance);
		}
	}
	
	public static void killInstance (){
		instance = null;
	}
	
	//---------------------------------
	// OBJECT
	//---------------------------------
	
	private ArrayList<String> modIDs = new ArrayList<String>();
	
	public DataSaver (String name){
		super (name);
	}
	
	public String[] getModIDs (){
		
		String [] array = new String [modIDs.size()];
		return modIDs.toArray (array);
	}
	
	/** synchronized for avoid ConcurrentModificationException with modIDs */
	public synchronized void addAModID (String modID){
		
		for (String id : modIDs){
			if (id.equals (modID)) return;
		}
		
		modIDs.add (modID);
		markDirty();
	}
	
	@Override
	public void readFromNBT (NBTTagCompound compound){
		
		String modIDsStr = compound.getString ("modIDs");
		
		modIDs.clear ();
		
		if (!modIDsStr.equals ("")){
			for (String modID : modIDsStr.split (":")){
				modIDs.add (modID);
			}
		}
	}
	
	@Override
	public void writeToNBT (NBTTagCompound compound){
		
		String modIDsStr = "";
		
		if (modIDs.size() > 0){
			
			for (String modID : modIDs){
				
				if (modIDsStr.equals ("")) modIDsStr = modID;
				else modIDsStr += ':' + modID;
			}
		}
		
		compound.setString ("modIDs", modIDsStr);
	}
}