package protocolsupportantibot.protocolvalidator;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import org.bukkit.craftbukkit.v1_10_R1.util.Waitable;

import net.minecraft.server.v1_10_R1.Entity;
import net.minecraft.server.v1_10_R1.MinecraftServer;
import net.minecraft.server.v1_10_R1.NBTTagCompound;

public class EntityIdPool {

	private final ConcurrentLinkedQueue<Integer> idPool = new ConcurrentLinkedQueue<>();

	public void claimPool() {
		int count = 1000;
		while (count-- > 0) {
			idPool.add(new FakeEntity().getId());
		}
	}

	@SuppressWarnings("deprecation")
	public int getId() {
		Integer value = idPool.poll();
		if (value != null) {
			return value;
		} else {
			Waitable<Integer> waitable = new Waitable<Integer>() {
				@Override
				protected Integer evaluate() {
					return new FakeEntity().getId();
				}
			};
			MinecraftServer.getServer().processQueue.add(waitable);
			try {
				return waitable.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException("Internal error: unable to generate entity id", e);
			}
		}
	}

	public void returnId(int id) {
		idPool.add(id);
	}

	private static class FakeEntity extends Entity {
		public FakeEntity() {
			super(null);
		}
		@Override
		protected void i() {
		}
		@Override
		protected void a(NBTTagCompound p0) {	
		}
		@Override
		protected void b(NBTTagCompound p0) {	
		}
	};

}
