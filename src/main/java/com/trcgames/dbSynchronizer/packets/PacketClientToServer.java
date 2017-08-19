package com.trcgames.dbSynchronizer.packets;

import com.trcgames.dbSynchronizer.DBSynchronizer;
import com.trcgames.dbSynchronizer.database.DBFolder;
import com.trcgames.dbSynchronizer.database.DataSaver;
import com.trcgames.dbSynchronizer.database.ServerDatabase;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient.StCPacketType;
import com.trcgames.dbSynchronizer.util.BlockPos;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketClientToServer implements IMessage{

	public enum CtSPacketType {INITIALIZATION_REQUEST, ADD_MOD_ID, SET_REMOVE_DATA};
	
	private CtSPacketType type;
	private String [] args;
	
	public PacketClientToServer (){}
	
	public PacketClientToServer (CtSPacketType type, String... args){
		
		this.type = type;
		this.args = args;
	}
	
	@Override
	public void fromBytes (ByteBuf buf){
		
		type = CtSPacketType.values() [buf.readInt()];
		args = new String [buf.readInt()];
		for (int i=0 ; i<args.length ; i++) args[i] = ByteBufUtils.readUTF8String (buf);
	}
	
	@Override
	public void toBytes (ByteBuf buf){
		
		buf.writeInt (type.ordinal());
		buf.writeInt (args.length);
		for (String arg : args) ByteBufUtils.writeUTF8String (buf, arg);
	}
	
	public static class Handler implements IMessageHandler <PacketClientToServer, IMessage>{

		@Override
		public IMessage onMessage (PacketClientToServer message, MessageContext ctx){
			
			switch (message.type){
				
				case INITIALIZATION_REQUEST :
					
					EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
					DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.ADD_MOD_IDS, DataSaver.getInstance().getModIDs()), sender);
					
					for (String modID : DataSaver.getInstance().getModIDs()){
						
						initializeClient (sender, modID, ServerDatabase.getInstance (modID).getPersistentFolder());
						initializeClient (sender, modID, ServerDatabase.getInstance (modID).getNonPersistentFolder());
					}
					
					return null;
					
				case ADD_MOD_ID :
					
					DataSaver.getInstance().addAModID (message.args [0]);
					DBSynchronizer.network.sendToAll (new PacketServerToClient (StCPacketType.ADD_MOD_IDS, ctx.getServerHandler().playerEntity, message.args[0]));
					return null;
					
				case SET_REMOVE_DATA :
					
					PacketCommonHandler.setRemoveData (message.args);
					ServerDatabase.getInstance (message.args[0]).markDirty ();
					
					DBSynchronizer.network.sendToAll (new PacketServerToClient (StCPacketType.SET_REMOVE_DATA, ctx.getServerHandler().playerEntity, message.args));
					return null;
					
				default : return null;
			}
		}
		
		private void initializeClient (EntityPlayerMP sender, String modID, DBFolder folder){
			
			String [] keys = folder.getKeys ();
			
			for (int i=0 ; i<keys.length ; i++){
				
				String [] hierarchy = folder.getHierarchy ();
				String key = keys [i];
				String dataType = folder.getDataType (i);
				String value = null;
				
				if (dataType.equals ("BlockPos")){
					
					BlockPos pos = folder.getBlockPos (key);
					value = pos.getX() +":"+ pos.getY() +":"+ pos.getZ();
					
				}else if (dataType.equals ("boolean")) value = ""+folder.getBoolean (key);
				else if (dataType.equals ("byte")) value = ""+folder.getByte (key);
				else if (dataType.equals ("char")) value = ""+folder.getChar (key);
				else if (dataType.equals ("double")) value = ""+folder.getDouble (key);
				else if (dataType.equals ("float")) value = ""+folder.getFloat (key);
				else if (dataType.equals ("DBFolder")) value = "";
				else if (dataType.equals ("int")) value = ""+folder.getInt (key);
				else if (dataType.equals ("long")) value = ""+folder.getLong (key);
				else if (dataType.equals ("short")) value = ""+folder.getShort (key);
				else if (dataType.equals ("String")) value = folder.getString (key);
				
				String [] args = new String [hierarchy.length+5];
				
				args [0] = modID;
				for (int j=0 ; j<hierarchy.length ; j++) args [j+1] = hierarchy [j];
				args [args.length-4] = "set";
				args [args.length-3] = dataType;
				args [args.length-2] = key;
				args [args.length-1] = value;
				
				DBSynchronizer.network.sendTo (new PacketServerToClient (StCPacketType.SET_REMOVE_DATA, args), sender);
				
				if (dataType.equals ("DBFolder")) initializeClient (sender, modID, folder.getDBFolder(key));
			}
		}
	}
}