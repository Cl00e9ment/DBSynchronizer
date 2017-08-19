package com.trcgames.dbSynchronizer;

import com.trcgames.dbSynchronizer.database.ClientDatabase;
import com.trcgames.dbSynchronizer.database.Database;
import com.trcgames.dbSynchronizer.database.ServerDatabase;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class DatabaseGetter{
	
	/**
	 * For get a Database object. It can be a ServerDatabase or a ClientDatabase depending of the side but the way to use it is the same.<br>
	 * You can't get a database when no world is loaded.<br>
	 * Each world possesses it specific database.<br>
	 * @param modID : The ID of your mod (it can't be null or an empty String).
	 * @return A Database object (can be a SeverDatabase or a ClientDatabase).
	 * @throws IllegalArgumentException If your mod ID is null, an empty String or if it contains ":".
	 * @throws NullPointerException if no world is loaded.
	 */
	public static Database getInstance (String modID){
		
		if (!DBSynchronizer.worldLoaded){
			throw new NullPointerException ("You can't get a database instance when no world is loaded.");
		}
		
		if (modID == null){
			throw new IllegalArgumentException ("Your mod ID can't be null.");
		}
		
		if (modID.equals ("")){
			throw new IllegalArgumentException ("Your mod ID can't be an empty String.");
		}
		
		if (modID.contains (":")){
			throw new IllegalArgumentException ("Your mod ID can't contains \":\".");
		}
		
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER){
			return ServerDatabase.getInstance (modID);
			
		}else{
			return ClientDatabase.getInstance (modID);
		}
	}
}