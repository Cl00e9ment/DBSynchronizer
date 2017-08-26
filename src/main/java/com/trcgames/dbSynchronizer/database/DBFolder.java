package com.trcgames.dbSynchronizer.database;

import java.util.ArrayList;

import com.trcgames.dbSynchronizer.DBSynchronizer;
import com.trcgames.dbSynchronizer.DatabaseGetter;
import com.trcgames.dbSynchronizer.database.dbFolderAccessControl.AccessController;
import com.trcgames.dbSynchronizer.database.dbFolderAccessControl.ClientAccessController;
import com.trcgames.dbSynchronizer.database.dbFolderAccessControl.ServerAccessController;
import com.trcgames.dbSynchronizer.packets.PacketClientToServer;
import com.trcgames.dbSynchronizer.packets.PacketClientToServer.CtSPacketType;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient.StCPacketType;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class DBFolder{
	
	private ArrayList<Object> dataArray = new ArrayList<Object>();
	private ArrayList<String> keysArray = new ArrayList<String>();
	private DBFolder parentFolder;
	private String modID;
	private String name;
	
	private Side side;
	protected AccessController accessController;
	
	// for modders
	public DBFolder (){}
	
	// for library usages
	protected DBFolder (String modID, String name, DBFolder parentFolder, NBTTagCompound tag){
		
		setModID (modID);
		this.name = name;
		this.parentFolder = parentFolder;
		if (tag != null) initFromNBT (tag);
	}
	
	/** This method doesn't just set the modID. It does a part of the DBFolder integration to the database. */
	private void setModID (String modID){
		
		this.modID = modID;
		side = FMLCommonHandler.instance().getEffectiveSide();
		
		for (DBFolder subFolder : getSubFolders()){
			subFolder.setModID (modID);
		}
		
		if (modID == null) accessController = null;
		else if (side == Side.SERVER) accessController = new ServerAccessController (this);
		else if (side == Side.CLIENT) accessController = new ClientAccessController ();
	}
	
	/** @return The ID of the mod that possesses the database instance where this DBFolder is contained. Or null if this DBFolder aren't contained in a database instance. */
	public String getModID (){
		return modID;
	}
	
	/** @return The parent folder.*/
	public DBFolder getParentFolder (){
		return parentFolder;
	}
	
	/** Retrieves an ArrayList that contains all DBFolder directly contained in this DBFolder.*/
	public ArrayList<DBFolder> getSubFolders (){
		
		ArrayList<DBFolder> subFolders = new ArrayList<DBFolder>();
		
		for (Object data : dataArray){
			
			if (data instanceof DBFolder){
				subFolders.add((DBFolder) data);
			}
		}
		
		return subFolders;
	}
	
	/** Retrieves <b>true</b> if this DBFolder contains another DBFolder with the specified key, <b>false</b> either.*/
	public boolean doesSubFolderExist (String key){
		
		for (Object data : dataArray){
			
			if (data instanceof DBFolder && ((DBFolder) data).getName().equals (key)){
				return true;
			}
		}
		
		return false;
	}
	
	private void synchronizeContent (){
		
		if (modID == null) return;
		
		for (int i=0 ; i<keysArray.size() ; i++){
			
			String key = keysArray.get (i);
			String dataType = getTypeName (dataArray.get(i).getClass());
			String value = null;
			
			if (dataType.equals ("BlockPos")){
				
				BlockPos pos = getBlockPos (key);
				value = pos.getX() +":"+ pos.getY() +":"+ pos.getZ();
				
			}else if (dataType.equals ("boolean")) value = ""+getBoolean (key);
			else if (dataType.equals ("byte")) value = ""+getByte (key);
			else if (dataType.equals ("char")) value = ""+getChar (key);
			else if (dataType.equals ("double")) value = ""+getDouble (key);
			else if (dataType.equals ("float")) value = ""+getFloat (key);
			else if (dataType.equals ("DBFolder")) value = "";
			else if (dataType.equals ("int")) value = ""+getInt (key);
			else if (dataType.equals ("long")) value = ""+getLong (key);
			else if (dataType.equals ("short")) value = ""+getShort (key);
			else if (dataType.equals ("String")) value = getString (key);
			
			dataModified ("set", dataType, key, value);
			if (dataType.equals ("DBFolder")) getDBFolder (key).synchronizeContent ();
		}
	}
	
	private void dataModified (String action, String type, String key, String value){
		
		if (modID == null) return;
		
		String [] hierarchy = getHierarchy();
		String [] args = new String [hierarchy.length+5];
		
		args [0] = modID;
		for (int i=0 ; i<hierarchy.length ; i++) args [i+1] = hierarchy [i];
		args [args.length-4] = action;
		args [args.length-3] = type;
		args [args.length-2] = key;
		args [args.length-1] = value;
		
		if (side == Side.SERVER){
			
			if (args [1].equals ("persistent folder")){
				((ServerDatabase) DatabaseGetter.getInstance (modID)).markDirty ();
			}
			
			DBSynchronizer.network.sendToAll (new PacketServerToClient (StCPacketType.SET_REMOVE_DATA, args));
			
		}else if (side == Side.CLIENT){
			DBSynchronizer.network.sendToServer (new PacketClientToServer (CtSPacketType.SET_REMOVE_DATA, args));
		}
	}
	
	/** Retrieves an array that contain all parent folders name.<br>
	 * 	example : {"persistent folder", "sub-folder", "sub-sub-folder", "this folder"}*/
	public String [] getHierarchy (){
		
		String [] hierarchy = getHierarchy (null);
		return hierarchy;
		
		//return getHierarchy (null);
	}
	
	/** recursive method */
	private String [] getHierarchy (String hierarchy){
		
		if (hierarchy == null) hierarchy = name;
		else hierarchy = name +':'+ hierarchy;
		
		if (parentFolder == null) return hierarchy.split (":");
		else return parentFolder.getHierarchy (hierarchy);
	}
	
	/** @return The name of this folder (its key). */
	public String getName (){
		return name;
	}
	
	/** @return A String array that contain all keys. Some keys can appear many time because different data (an int and a boolean for example) can have the same key. */
	public String [] getKeys (){
		
		String [] array = new String [keysArray.size()];
		return keysArray.toArray (array);
	}
	
	public String getDataType (int index){
		
		if (index < 0 || index > dataArray.size()-1) return null;
		return getTypeName (dataArray.get (index).getClass());
	}
	
	private String getTypeName (Class<? extends Object> type){
		
		if (type == Integer.class) return "int";
		else if (type == Character.class) return "char";
		else if (type == BlockPos.class || type == DBFolder.class || type == String.class) return type.getSimpleName();
		else return type.getSimpleName().toLowerCase();
	}
	
	// synchronized for avoid ConcurrentModificationException with dataArray and keysArray
	public synchronized DBFolder copy (){
		
		DBFolder folder = new DBFolder ();
		
		for (String key : keysArray){
			folder.keysArray.add (key);
		}
		
		for (Object data : dataArray){
			
			if (data instanceof BlockPos){
				BlockPos pos = ((BlockPos) data);
				folder.dataArray.add (new BlockPos (pos.getX(), pos.getY(), pos.getZ()));
				
			}else if (data instanceof Boolean){
				folder.dataArray.add (((Boolean) data).booleanValue ());
				
			}else if (data instanceof Byte){
				folder.dataArray.add (((Byte) data).byteValue ());
				
			}else if (data instanceof Character){
				folder.dataArray.add (((Character) data).charValue ());
				
			}else if (data instanceof DBFolder){
				folder.dataArray.add (((DBFolder) data).copy ());
				
			}else if (data instanceof Double){
				folder.dataArray.add (((Double) data).doubleValue ());
				
			}else if (data instanceof Float){
				folder.dataArray.add (((Float) data).floatValue ());
				
			}else if (data instanceof Integer){
				folder.dataArray.add (((Integer) data).intValue ());
				
			}else if (data instanceof Long){
				folder.dataArray.add (((Long) data).longValue ());
				
			}else if (data instanceof Short){
				folder.dataArray.add (((Short) data).shortValue ());
				
			}else if (data instanceof String){
				folder.dataArray.add ((String) data);
			}
		}
		
		return folder;
	}
	
	/**
	 * Print the content of the folder in the console.
	 * @param unicodeSuportedByTheConsole : Before set this true, make sure that your IDE's console support unicode characters.
	 */
	public void printInConsole (boolean unicodeSuportedByTheConsole){
		printInConsole ("", unicodeSuportedByTheConsole);
	}
	
	/** synchronized for avoid ConcurrentModificationException with dataArray and keysArray */
	private synchronized void printInConsole (String prefix, boolean unicodeSuportedByTheConsole){
		
		char lineX = unicodeSuportedByTheConsole ? '\u251C' : '|';
		char lineI = unicodeSuportedByTheConsole ? '\u2502' : '|';
		char lineL = unicodeSuportedByTheConsole ? '\u2514' : '|';
		
		String folderDisplayName = (name == null) ? "no-name" : name;
		
		if (prefix.length () > 0){
			
			char line = (prefix.charAt (prefix.length()-1) == ' ') ? lineL : lineX;
			System.out.println (prefix.substring (0, prefix.length()-1) + line + "(DBFolder) " + folderDisplayName);
			
		}else System.out.println ("(DBFolder) " + folderDisplayName);
		
		for (int i=0 ; i<keysArray.size() ; i++){
			
			Object data = dataArray.get (i);
			
			if (data instanceof DBFolder){
				
				char line = (i == keysArray.size()-1) ? ' ' : lineI;
				((DBFolder) data).printInConsole (prefix + line, unicodeSuportedByTheConsole);
				
			}else{
				
				String type = "unknow";
				String value = "unknow";
				
				if (data instanceof BlockPos){
					
					BlockPos pos = (BlockPos) dataArray.get (i);
					
					type = "BlockPos";
					value = "x=" + pos.getX() + " y=" + pos.getY() + " z=" + pos.getZ();
					
				}else if (data instanceof Boolean){
					
					type = "boolean";
					value = ((Boolean) data).booleanValue () +"";
					
				}else if (data instanceof Byte){
					
					type = "byte";
					value = ((Byte) data).byteValue () +"";
					
				}else if (data instanceof Character){
					
					type = "char";
					value = ((Character) data).charValue () +"";
					
				}else if (data instanceof Double){
					
					type = "double";
					value = ((Double) data).doubleValue () +"";
					
				}else if (data instanceof Float){
					
					type = "float";
					value = ((Float) data).floatValue () +"";
					
				}else if (data instanceof Integer){
					
					type = "int";
					value = ((Integer) data).intValue () +"";
					
				}else if (data instanceof Long){
					
					type = "long";
					value = ((Long) data).longValue () +"";
					
				}else if (data instanceof Short){
					
					type = "short";
					value = ((Short) data).shortValue () +"";
					
				}else if (data instanceof String){
					
					type = "String";
					value = (String) data;
				}
				
				char line = (i == keysArray.size()-1) ? lineL : lineX;
				System.out.println (prefix + line + '(' + type + ") " + keysArray.get(i) + " : " + value);
			}
		}
	}
	
	//---------------------------------
	// ACCESS CONTROL
	//---------------------------------
	
	/**<span style="font-size : 2em">DON'T USE THIS METHOD !</span><br>
	 * it can desynchronize the clients/server
	 */
	public AccessController getAccessController (){
		return accessController;
	}
	
	/** @return <b>true</b> if you can modify data inside this DBFolder (if you are on the server side or if the server has allowed you), <b>false</b> either. */
	public boolean canModifyDataInside (){
		return accessController.canAccessToData();
	}
	
	// Server side only :
	
	/**
	 * Allows all players to modify the data inside this DBFolder.<br>
	 * You can use this method only on the server side.
	 * @throws IllegalAccessException If you use this method on the client side.
	 */
	public void allowAccessToAll () throws IllegalAccessException{
		
		if (modID == null) return;
		if (side == Side.CLIENT) throw new IllegalAccessException ();
		
		((ServerAccessController) accessController).allowAccessToAll();
		
		for (DBFolder subFolder : getSubFolders()){
			subFolder.allowAccessToAll();
		}
	}
	
	/**
	 * Allows a player to modify the data inside this DBFolder.<br>
	 * You can use this method only on the server side.
	 * @param playerName Name of the allowed player.
	 * @throws IllegalAccessException If you use this method on the client side.
	 */
	public void allowAccessTo (String playerName) throws IllegalAccessException{
		
		if (modID == null) return;
		if (side == Side.CLIENT) throw new IllegalAccessException ();
		
		((ServerAccessController) accessController).allowAccessTo (playerName);
		
		for (DBFolder subFolder : getSubFolders()){
			subFolder.allowAccessTo (playerName);
		}
	}
	
	/**
	 * Allows all players except one to modify the data inside this DBFolder.<br>
	 * You can use this method only on the server side.
	 * @param playerName Name of the not allowed player.
	 * @throws IllegalAccessException If you use this method on the client side.
	 */
	public void allowAccessToAllExcept (String playerName) throws IllegalAccessException{
		
		if (modID == null) return;
		if (side == Side.CLIENT) throw new IllegalAccessException ();
		
		((ServerAccessController) accessController).allowAccessToAllExcept (playerName);
		
		for (DBFolder subFolder : getSubFolders()){
			subFolder.allowAccessToAllExcept (playerName);
		}
	}
	
	/**
	 * Forbids all players to modify the data inside this DBFolder.<br>
	 * You can use this method only on the server side.
	 * @throws IllegalAccessException If you use this method on the client side.
	 */
	public void forbidAccessToAll () throws IllegalAccessException{
		
		if (modID == null) return;
		if (side == Side.CLIENT) throw new IllegalAccessException ();
		
		((ServerAccessController) accessController).forbidAccessToAll();
		
		for (DBFolder subFolder : getSubFolders()){
			subFolder.forbidAccessToAll();
		}
	}
	
	/**
	 * Forbids a player to modify the data inside this DBFolder.<br>
	 * You can use this method only on the server side.
	 * @param playerName Name of the allowed player.
	 * @throws IllegalAccessException If you use this method on the client side.
	 */
	public void forbidAccessTo (String playerName) throws IllegalAccessException{
		
		if (modID == null) return;
		if (side == Side.CLIENT) throw new IllegalAccessException ();
		
		((ServerAccessController) accessController).forbidAccessTo (playerName);
		
		for (DBFolder subFolder : getSubFolders()){
			subFolder.forbidAccessTo (playerName);
		}
	}
	
	/**
	 * Forbids all players except one to modify the data inside this DBFolder.<br>
	 * You can use this method only on the server side.
	 * @throws IllegalAccessException If you use this method on the client side.
	 */
	public void forbidAccessToAllExcept (String playerName) throws IllegalAccessException{
		
		if (modID == null) return;
		if (side == Side.CLIENT) throw new IllegalAccessException ();
		
		((ServerAccessController) accessController).forbidAccessToAllExcept (playerName);
		
		for (DBFolder subFolder : getSubFolders()){
			subFolder.forbidAccessToAllExcept (playerName);
		}
	}
	
	//---------------------------------
	// GETTERS & SETTERS
	//---------------------------------
	
	/**Stores the given BlockPos object using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException If key contains ':'
	 * @throws NullPointerException If key or value are null. Use <u>removeBlockPos (String key)</u> for remove a BlockPos.
	 * @return <b>true</b> if the BlockPos has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setBlockPos (String key, BlockPos value){
		
		if (value == null) throw new NullPointerException ("value can't be null");
		return setData (key, value, value.getX()+":"+value.getY()+":"+value.getZ(), true);
	}
	
	/**Stores the given boolean value using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key is null.
	 * @return <b>true</b> if the boolean has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setBoolean (String key, boolean value){
		return setData (key, new Boolean (value), value+"", true);
	}
	
	/**Stores the given byte value using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key is null.
	 * @return <b>true</b> if the byte has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setByte (String key, byte value){
		return setData (key, new Byte (value), value+"", true);
	}
	
	/**Stores the given char value using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key is null.
	 * @return <b>true</b> if the char has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setChar (String key, char value){
		return setData (key, new Character (value), value+"", true);
	}
	
	/**Stores the given DBFolder object using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key or value are null. Use <u>removeDBFolder (String key)</u> for remove a DBFolder.
	 * @return <b>true</b> if the DBFolder has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setDBFolder (String key, DBFolder value){
		
		if (value == null) throw new NullPointerException ("value can't be null");
		
		if (modID == null || accessController.canAccessToData()){
			
			value.parentFolder = this;
			value.setModID (modID);
			value.name = key;
			
			setData (key, value, "", true);
			
			value.synchronizeContent ();
			value.accessController.dbFolderAddedToDataBaseByUser();
			
			return true;
			
		}else return false;
	}
	
	/**
	 * <span style="font-size : 2em">DON'T USE THIS METHOD !</span><br>
	 * it can desynchronize the clients/server or corrupt the save
	 */
	public boolean addNewDBFolderFromPacket (String key){
		
		DBFolder folder = new DBFolder (modID, key, this, null);
		boolean success = setData (key, folder, "", false); // "false" for avoid a re-synchronization (chain reaction)
		if (side == Side.SERVER) folder.accessController.dbFolderAddedToDataBaseByUser();
		
		return success;
	}
	
	/**Stores the given double value using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key is null.
	 * @return <b>true</b> if the double has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setDouble (String key, double value){
		return setData (key, new Double (value), value+"", true);
	}
	
	/**Stores the given float value using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key is null.
	 * @return <b>true</b> if the float has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setFloat (String key, float value){
		return setData (key, new Float (value), value+"", true);
	}
	
	/**Stores the given int value using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key is null.
	 * @return <b>true</b> if the int has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setInt (String key, int value){
		return setData (key, new Integer (value), value+"", true);
	}
	
	/**Stores the given long value using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key is null.
	 * @return <b>true</b> if the long has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setLong (String key, long value){
		return setData (key, new Long (value), value+"", true);
	}
	
	/**Stores the given short value using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key is null.
	 * @return <b>true</b> if the short has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setShort (String key, short value){
		return setData (key, new Short (value), value+"", true);
	}
	
	/**Stores the given String object using the given string key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @throws IllegalArgumentException if key is null or if it contains ':'
	 * @throws NullPointerException If key or value are null. Use <u>removeString (String key)</u> for remove a String.
	 * @return <b>true</b> if the String has been set (if you are allowed to modify it), <b>false</b> either.
	 */
	public boolean setString (String key, String value){
		
		if (value == null) throw new NullPointerException ("value can't be null");
		return setData (key, value, value, true);
	}
	
	// synchronized for avoid ConcurrentModificationException with dataArray and keysArray
	/**
	 * <span style="font-size : 2em">DON'T USE THIS METHOD !</span><br>
	 * it can desynchronize the clients/server or corrupt the save
	 */
	public synchronized boolean setData (String key, Object value, String valueStr, boolean synchronizeTheOtherSide){
		
		if (!(modID == null || accessController.canAccessToData())) return false;
		if (key == null) throw new NullPointerException ("key can't be null");
		if (key.contains (":")) throw new IllegalArgumentException ("key can't contain ':'");
		
		for (int i=0 ; i<keysArray.size() ; i++){
			if (keysArray.get(i).equals (key) && dataArray.get(i).getClass() == value.getClass()){
				
				if (dataArray.get (i) != value){
					
					dataArray.set (i, value);
					if (synchronizeTheOtherSide) dataModified ("set", getTypeName (value.getClass()), key, valueStr);
				}
				
				return true;
			}
		}
		
		keysArray.add (key);
		dataArray.add (value);
		if (synchronizeTheOtherSide) dataModified ("set", getTypeName (value.getClass()), key, valueStr);
		
		return true;
	}
	
	/**Retrieves a BlockPos object using the specified key, or null if no such key was stored.*/
	public BlockPos getBlockPos (String key){
		return (BlockPos) getData (key, BlockPos.class, null);
	}
	
	/**Retrieves a boolean value using the specified key, or false if no such key was stored.*/
	public boolean getBoolean (String key){
		return ((Boolean) getData (key, Boolean.class, new Boolean (false))).booleanValue();
	}
	
	/**Retrieves a byte value using the specified key, or 0 if no such key was stored.*/
	public byte getByte (String key){
		return ((Byte) getData (key, Byte.class, new Byte ((byte) 0))).byteValue();
	}
	
	/**Retrieves a char value using the specified key, or char 0x00 if no such key was stored.*/
	public char getChar (String key){
		return ((Character) getData (key, Character.class, new Character ((char) 0))).charValue();
	}
	
	/**
	 * Retrieves a DBFolder object using the specified key.<br>
	 * If no such key was stored, it creates a new DBFolder and stores it, but only if you have the permission (if you are on the server side or if the server allowed you), else it retrieves null.
	 */
	public DBFolder getDBFolder (String key){
		
		DBFolder folder = (DBFolder) getData (key, DBFolder.class, null);
		
		if (folder == null && accessController.canAccessToData()){
			
			folder = new DBFolder (modID, key, this, null);
			
			keysArray.add (key);
			dataArray.add (folder);
			
			dataModified ("set", "DBFolder", key, "");
			folder.accessController.dbFolderAddedToDataBaseByUser();
		}
		
		return folder;
	}
	
	/**Retrieves a double value using the specified key, or 0D if no such key was stored.*/
	public double getDouble (String key){
		return ((Double) getData (key, Double.class, new Double (0D))).doubleValue();
	}
	
	/**Retrieves a float value using the specified key, or 0F if no such key was stored.*/
	public float getFloat (String key){
		return ((Float) getData (key, Float.class, new Float (0F))).floatValue();
	}
	
	/**Retrieves an int value using the specified key, or 0 if no such key was stored.*/
	public int getInt (String key){
		return ((Integer) getData (key, Integer.class, new Integer (0))).intValue();
	}
	
	/**Retrieves a long value using the specified key, or 0L if no such key was stored.*/
	public long getLong (String key){
		return ((Long) getData (key, Long.class, new Long (0L))).longValue();
	}
	
	/**Retrieves a short value using the specified key, or 0 if no such key was stored.*/
	public short getShort (String key){
		return ((Short) getData (key, Short.class, new Short ((short) 0))).shortValue();
	}
	
	/**Retrieves a String object using the specified key, or "" if no such key was stored.*/
	public String getString (String key){
		return (String) getData (key, String.class, new String (""));
	}
	
	/** synchronized for avoid ConcurrentModificationException with dataArray and keysArray */
	private synchronized Object getData (String key, Class<? extends Object> type, Object defaultValue){
		
		for (int i=0 ; i<keysArray.size() ; i++){
			if (keysArray.get(i).equals (key) && dataArray.get(i).getClass() == type){
				
				return dataArray.get (i);
			}
		}
		
		return defaultValue;
	}
	
	/**Removes a BlockPos object using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the BlockPos has been successful removed, <b>false</b> either (if the key doesn't math with any BlockPos or if you are not allowed to remove it).*/
	public boolean removeBlockPos (String key){
		return removeData (key, BlockPos.class, true);
	}
	
	/**Removes a boolean value using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the boolean has been successful removed, <b>false</b> either (if the key doesn't math with any boolean or if you are not allowed to remove it).*/
	public boolean removeBoolean (String key){
		return removeData (key, Boolean.class, true);
	}
	
	/**Removes a byte value using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the byte has been successful removed, <b>false</b> either (if the key doesn't math with any byte or if you are not allowed to remove it).*/
	public boolean removeByte (String key){
		return removeData (key, Byte.class, true);
	}
	
	/**Removes a char value using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the char has been successful removed, <b>false</b> either (if the key doesn't math with any char or if you are not allowed to remove it).*/
	public boolean removeChar (String key){
		return removeData (key, Character.class, true);
	}
	
	/**Removes a double value using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the double has been successful removed, <b>false</b> either (if the key doesn't math with any double or if you are not allowed to remove it).*/
	public boolean removeDouble (String key){
		return removeData (key, Double.class, true);
	}
	
	/**Removes a DBFolder object using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the DBFolder has been successful removed, <b>false</b> either (if the key doesn't math with any DBFolder or if you are not allowed to remove it).*/
	public boolean removeDBFolder (String key){
		return removeData (key, DBFolder.class, true);
	}
	
	/**Removes a float value using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the float has been successful removed, <b>false</b> either (if the key doesn't math with any float or if you are not allowed to remove it).*/
	public boolean removeFloat (String key){
		return removeData (key, Float.class, true);
	}
	
	/**Removes a int value using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the int has been successful removed, <b>false</b> either (if the key doesn't math with any int or if you are not allowed to remove it).*/
	public boolean removeInt (String key){
		return removeData (key, Integer.class, true);
	}
	
	/**Removes a long value using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the long has been successful removed, <b>false</b> either (if the key doesn't math with any long or if you are not allowed to remove it).*/
	public boolean removeLong (String key){
		return removeData (key, Long.class, true);
	}
	
	/**Removes a short value using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the short has been successful removed, <b>false</b> either (if the key doesn't math with any short or if you are not allowed to remove it).*/
	public boolean removeShort (String key){
		return removeData (key, Short.class, true);
	}
	
	/**Removes a String object using the specified key.<br>
	 * You can use this method only if you are allowed (if you are on the server side or if the server has allowed you).
	 * @return <b>true</b> if the String has been successful removed, <b>false</b> either (if the key doesn't math with any String or if you are not allowed to remove it).*/
	public boolean removeString (String key){
		return removeData (key, String.class, true);
	}
	
	// synchronized for avoid ConcurrentModificationException with dataArray and keysArray
	/**<span style="font-size : 2em">DON'T USE THIS METHOD !</span><br>
	 * it can desynchronize the clients/server or corrupt the save
	 */
	public synchronized boolean removeData (String key, Class<? extends Object> type, boolean synchronizeOtherSide){
		
		if (!accessController.canAccessToData()) return false;
		
		for (int i=0 ; i<keysArray.size() ; i++){
			
			if (keysArray.get(i).equals (key) && dataArray.get(i).getClass() == type){
				
				if (type == DBFolder.class){
					
					// cleaning the folder before remove it (if the user has a pointer to this folder)
					DBFolder folder = (DBFolder) dataArray.get (i);
					folder.parentFolder = null;
					folder.setModID (null);
				}
				
				keysArray.remove (i);
				dataArray.remove (i);
				if (synchronizeOtherSide) dataModified ("remove", getTypeName (type), key, "");
				
				return true;
			}
		}
		
		return false;
	}
	
	//---------------------------------
	// NBT
	//---------------------------------
	
	protected void initFromNBT (NBTTagCompound mainTag){
		
		keysArray.clear ();
		dataArray.clear ();
		
		// BlockPos
		{
			String keys = mainTag.getString ("keysBlockPos");
			
			if (!keys.equals ("")){
				
				NBTTagCompound tagBlockPos = mainTag.getCompoundTag ("valuesBlockPos");
				String [] splitKeys = keys.split (":");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					NBTTagCompound aBlockPos = tagBlockPos.getCompoundTag (i+"");
					
					keysArray.add (splitKeys [i]);
					dataArray.add (new BlockPos (aBlockPos.getInteger ("x"), aBlockPos.getInteger ("y"), aBlockPos.getInteger ("z")));
				}
			}
		}
		
		// boolean
		{
			String keys = mainTag.getString ("keysBoolean");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagBoolean = mainTag.getCompoundTag ("valuesBoolean");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add (tagBoolean.getBoolean (i+""));
				}
			}
		}
		
		// byte
		{
			String keys = mainTag.getString ("keysByte");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagByte = mainTag.getCompoundTag ("valuesByte");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add (tagByte.getByte (i+""));
				}
			}
		}
		
		// char
		{
			String keys = mainTag.getString ("keysChar");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagChar = mainTag.getCompoundTag ("valuesChar");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add ((char) tagChar.getByte (i+""));
				}
			}
		}
		
		// double
		{
			String keys = mainTag.getString ("keysDouble");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagDouble = mainTag.getCompoundTag ("valuesDouble");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add (tagDouble.getDouble (i+""));
				}
			}
		}
		
		// float
		{
			String keys = mainTag.getString ("keysFloat");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagFloat = mainTag.getCompoundTag ("valuesFloat");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add (tagFloat.getFloat (i+""));
				}
			}
		}
		
		// DBFolder
		{
			String keys = mainTag.getString ("keysFolder");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagFolder = mainTag.getCompoundTag ("valuesFolder");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add (new DBFolder (modID, splitKeys[i], this, tagFolder.getCompoundTag (i+"")));
				}
			}
		}
		
		// int
		{
			String keys = mainTag.getString ("keysInt");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagInt = mainTag.getCompoundTag ("valuesInt");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add (tagInt.getInteger (i+""));
				}
			}
		}
		
		// long
		{
			String keys = mainTag.getString ("keysLong");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagLong = mainTag.getCompoundTag ("valuesLong");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add (tagLong.getLong (i+""));
				}
			}
		}
		
		// short
		{
			String keys = mainTag.getString ("keysShort");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagShort = mainTag.getCompoundTag ("valuesShort");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add (tagShort.getShort (i+""));
				}
			}
		}
		
		// String
		{
			String keys = mainTag.getString ("keysString");
			
			if (!keys.equals ("")){
				
				String [] splitKeys = keys.split (":");
				NBTTagCompound tagString = mainTag.getCompoundTag ("valuesString");
				
				for (int i=0 ; i<splitKeys.length ; i++){
					
					keysArray.add (splitKeys [i]);
					dataArray.add (tagString.getString (i+""));
				}
			}
		}
	}
	
	/** synchronized for avoid ConcurrentModificationException with dataArray and keysArray during the calls of getValuesByType and getKeys */
	protected synchronized void saveInNBT (NBTTagCompound mainTag){
		
		mainTag.setString ("keysBlockPos", getKeys (BlockPos.class));
		mainTag.setString ("keysBoolean", getKeys (Boolean.class));
		mainTag.setString ("keysByte", getKeys (Byte.class));
		mainTag.setString ("keysChar", getKeys (Character.class));
		mainTag.setString ("keysDouble", getKeys (Double.class));
		mainTag.setString ("keysFloat", getKeys (Float.class));
		mainTag.setString ("keysFolder", getKeys (DBFolder.class));
		mainTag.setString ("keysInt", getKeys (Integer.class));
		mainTag.setString ("keysLong", getKeys (Long.class));
		mainTag.setString ("keysShort", getKeys (Short.class));
		mainTag.setString ("keysString", getKeys (String.class));
		
		// BlockPos
		{
			NBTTagCompound tagBlockPos = new NBTTagCompound ();
			mainTag.setTag ("valuesBlockPos", tagBlockPos);
			
			ArrayList<Object> valuesBlockPos = getValuesByType (BlockPos.class);
			
			for (int i=0 ; i<valuesBlockPos.size() ; i++){
				
				NBTTagCompound coo = new NBTTagCompound ();
				tagBlockPos.setTag (""+i, coo);
				
				coo.setInteger ("x", ((BlockPos) valuesBlockPos.get (i)).getX());
				coo.setInteger ("y", ((BlockPos) valuesBlockPos.get (i)).getY());
				coo.setInteger ("z", ((BlockPos) valuesBlockPos.get (i)).getZ());
			}
		}
		
		// boolean
		{
			NBTTagCompound tagBoolean = new NBTTagCompound ();
			mainTag.setTag ("valuesBoolean", tagBoolean);
			
			ArrayList<Object> valuesBoolean = getValuesByType (Boolean.class);
			
			for (int i=0 ; i<valuesBoolean.size() ; i++){
				tagBoolean.setBoolean (i+"", ((Boolean) valuesBoolean.get (i)).booleanValue ());
			}
		}
		
		// byte
		{
			NBTTagCompound tagByte = new NBTTagCompound ();
			mainTag.setTag ("valuesByte", tagByte);
			
			ArrayList<Object> valuesByte = getValuesByType (Byte.class);
			
			for (int i=0 ; i<valuesByte.size() ; i++){
				tagByte.setByte (i+"", ((Byte) valuesByte.get (i)).byteValue ());
			}
		}
		
		// char
		{
			NBTTagCompound tagChar = new NBTTagCompound ();
			mainTag.setTag ("valuesChar", tagChar);
			
			ArrayList<Object> valuesChar = getValuesByType (Character.class);
			
			for (int i=0 ; i<valuesChar.size() ; i++){
				tagChar.setByte (i+"", (byte) ((Character) valuesChar.get (i)).charValue ());
			}
		}
		
		// double
		{
			NBTTagCompound tagDouble = new NBTTagCompound ();
			mainTag.setTag ("valuesDouble", tagDouble);
			
			ArrayList<Object> valuesDouble = getValuesByType (Double.class);
			
			for (int i=0 ; i<valuesDouble.size() ; i++){
				tagDouble.setDouble (i+"", ((Double) valuesDouble.get (i)).doubleValue ());
			}
		}
		
		// float
		{
			NBTTagCompound tagFloat = new NBTTagCompound ();
			mainTag.setTag ("valuesFloat", tagFloat);
			
			ArrayList<Object> valuesFloat = getValuesByType (Float.class);
			
			for (int i=0 ; i<valuesFloat.size() ; i++){
				tagFloat.setFloat (i+"", ((Float) valuesFloat.get (i)).floatValue ());
			}
		}
		
		// DBFolder
		{
			NBTTagCompound tagFolder = new NBTTagCompound ();
			mainTag.setTag ("valuesFolder", tagFolder);
			
			ArrayList<Object> valuesFolder = getValuesByType (DBFolder.class);
			
			for (int i=0 ; i<valuesFolder.size() ; i++){
				
				NBTTagCompound aFolder = new NBTTagCompound ();
				tagFolder.setTag (i+"", aFolder);
				
				((DBFolder) valuesFolder.get (i)).saveInNBT (aFolder);
			}
		}
		
		// int
		{
			NBTTagCompound tagInt = new NBTTagCompound ();
			mainTag.setTag ("valuesInt", tagInt);
			
			ArrayList<Object> valuesInt = getValuesByType (Integer.class);
			
			for (int i=0 ; i<valuesInt.size() ; i++){
				tagInt.setInteger (i+"", ((Integer) valuesInt.get (i)).intValue ());
			}
		}
		
		// long
		{
			NBTTagCompound tagLong = new NBTTagCompound ();
			mainTag.setTag ("valuesLong", tagLong);
			
			ArrayList<Object> valuesLong = getValuesByType (Long.class);
			
			for (int i=0 ; i<valuesLong.size() ; i++){
				tagLong.setLong (i+"", ((Long) valuesLong.get (i)).longValue ());
			}
		}
		
		// short
		{
			NBTTagCompound tagShort = new NBTTagCompound ();
			mainTag.setTag ("valuesShort", tagShort);
			
			ArrayList<Object> valuesShort = getValuesByType (Short.class);
			
			for (int i=0 ; i<valuesShort.size() ; i++){
				tagShort.setShort (i+"", ((Short) valuesShort.get (i)).shortValue ());
			}
		}
		
		// String
		{
			NBTTagCompound tagString = new NBTTagCompound ();
			mainTag.setTag ("valuesString", tagString);
			
			ArrayList<Object> valuesString = getValuesByType (String.class);
			
			for (int i=0 ; i<valuesString.size() ; i++){
				tagString.setString (i+"", (String) valuesString.get (i));
			}
		}
	}
	
	private ArrayList<Object> getValuesByType (Class<? extends Object> type){
		
		ArrayList<Object> specificData = new ArrayList<Object>();
		
		for (Object data : dataArray){
			if (data.getClass() == type){
				specificData.add (data);
			}
		}
		
		return specificData;
	}
	
	private String getKeys (Class<? extends Object> type){
		
		String keys = "";
		
		for (int i=0 ; i<dataArray.size() ; i++){
			
			if (dataArray.get(i).getClass() == type){
				
				if (keys.equals ("")) keys = keysArray.get(i);
				else keys += ':' + keysArray.get(i);
			}
		}
		
		return keys;
	}
}