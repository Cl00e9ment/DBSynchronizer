package com.trcgames.dbSynchronizer.database.dbFolderAccessControl;

import com.trcgames.dbSynchronizer.database.DBFolder;

public abstract class AccessController{
	
	protected DBFolder folder;
	
	public AccessController (DBFolder folder){
		this.folder = folder;
	}
	
	public abstract boolean canAccessToData ();
}