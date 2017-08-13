package net.redstoneore.legacyfactions.cmd;

import net.redstoneore.legacyfactions.*;
import net.redstoneore.legacyfactions.entity.Board;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;
import net.redstoneore.legacyfactions.entity.Faction;


public class CmdFactionsOwner extends FCommand {
	
	// -------------------------------------------------- //
	// INSTANCE
	// -------------------------------------------------- //
	
	private static CmdFactionsOwner instance = new CmdFactionsOwner();
	public static CmdFactionsOwner get() { return instance; }
	
	// -------------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------------- //

	private CmdFactionsOwner() {
		this.aliases.addAll(Conf.cmdAliasesOwner);

		this.optionalArgs.put("player name", "you");

		this.permission = Permission.OWNER.getNode();
		this.disableOnLock = true;

		this.senderMustBePlayer = true;
		this.senderMustBeMember = false;
		this.senderMustBeModerator = false;
		this.senderMustBeColeader = false;
		this.senderMustBeAdmin = false;
	}

	// TODO: Fix colors!

	// -------------------------------------------------- //
	// METHODS
	// -------------------------------------------------- //

	@Override
	public void perform() {
		boolean hasBypass = fme.isAdminBypassing();
		
		if (!hasBypass && !assertHasFaction()) {
			return;
		}

		if (!Conf.ownedAreasEnabled) {
			this.sendMessage(Lang.COMMAND_OWNER_DISABLED);
			return;
		}

		if (!hasBypass && Conf.ownedAreasLimitPerFaction > 0 && myFaction.getCountOfClaimsWithOwners() >= Conf.ownedAreasLimitPerFaction) {
			this.fme.sendMessage(Lang.COMMAND_OWNER_LIMIT, Conf.ownedAreasLimitPerFaction);
			return;
		}

		if (!hasBypass && !assertMinRole(Conf.ownedAreasModeratorsCanSet ? Role.MODERATOR : Role.COLEADER)) {
			return;
		}

		FLocation flocation = new FLocation(fme);

		Faction factionHere = Board.get().getFactionAt(flocation);
		if (factionHere != myFaction) {
			if (!factionHere.isNormal()) {
				fme.sendMessage(Lang.COMMAND_OWNER_NOTCLAIMED);
				return;
			}

			if (!hasBypass) {
				fme.sendMessage(Lang.COMMAND_OWNER_WRONGFACTION);
				return;
			}

		}

		FPlayer target = this.argAsBestFPlayerMatch(0, this.fme);
		if (target == null) return;

		String playerName = target.getName();

		if (target.getFaction() != myFaction) {
			fme.sendMessage(Lang.COMMAND_OWNER_NOTMEMBER, playerName);
			return;
		}

		// if no player name was passed, and this claim does already have owners set, clear them
		if (args.isEmpty() && myFaction.doesLocationHaveOwnersSet(flocation)) {
			myFaction.clearClaimOwnership(flocation);
			fme.sendMessage(Lang.COMMAND_OWNER_CLEARED);
			return;
		}

		if (myFaction.isPlayerInOwnerList(target, flocation)) {
			myFaction.removePlayerAsOwner(target, flocation);
			fme.sendMessage(Lang.COMMAND_OWNER_REMOVED, playerName);
			return;
		}

		// if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
		if (!payForCommand(Conf.econCostOwner, Lang.COMMAND_OWNER_TOSET, Lang.COMMAND_OWNER_FORSET)) {
			return;
		}

		myFaction.setPlayerAsOwner(target, flocation);

		fme.sendMessage(Lang.COMMAND_OWNER_ADDED, playerName);
	}

	
	@Override
	public String getUsageTranslation() {
		return Lang.COMMAND_OWNER_DESCRIPTION.toString();
	}
}
