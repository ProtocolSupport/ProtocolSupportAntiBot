package protocolsupportantibot.fakespawn;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.comphenix.protocol.events.PacketContainer;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.DataException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.ChunkSection;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.PacketDataSerializer;
import net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk;
import protocolsupportantibot.ProtocolSupportAntiBot;

@SuppressWarnings("deprecation")
public class LobbySchematic {

	public static final String name = "lobby.schematic";

	static PacketContainer chunkdata;

	public static void load() {
		try {
			Chunk chunk = new Chunk(null, 0, 0);
			CuboidClipboard clipboard = CuboidClipboard.loadSchematic(new File(ProtocolSupportAntiBot.getInstance().getDataFolder(), name));
			for (int x = 0; x < clipboard.getLength(); x++) {
				for (int z = 0; z < clipboard.getWidth(); z++) {
					for (int y = 0; y < clipboard.getHeight(); y++) {
						BaseBlock block = clipboard.getBlock(new Vector(x, y, z));
						ChunkSection[] sections = chunk.getSections();
						int ysect = y >> 4;
						ChunkSection section = sections[ysect];
						if (section == null) {
							section = new ChunkSection(ysect, true);
							sections[ysect] = section; 
						}
						IBlockData iblockdata = Block.getById(block.getId()).fromLegacyData(block.getData());
						section.setType(x, y & 0xF, z, iblockdata);
						section.a(x, y & 0xF, z, 15);
						section.b(x, y & 0xF, z, 15);
					}
				}
			}
			PacketPlayOutMapChunk mapchunk = new PacketPlayOutMapChunk();
			chunkdata = PacketContainer.fromPacket(mapchunk);
			chunkdata.getBooleans().write(0, true);
			ByteBuf buffer = Unpooled.buffer();
			chunkdata.getIntegers().write(2, mapchunk.a(new PacketDataSerializer(buffer), chunk, true, 65535));
			byte[] bufferdata = new byte[buffer.readableBytes()];
			buffer.readBytes(bufferdata);
			chunkdata.getByteArrays().write(0, bufferdata);
			chunkdata.getSpecificModifier(List.class).write(0, Collections.emptyList());
		} catch (DataException | IOException e) {
		}
	}

}
