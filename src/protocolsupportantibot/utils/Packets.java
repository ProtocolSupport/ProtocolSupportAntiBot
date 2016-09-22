package protocolsupportantibot.utils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.WorldType;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.Difficulty;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;

import net.minecraft.server.v1_10_R1.MapIcon;

public class Packets {

	public static PacketContainer createRespawnPacket(int dimId, Difficulty difficulty, WorldType worldType, NativeGameMode gamemode) {
		PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.RESPAWN);
		container.getIntegers().write(0, dimId);
		container.getDifficulties().write(0, difficulty);
		container.getWorldTypeModifier().write(0, worldType);
		container.getGameModes().write(0, gamemode);
		return container;
	}

	public static PacketContainer createJoinGamePacket(int playerId, int maxPlayers) {
		PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.LOGIN);
		container.getIntegers().write(0, playerId);
		container.getIntegers().write(2, maxPlayers);
		container.getGameModes().write(0, NativeGameMode.SURVIVAL);
		container.getWorldTypeModifier().write(0, WorldType.NORMAL);
		container.getDifficulties().write(0, Difficulty.EASY);
		return container;
	}

	public static PacketContainer createTransactionPacket() {
		PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.TRANSACTION);
		container.getShorts().write(0, (short) 1337);
		return container;
	}

	public static PacketContainer createEmptyChunkPacket() {
		PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.MAP_CHUNK);
		container.getSpecificModifier(List.class).write(0, Collections.emptyList());
		container.getBooleans().write(0, true);
		container.getByteArrays().write(0, new byte[256]);
		return container;
	}

	public static PacketContainer createTeleportPacket(double x, double y, double z, float yaw, float pitch) {
		PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.POSITION);
		container.getDoubles().write(0, x);
		container.getDoubles().write(1, y);
		container.getDoubles().write(2, z);
		container.getFloat().write(0, yaw);
		container.getFloat().write(1, pitch);
		container.getIntegers().write(0, 1337);
		container.getSpecificModifier(Set.class).write(0, Collections.emptySet());
		return container;
	}

	public static PacketContainer createSetSlotPacket(int slot, ItemStack itemstack) {
		PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SET_SLOT);
		container.getIntegers().write(1, slot);
		container.getItemModifier().write(0, itemstack);
		return container;
	}

	public static PacketContainer createMapDataPacket(int subId, byte[] mapdata) {
		PacketContainer container = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.MAP);
		container.getIntegers().write(0, subId);
		container.getBytes().write(0, (byte) 4);
		container.getIntegers().write(3, 128);
		container.getIntegers().write(4, 128);
		container.getByteArrays().write(0, mapdata);
		container.getSpecificModifier(MapIcon[].class).write(0, new MapIcon[0]);
		return container;
	}

}
