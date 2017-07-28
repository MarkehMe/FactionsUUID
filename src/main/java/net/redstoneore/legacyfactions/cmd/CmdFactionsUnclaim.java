package net.redstoneore.legacyfactions.cmd;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

import net.redstoneore.legacyfactions.*;
import net.redstoneore.legacyfactions.entity.Board;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.Faction;
import net.redstoneore.legacyfactions.event.EventFactionsLandChange;
import net.redstoneore.legacyfactions.event.EventFactionsLandChange.LandChangeCause;
import net.redstoneore.legacyfactions.integration.vault.VaultEngine;
import net.redstoneore.legacyfactions.task.SpiralTask;

public class CmdFactionsUnclaim extends FCommand {

	// -------------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------------- //

	public CmdFactionsUnclaim() {
		this.aliases.addAll(Conf.cmdAliasesUnclaim);

		this.optionalArgs.put("radius", "1");
		this.optionalArgs.put("faction", "your");

		this.permission = Permission.UNCLAIM.getNode();
		this.disableOnLock = true;

		this.senderMustBePlayer = true;
		this.senderMustBeMember = false;
		this.senderMustBeModerator = false;
		this.senderMustBeAdmin = false;
	}

	// -------------------------------------------------- //
	// METHODS
	// -------------------------------------------------- //

	@Override
	public void perform() {
		// Read and validate input
		int radius = this.argAsInt(0, 1); // Default to 1
		
		if (radius < 1) {
			sendMessage(Lang.COMMAND_CLAIM_INVALIDRADIUS);
			return;
		}

		if (radius < 2) {
			// single chunk
			unClaim(new FLocation(me));
		} else {
			// radius claim
			if (!Permission.CLAIM_RADIUS.has(sender, false)) {
				sendMessage(Lang.COMMAND_CLAIM_DENIED);
				return;
			}

			new SpiralTask(new FLocation(me), radius) {
				private int failCount = 0;
				private final int limit = Conf.radiusClaimFailureLimit - 1;

				@Override
				public boolean work() {
					boolean success = unClaim(this.currentFLocation());
					if (success) {
						failCount = 0;
					} else if (failCount++ >= limit) {
						this.stop();
						return false;
					}

					return true;
				}
			};
		}
	}

	private boolean unClaim(FLocation target) {
		Faction targetFaction = Board.get().getFactionAt(target);
		if (targetFaction.isSafeZone()) {
			if (Permission.MANAGE_SAFE_ZONE.has(sender)) {
				Board.get().removeAt(target);
				sendMessage(Lang.COMMAND_UNCLAIM_SAFEZONE_SUCCESS);

				if (Conf.logLandUnclaims) {
					Factions.get().log(Lang.COMMAND_UNCLAIM_LOG.format(fme.getName(), target.getCoordString(), targetFaction.getTag()));
				}
				return true;
			} else {
				sendMessage(Lang.COMMAND_UNCLAIM_SAFEZONE_NOPERM);
				return false;
			}
		} else if (targetFaction.isWarZone()) {
			if (Permission.MANAGE_WAR_ZONE.has(sender)) {
				Board.get().removeAt(target);
				sendMessage(Lang.COMMAND_UNCLAIM_WARZONE_SUCCESS);

				if (Conf.logLandUnclaims) {
					Factions.get().log(Lang.COMMAND_UNCLAIM_LOG.format(fme.getName(), target.getCoordString(), targetFaction.getTag()));
				}
				return true;
			} else {
				sendMessage(Lang.COMMAND_UNCLAIM_WARZONE_NOPERM);
				return false;
			}
		}

		if (fme.isAdminBypassing()) {
			Map<FLocation, Faction> transactions = new HashMap<FLocation, Faction>();
			
			transactions.put(target, targetFaction);

			EventFactionsLandChange event = new EventFactionsLandChange(fme, transactions, LandChangeCause.Unclaim);
			
			if (event.isCancelled()) return false;
			
			for (FLocation location : event.getTransactions().keySet()) {
				Board.get().removeAt(location);
			}
			
			targetFaction.sendMessage(Lang.COMMAND_UNCLAIM_UNCLAIMED, fme.describeTo(targetFaction, true));
			sendMessage(Lang.COMMAND_UNCLAIM_UNCLAIMS);

			if (Conf.logLandUnclaims) {
				Factions.get().log(Lang.COMMAND_UNCLAIM_LOG.format(fme.getName(), target.getCoordString(), targetFaction.getTag()));
			}

			return true;
		}

		if (!assertHasFaction()) {
			return false;
		}

		if (!assertMinRole(Role.MODERATOR)) {
			return false;
		}


		if (myFaction != targetFaction) {
			sendMessage(Lang.COMMAND_UNCLAIM_WRONGFACTION);
			return false;
		}
		
		Map<FLocation, Faction> transactions = new HashMap<FLocation, Faction>();

		transactions.put(FLocation.valueOf(me.getLocation()), targetFaction);
		
		EventFactionsLandChange event = new EventFactionsLandChange(fme, transactions, LandChangeCause.Unclaim);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) return false;

		if (VaultEngine.getUtils().shouldBeUsed()) {
			double refund = VaultEngine.getUtils().calculateClaimRefund(myFaction.getLandRounded());

			if (Conf.bankEnabled && Conf.bankFactionPaysLandCosts) {
				if (!VaultEngine.getUtils().modifyMoney(myFaction, refund, Lang.COMMAND_UNCLAIM_TOUNCLAIM.toString(), Lang.COMMAND_UNCLAIM_FORUNCLAIM.toString())) {
					return false;
				}
			} else {
				if (!VaultEngine.getUtils().modifyMoney(fme, refund, Lang.COMMAND_UNCLAIM_TOUNCLAIM.toString(), Lang.COMMAND_UNCLAIM_FORUNCLAIM.toString())) {
					return false;
				}
			}
		}

		event.getTransactions().entrySet().stream().forEach(entry -> {
			FLocation thisLocation = entry.getKey();
			Faction thisFaction = entry.getValue();
			
			Board.get().removeAt(entry.getKey());
			thisFaction.sendMessage(Lang.COMMAND_UNCLAIM_FACTIONUNCLAIMED, fme.describeTo(myFaction, true));
			

			if (!Conf.logLandUnclaims) return;
			Factions.get().log(Lang.COMMAND_UNCLAIM_LOG.format(fme.getName(), thisLocation.getCoordString(), thisFaction.getTag()));
			
		});
		
		return true;
	}

	@Override
	public String getUsageTranslation() {
		return Lang.COMMAND_UNCLAIM_DESCRIPTION.toString();
	}

}
