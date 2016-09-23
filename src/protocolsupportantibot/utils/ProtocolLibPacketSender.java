package protocolsupportantibot.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.entity.Player;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.PacketFilterManager;
import com.comphenix.protocol.injector.player.PlayerInjectionHandler;

public class ProtocolLibPacketSender {

	private static PlayerInjectionHandler playerInjector;

	public static void init() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		Field field = PacketFilterManager.class.getDeclaredField("playerInjection");
		field.setAccessible(true);
		playerInjector = (PlayerInjectionHandler) field.get(ProtocolLibrary.getProtocolManager());
	}

	public static void sendServerPacket(Player player, PacketContainer packet) throws InvocationTargetException {
		playerInjector.sendServerPacket(player, packet, null, false);
	}

}
