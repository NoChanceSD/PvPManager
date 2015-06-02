package me.NoChance.PvPManager.Managers;

import java.util.HashMap;
import java.util.UUID;

import me.NoChance.PvPManager.PvPManager;
import me.NoChance.PvPManager.PvPlayer;
import me.NoChance.PvPManager.TeamProfile;
import me.NoChance.PvPManager.Config.Variables;
import me.NoChance.PvPManager.Tasks.CleanKillersTask;
import me.NoChance.PvPManager.Tasks.TagTask;
import me.NoChance.PvPManager.Utils.CancelResult;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;

public class PlayerHandler {

	private final HashMap<String, PvPlayer> players = new HashMap<>();
	private final ConfigManager configManager;
	private final DependencyManager dependencyManager;
	private final PvPManager plugin;
	private final TagTask tagTask = new TagTask();

	public PlayerHandler(final PvPManager plugin) {
		this.plugin = plugin;
		this.configManager = plugin.getConfigM();
		this.dependencyManager = plugin.getDependencyManager();
		if (Variables.isKillAbuseEnabled())
			new CleanKillersTask(this).runTaskTimer(plugin, 1200, Variables.getKillAbuseTime() * 20);

		addOnlinePlayers();
		tagTask.runTaskTimerAsynchronously(plugin, 20, 20);
	}

	public final CancelResult tryCancel(final Player damager, final Player defender) {
		final PvPlayer attacker = get(damager);
		final PvPlayer attacked = get(defender);
		if (attacker.hasOverride() || dependencyManager.hasWGFlag(defender) || Variables.isStopBorderHopping() && canAttack(attacker, attacked))
			return CancelResult.FAIL_OVERRIDE;
		if (attacked.hasRespawnProtection() || attacker.hasRespawnProtection())
			return CancelResult.RESPAWN_PROTECTION;
		if (attacked.isNewbie())
			return CancelResult.NEWBIE_OTHER;
		if (attacker.isNewbie())
			return CancelResult.NEWBIE;
		if (!attacked.hasPvPEnabled())
			return CancelResult.PVPDISABLED_OTHER;
		if (!attacker.hasPvPEnabled())
			return CancelResult.PVPDISABLED;

		return CancelResult.FAIL;
	}

	private boolean canAttack(final PvPlayer attacker, final PvPlayer defender) {
		if (attacker.isInCombat() && defender.isInCombat())
			return dependencyManager.canAttack(attacker.getPlayer(), defender.getPlayer());
		return false;
	}

	private void addOnlinePlayers() {
		for (final Player p : plugin.getServer().getOnlinePlayers()) {
			add(p);
		}
	}

	public final PvPlayer get(final Player player) {
		final String name = player.getName();
		return players.containsKey(name) ? players.get(name) : add(player);
	}

	private PvPlayer add(final Player player) {
		if (plugin.getServer().getPlayer(player.getUniqueId()) == null)
			return null;
		final PvPlayer pvPlayer = new PvPlayer(player, plugin);
		players.put(player.getName(), pvPlayer);
		if ((Variables.isUseNameTag() || Variables.isToggleNametagsEnabled()) && players.size() == 1)
			TeamProfile.setupTeams();
		pvPlayer.loadPvPState();
		return pvPlayer;
	}

	public final void untag(final PvPlayer p) {
		tagTask.getTagged().remove(p);
		p.unTag();
	}

	public final void tag(final PvPlayer p) {
		tagTask.getTagged().add(p);
	}

	public final void remove(final PvPlayer player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (player.getPlayer() == null) {
					players.remove(player.getName());
				}
			}
		}.runTaskLater(plugin, Variables.getToggleCooldown() * 20);
		if (player.hasPvPLogged())
			player.setPvpLogged(false);
		savePvPState(player.getUUID(), player.hasPvPEnabled());
	}

	public final void removeTeams() {
		final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		if (scoreboard.getTeam("InCombat") != null)
			scoreboard.getTeam("InCombat").unregister();

		if (scoreboard.getTeam("PvPOn") != null)
			scoreboard.getTeam("PvPOn").unregister();

		if (scoreboard.getTeam("PvPOff") != null)
			scoreboard.getTeam("PvPOff").unregister();
	}

	public final void savePvPState(final UUID id, final boolean pvpState) {
		configManager.saveUser(id, !pvpState);
	}

	public final void applyPunishments(final PvPlayer player) {
		Player p = player.getPlayer();
		if (Variables.isKillOnLogout()) {
			player.setPvpLogged(true);
			p.setHealth(0);
		}
		if (Variables.isFineEnabled())
			player.applyFine();
	}

	public final HashMap<String, PvPlayer> getPlayers() {
		return players;
	}

	public final TagTask getTagTask() {
		return tagTask;
	}

}
