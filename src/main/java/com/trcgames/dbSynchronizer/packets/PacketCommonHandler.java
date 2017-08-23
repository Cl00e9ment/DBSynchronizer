package com.trcgames.dbSynchronizer.packets;

import com.trcgames.dbSynchronizer.DatabaseGetter;
import com.trcgames.dbSynchronizer.database.ClientDatabase;
import com.trcgames.dbSynchronizer.database.DBFolder;
import com.trcgames.dbSynchronizer.database.Database;
import com.trcgames.dbSynchronizer.database.ServerDatabase;
import com.trcgames.dbSynchronizer.database.dbFolderAccessControl.ClientAccessController;
import com.trcgames.dbSynchronizer.database.dbFolderAccessControl.ServerAccessController;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class PacketCommonHandler{
	
	/**method used by the 2 handler
	 * @return <b>true</b> if data was set/removed successfully, <b>false</b> either
	 */
	protected static boolean setRemoveData (String [] args, String packetSender){
		
		// args array :
		// modID - persistent folder/non-persistent folder - sub folder 1 - sub folder 2 - etc... - set/remove - type - key - value (empty String if remove and for DBFolder)
		
		if (args.length < 6) return false;
		
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		
		if (side == Side.SERVER && !ServerDatabase.doesInstanceStored (args [0])
		 || side == Side.CLIENT && !ClientDatabase.doesInstanceStored (args [0])) return false;
		
		Database database = DatabaseGetter.getInstance (args [0]);
		DBFolder folder = null;
		
		if (args [1].equals ("persistent folder")){
			folder = database.getPersistentFolder();
			
		}else if (args [1].equals ("non-persistent folder")){
			folder = database.getNonPersistentFolder();
			
		}else return false;
		
		for (int i=2 ; i<args.length-4 ; i++){
			
			if (!folder.doesSubFolderExist(args [i])) return false;
			folder = folder.getDBFolder (args [i]);
		}
		
		String action = args [args.length-4];
		String type = args [args.length-3];
		String key = args [args.length-2];
		String value = args [args.length-1];
		
		if (side == Side.CLIENT){
			((ClientAccessController) folder.getAccessController()).allowAccessTentatively();
			
		}else if (side == Side.SERVER){
			
			if (!((ServerAccessController) folder.getAccessController()).canPlayerAccessToData (packetSender)){
				return false;
			}
		}
		
		if (action.equals ("set")){
			
			if (type.equals ("BlockPos")){
				
				String [] coo = value.split (":");
				if (coo.length != 3) return false;
				
				int x, y, z;
				
				try{
					
					x = Integer.parseInt (coo [0]);
					y = Integer.parseInt (coo [1]);
					z = Integer.parseInt (coo [2]);
					
				}catch (NumberFormatException e){
					return false;
				}
				
				return folder.setData (key, new BlockPos (x, y, z), value, false);
				
			}else if (type.equals ("boolean")){
				return folder.setData (key, Boolean.valueOf (value), value, false);
				
			}else if (type.equals ("byte")){
				return folder.setData (key, Byte.valueOf  (value), value, false);
				
			}else if (type.equals ("char")){
				return folder.setData (key, value.charAt (0), value, false);
				
			}else if (type.equals ("double")){
				return folder.setData (key, Double.valueOf (value), value, false);
				
			}else if (type.equals ("float")){
				return folder.setData (key, Float.valueOf (value), value, false);
				
			}else if (type.equals ("DBFolder")){
				return folder.addNewDBFolderFromPacket (key);
				
			}else if (type.equals ("int")){
				return folder.setData (key, Integer.valueOf (value), value, false);
				
			}else if (type.equals ("long")){
				return folder.setData (key, Long.valueOf (value), value, false);
				
			}else if (type.equals ("short")){
				return folder.setData (key, Short.valueOf (value), value, false);
				
			}else if (type.equals ("String")){
				return folder.setData (key, value, value, false);
				
			}else return false;
			
		}else if (action.equals ("remove")){
			
			if (type.equals ("BlockPos")){
				return folder.removeData (key, BlockPos.class, false);
				
			}else if (type.equals ("boolean")){
				return folder.removeData (key, Boolean.class, false);
				
			}else if (type.equals ("byte")){
				return folder.removeData (key, Byte.class, false);
				
			}else if (type.equals ("char")){
				return folder.removeData (key, Character.class, false);
				
			}else if (type.equals ("double")){
				return folder.removeData (key, Double.class, false);
				
			}else if (type.equals ("float")){
				return folder.removeData (key, Float.class, false);
				
			}else if (type.equals ("DBFolder")){
				return folder.removeData (key, DBFolder.class, false);
				
			}else if (type.equals ("int")){
				return folder.removeData (key, Integer.class, false);
				
			}else if (type.equals ("long")){
				return folder.removeData (key, Long.class, false);
				
			}else if (type.equals ("short")){
				return folder.removeData (key, Short.class, false);
				
			}else if (type.equals ("String")){
				return folder.removeData (key, String.class, false);
				
			}else return false;
		}else return false;
	}
}