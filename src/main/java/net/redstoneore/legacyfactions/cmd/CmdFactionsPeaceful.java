package net.redstoneore.legacyfactions.cmd;

import net.redstoneore.legacyfactions.Permission;
import net.redstoneore.legacyfactions.Lang;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;
import net.redstoneore.legacyfactions.entity.FPlayerColl;
import net.redstoneore.legacyfactions.entity.Faction;

public class CmdFactionsPeaceful extends FCommand {

	// -------------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------------- //

	public CmdFactionsPeaceful() {
		this.aliases.addAll(Conf.cmdAliasesPeaceful);

		this.requiredArgs.add("faction tag");
		
		this.permission = Permission.SET_PEACEFUL.getNode();
		this.disableOnLock = true;

		this.senderMustBePlayer = false;
		this.senderMustBeMember = false;
		this.senderMustBeModerator = false;
		this.senderMustBeColeader = false;
		this.senderMustBeAdmin = false;
	}

	// -------------------------------------------------- //
	// METHODS
	// -------------------------------------------------- //
	@Override
	public void perform() {
		Faction faction = this.argAsFaction(0);
		if (faction == null) {
			return;
		}

		String change;
		if (faction.isPeaceful()) {
			change = Lang.COMMAND_PEACEFUL_REVOKE.toString();
			faction.setPeaceful(false);
		} else {
			change = Lang.COMMAND_PEACEFUL_GRANT.toString();
			faction.setPeaceful(true);
		}

		// Inform all players
		for (FPlayer fplayer : FPlayerColl.all(true)) {
			String blame = (fme == null ? Lang.GENERIC_SERVERADMIN.toString() : fme.describeTo(fplayer, true));
			if (fplayer.getFaction() == faction) {
				fplayer.msg(Lang.COMMAND_PEACEFUL_YOURS, blame, change);
			} else {
				fplayer.msg(Lang.COMMAND_PEACEFUL_OTHER, blame, change, faction.getTag(fplayer));
			}
		}

	}

	@Override
	public String getUsageTranslation() {
		return Lang.COMMAND_PEACEFUL_DESCRIPTION.toString();
	}

}
