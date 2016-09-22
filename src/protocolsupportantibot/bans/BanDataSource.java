package protocolsupportantibot.bans;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import protocolsupportantibot.Settings;

public class BanDataSource {

	private static final BanDataSource instance = new BanDataSource();

	public static BanDataSource getInstance() {
		return instance;
	}

	private final Cache<InetAddress, Boolean> bans = CacheBuilder.newBuilder()
	.expireAfterWrite(Settings.tempBanTime, TimeUnit.SECONDS)
	.build();


	public boolean isBanned(InetAddress addr) {
		return bans.getIfPresent(addr) != null;
	}

	public void ban(InetAddress addr) {
		bans.put(addr, Boolean.TRUE);
	}

}
