package com.trcgames.dbSynchronizer.packets;

import com.trcgames.dbSynchronizer.DBSynchronizer;
import com.trcgames.dbSynchronizer.database.DBFolder;
import com.trcgames.dbSynchronizer.database.ServerDatabase;
import com.trcgames.dbSynchronizer.packets.PacketServerToClient.StCPacketType;
import com.trcgames.dbSynchronizer.database.dbFolderAccessControl.ServerAccessController;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketClientToServer implements IMessage{

	public enum CtSPacketType {INITIALIZATION_REQUEST, SET_REMOVE_DATA};
	
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
					
					EntityPlayerMP sender = ctx.getServerHandler().player;
					DBSynchronizer.instance.getNetwork().sendTo (new PacketServerToClient (StCPacketType.ADD_MOD_IDS, ServerDatabase.getModIDs()), sender);
					
					for (String modID : ServerDatabase.getModIDs()){
						
						initializeClient (sender, modID, ServerDatabase.getInstance (modID).getPersistentFolder());
						initializeClient (sender, modID, ServerDatabase.getInstance (modID).getNonPersistentFolder());
					}
					
					return null;
					
				case SET_REMOVE_DATA :
					
					boolean dataSuccessfullyModified = PacketCommonHandler.setRemoveData (message.args, ctx.getServerHandler().player.getName());
					
					if (dataSuccessfullyModified){
						
						ServerDatabase.getInstance (message.args[0]).markDirty ();
						DBSynchronizer.instance.getNetwork().sendToAll (new PacketServerToClient (StCPacketType.SET_REMOVE_DATA, ctx.getServerHandler().player, message.args));
					}
					
					return null;
					
				default : return null;
			}
		}
		
		private void initializeClient (EntityPlayerMP sender, String modID, DBFolder folder){
			
			String [] hierarchy = folder.getHierarchy ();
			
			if (((ServerAccessController) folder.getAccessController()).canPlayerAccessToData (sender.getName())){
				
				String [] args = new String [hierarchy.length+2];
				args [0] = modID;
				for (int j=0 ; j<hierarchy.length ; j++) args [j+1] = hierarchy [j];
				args [args.length-1] = "allow";
				
				DBSynchronizer.instance.getNetwork().sendTo (new PacketServerToClient (StCPacketType.SET_ACCESS_PERMISSION, args), sender);
			}
			
			String [] keys = folder.getKeys ();
			
			for (int i=0 ; i<keys.length ; i++){
				
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
				
				DBSynchronizer.instance.getNetwork().sendTo (new PacketServerToClient (StCPacketType.SET_REMOVE_DATA, args), sender);
				
				if (dataType.equals ("DBFolder")) initializeClient (sender, modID, folder.getDBFolder(key));
			}
		}
	}
}