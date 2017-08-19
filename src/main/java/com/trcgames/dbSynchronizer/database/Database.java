package com.trcgames.dbSynchronizer.database;

public interface Database{
	
	/**
	 * @return The root folder of the persistent data (data that will be stored in the world save).
	 */
	public DBFolder getPersistentFolder ();
	
	/**
	 * @return The root folder of the non-persistent data (data that will not be stored in the world save).
	 */
	public DBFolder getNonPersistentFolder ();
}