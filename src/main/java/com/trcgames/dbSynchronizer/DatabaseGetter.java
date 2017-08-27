package com.trcgames.dbSynchronizer;

import com.trcgames.dbSynchronizer.database.ClientDatabase;
import com.trcgames.dbSynchronizer.database.Database;
import com.trcgames.dbSynchronizer.database.ServerDatabase;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class DatabaseGetter{
	
	/**
	 * Registers your modID for get the database instance later.<br>
	 * Must be called during the preinit.
	 * @param modID : The ID of your mod (it can't be null, an empty String or contain ":").
	 * @throws IllegalArgumentException If your mod ID is an empty String or if it contains ":".
	 * @throws NullPointerException If your mod ID is null.
	 */
	public static void registerInstance (String modID){
		
		checkModID (modID);
		
		if (!DBSynchronizer.instance.isInitialized()){
			ServerDatabase.addAModID (modID);
		}
	}
	
	/**
	 * For get a Database object. It can be a ServerDatabase or a ClientDatabase depending of the side but the way to use it is the same.<br>
	 * You can't get a database when no world is loaded.<br>
	 * Each world possesses it specific database.<br>
	 * @param modID : The ID of your mod (it can't be null, an empty String or contain ":").
	 * @return A Database object (can be a SeverDatabase or a ClientDatabase).
	 * @throws IllegalArgumentException If your mod ID is an empty String or if it contains ":".
	 * @throws NullPointerException If your mod ID is null or if no world is loaded.
	 */
	public static Database getInstance (String modID){
		
		checkModID (modID);
		
		if (!DBSynchronizer.instance.isWorldLoaded()){
			throw new NullPointerException ("You can't get a database instance when no world is loaded.");
		}
		
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER){
			return ServerDatabase.getInstance (modID);
			
		}else{
			return ClientDatabase.getInstance (modID);
		}
	}
	
	private static void checkModID (String modID){
		
		if (modID == null){
			throw new NullPointerException ("Your mod ID can't be null.");
		}
		
		if (modID.equals ("")){
			throw new IllegalArgumentException ("Your mod ID can't be an empty String.");
		}
		
		if (modID.contains (":")){
			throw new IllegalArgumentException ("Your mod ID can't contains \":\".");
		}
	}
}