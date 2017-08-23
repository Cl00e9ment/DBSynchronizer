package com.trcgames.dbSynchronizer.packets;

import com.trcgames.dbSynchronizer.DBSynchronizer;
import com.trcgames.dbSynchronizer.DatabaseGetter;
import com.trcgames.dbSynchronizer.database.ClientDatabase;
import com.trcgames.dbSynchronizer.database.DBFolder;
import com.trcgames.dbSynchronizer.database.Database;
import com.trcgames.dbSynchronizer.packets.PacketClientToServer.CtSPacketType;
import com.trcgames.dbSynchronizer.database.dbFolderAccessControl.ClientAccessController;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketServerToClient implements IMessage{
	
	public enum StCPacketType {ADD_MOD_IDS, PLAYER_LOGGED_IN, SET_REMOVE_DATA, SET_ACCESS_PERMISSION};
	
	private StCPacketType type;
	private String clientToIgnor = "";
	private String [] args;
	
	public PacketServerToClient (){}
	
	public PacketServerToClient (StCPacketType type, String... args){
		
		this.type = type;
		this.args = args;
	}
	
	public PacketServerToClient (StCPacketType type, EntityPlayerMP clientToIgnor, String... args){
		
		this.type = type;
		this.clientToIgnor = clientToIgnor.getName ();
		this.args = args;
	}
	
	@Override
	public void fromBytes (ByteBuf buf){
		
		type = StCPacketType.values() [buf.readInt()];
		clientToIgnor = ByteBufUtils.readUTF8String (buf);
		args = new String [buf.readInt()];
		for (int i=0 ; i<args.length ; i++) args[i] = ByteBufUtils.readUTF8String (buf);
	}
	
	@Override
	public void toBytes (ByteBuf buf){
		
		buf.writeInt (type.ordinal());
		ByteBufUtils.writeUTF8String (buf, clientToIgnor);
		buf.writeInt (args.length);
		for (String arg : args) ByteBufUtils.writeUTF8String (buf, arg);
	}
	
	public static class Handler implements IMessageHandler <PacketServerToClient, IMessage>{
		
		@Override
		@SideOnly (Side.CLIENT)
		public IMessage onMessage (PacketServerToClient message, MessageContext ctx){
			
			if (Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().player.getName().equals (message.clientToIgnor)){
				return null;
			}
			
			switch (message.type){
				
				case ADD_MOD_IDS :
					
					for (String modID : message.args){
						ClientDatabase.addInstance (modID);
					}
					
					return null;
					
				case PLAYER_LOGGED_IN :
					
					if (Minecraft.getMinecraft().player != null){
						
						if (Minecraft.getMinecraft().player.getName().equals (message.args [0])){
							
							DBSynchronizer.worldLoaded = true;
							DBSynchronizer.network.sendToServer (new PacketClientToServer (CtSPacketType.INITIALIZATION_REQUEST));
						}
						
						return null;
					}
					
					//When Client thread will receive the task, Minecraft.getMinecraft.player will be initialized.
					//But now, it's not.
					Minecraft.getMinecraft().addScheduledTask (new Runnable(){
						
						private String playerName;
						
						public Runnable setPlayerName (String playerName){
							
							this.playerName = playerName;
							return this;
						}
						
						@Override
						public void run(){
							
							if (Minecraft.getMinecraft().player.getName().equals (playerName)){
								DBSynchronizer.network.sendToServer (new PacketClientToServer (CtSPacketType.INITIALIZATION_REQUEST));
							}
						}
					}.setPlayerName (message.args [0]));
					
					return null;
					
				case SET_REMOVE_DATA :
					
					PacketCommonHandler.setRemoveData (message.args, "server");
					return null;
					
				case SET_ACCESS_PERMISSION :
					
					String [] args = message.args;
					Database database = DatabaseGetter.getInstance (args [0]);
					DBFolder folder;
					
					if (args [1].equals ("persistent folder")) folder = database.getPersistentFolder();
					else if (args [1].equals ("non-persistent folder")) folder = database.getNonPersistentFolder();
					else return null;
					
					for (int i=2 ; i<args.length-1 ; i++){
						folder = folder.getDBFolder (args [i]);
					}
					
					ClientAccessController accessController = (ClientAccessController) folder.getAccessController();
					
					if (args [args.length-1].equals ("allow")) accessController.setAccessAuthorization (true);
					else if (args [args.length-1].equals ("forbid")) accessController.setAccessAuthorization (false);
					
					return null;
					
				default : return null;
			}
		}
	}
}