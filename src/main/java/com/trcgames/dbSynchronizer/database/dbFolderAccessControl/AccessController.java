package com.trcgames.dbSynchronizer.database.dbFolderAccessControl;

public abstract class AccessController{
	
	public abstract boolean canAccessToData ();
	public abstract void dbFolderAddedToDataBaseByUser ();
}