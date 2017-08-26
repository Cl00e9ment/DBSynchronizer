package com.trcgames.dbSynchronizer.database.dbFolderAccessControl;

public class ClientAccessController extends AccessController {

	private boolean canAccessToData, accessAllowedTentatively;
	
	@Override
	public void dbFolderAddedToDataBaseByUser (){
		canAccessToData = true;
	}
	
	public void setAccessAuthorization (boolean canAccessToData){
		this.canAccessToData = canAccessToData;
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
