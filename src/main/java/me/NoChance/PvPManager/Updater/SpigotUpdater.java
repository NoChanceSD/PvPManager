package me.NoChance.PvPManager.Updater;

import me.NoChance.PvPManager.Utils.Log;

import org.bukkit.plugin.Plugin;

import com.google.common.io.Files;
import com.jaunt.Element;
import com.jaunt.UserAgent;
import com.jaunt.util.HandlerForBinary;

public class SpigotUpdater extends Updater {

	private UserAgent user;
	private String versionName;

	public SpigotUpdater(final Plugin plugin, final UpdateType type) {
		super(plugin, type);
		this.thread.start();
	}

	@Override
	public void runUpdater() {
		user = new UserAgent();
		try {
			user.visit("http://www.spigotmc.org/resources/pvpmanager.845/history");
			Element versionEntry = user.doc.findFirst("<td class=version>");
			if (versionCheck(versionEntry.innerHTML())) {
				result = UpdateResult.UPDATE_AVAILABLE;
				if (type == UpdateType.VERSION_CHECK)
					return;
				downloadFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean downloadFile() {
		try {
			Element download = user.doc.findFirst("<td class=dataOptions.download>");
			HandlerForBinary handlerForBinary = new HandlerForBinary();
			user.setHandler("application/octet-stream", handlerForBinary);
			user.visit(download.getElement(0).getAt("href"));
			Files.write(handlerForBinary.getContent(), file);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	protected boolean versionCheck(final String title) {
		final String version = this.plugin.getDescription().getVersion();
		this.versionName = title;
		if (title.matches("^\\d.*")) {
			String[] remote = getVersionArray(title);
			String[] local = getVersionArray(version);
			final int length = Math.max(local.length, remote.length);
			try {
				for (int i = 0; i < length; i++) {
					final int localNumber = i < local.length ? Integer.parseInt(local[i]) : 0;
					final int remoteNumber = i < remote.length ? Integer.parseInt(remote[i]) : 0;
					if (remoteNumber > localNumber)
						return true;
					if (remoteNumber < localNumber || title.equalsIgnoreCase(version)) {
						this.result = UpdateResult.NO_UPDATE;
						return false;
					}
				}
			} catch (NumberFormatException ex) {
				Log.warning("Error reading version number!");
			}
		} else {
			this.result = UpdateResult.FAIL_NOVERSION;
			return false;
		}
		return true;
	}

	@Override
	public String getLatestName() {
		this.waitForThread();
		return versionName;
	}

}
