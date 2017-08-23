package com.trcgames.dbSynchronizer.database.dbFolderAccessControl;

import com.trcgames.dbSynchronizer.database.DBFolder;

public class ClientAccessController extends AccessController {

	private boolean canAccessToData, accessAllowedTentatively;
	
	public ClientAccessController (DBFolder folder){
		super (folder);
	}
	
	public void setAccessAuthorization (boolean canAccessToData){
		
		this.canAccessToData = canAccessToData;
		
		// TODO remove
		/*
		for (DBFolder subFolder : folder.getSubFolders()){
			((ClientAccessController) subFolder.getAccessController()).setAccessAuthorization (canAccessToData);
		}*/
	}
	
	@Override
	public boolean canAccessToData (){
		
		if (accessAllowedTentatively){
			
			accessAllowedTentatively = false;
			return true;
			
		}else return canAccessToData;
	}
	
	/** For modify data according to the server instructions. */
	public void allowAccessTentatively (){
		accessAllowedTentatively = true;
	}
}
