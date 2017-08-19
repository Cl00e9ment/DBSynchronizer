package com.trcgames.dbSynchronizer.packets;

import com.trcgames.dbSynchronizer.DatabaseGetter;
import com.trcgames.dbSynchronizer.database.DBFolder;
import com.trcgames.dbSynchronizer.database.Database;
import com.trcgames.dbSynchronizer.util.BlockPos;

public class PacketCommonHandler{
	
	/** method used by the 2 handler */
	protected static void setRemoveData (String [] args){
		
		// args array :
		// modID - persistent folder/non-persistent folder - sub folder 1 - sub folder 2 - etc... - set/remove - type - key - value (empty String if remove or for DBFolder)
		
		Database database = DatabaseGetter.getInstance (args [0]);
		DBFolder folder = null;
		
		if (args [1].equals ("persistent folder")){
			folder = database.getPersistentFolder();
			
		}else if (args [1].equals ("non-persistent folder")){
			folder = database.getNonPersistentFolder();
		}
		
		for (int i=2 ; i<args.length-4 ; i++){
			folder = folder.getDBFolder (args [i]);
		}

		String action = args [args.length-4];
		String type = args [args.length-3];
		String key = args [args.length-2];
		String value = args [args.length-1];
		
		if (action.equals ("set")){
			
			if (type.equals ("BlockPos")){
				
				String [] coo = value.split (":");
				folder.setData (key, new BlockPos (Integer.valueOf (coo[0]), Integer.valueOf (coo[1]), Integer.valueOf (coo[2])), value, false);
				
			}else if (type.equals ("boolean")){
				folder.setData (key, Boolean.valueOf (value), value, false);
				
			}else if (type.equals ("byte")){
				folder.setData (key, Byte.valueOf  (value), value, false);
				
			}else if (type.equals ("char")){
				folder.setData (key, value.charAt (0), value, false);
				
			}else if (type.equals ("double")){
				folder.setData (key, Double.valueOf (value), value, false);
				
			}else if (type.equals ("float")){
				folder.setData (key, Float.valueOf (value), value, false);
				
			}else if (type.equals ("DBFolder")){
				folder.addNewDBFolderFromPacket (key);
				
			}else if (type.equals ("int")){
				folder.setData (key, Integer.valueOf (value), value, false);
				
			}else if (type.equals ("long")){
				folder.setData (key, Long.valueOf (value), value, false);
				
			}else if (type.equals ("short")){
				folder.setData (key, Short.valueOf (value), value, false);
				
			}else if (type.equals ("String")){
				folder.setData (key, value, value, false);
			}
			
		}else if (action.equals ("remove")){
			
			if (type.equals ("BlockPos")){
				folder.removeData (key, BlockPos.class, false);
				
			}else if (type.equals ("boolean")){
				folder.removeData (key, Boolean.class, false);
				
			}else if (type.equals ("byte")){
				folder.removeData (key, Byte.class, false);
				
			}else if (type.equals ("char")){
				folder.removeData (key, Character.class, false);
				
			}else if (type.equals ("double")){
				folder.removeData (key, Double.class, false);
				
			}else if (type.equals ("float")){
				folder.removeData (key, Float.class, false);
				
			}else if (type.equals ("DBFolder")){
				folder.removeData (key, DBFolder.class, false);
				
			}else if (type.equals ("int")){
				folder.removeData (key, Integer.class, false);
				
			}else if (type.equals ("long")){
				folder.removeData (key, Long.class, false);
				
			}else if (type.equals ("short")){
				folder.removeData (key, Short.class, false);
				
			}else if (type.equals ("String")){
				folder.removeData (key, String.class, false);
			}
		}
	}
}