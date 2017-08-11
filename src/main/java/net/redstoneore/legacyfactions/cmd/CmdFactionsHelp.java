package net.redstoneore.legacyfactions.cmd;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import mkremins.fanciful.FancyMessage;
import net.redstoneore.legacyfactions.Factions;
import net.redstoneore.legacyfactions.Permission;
import net.redstoneore.legacyfactions.Lang;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;
import net.redstoneore.legacyfactions.integration.vault.VaultEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CmdFactionsHelp extends FCommand {

	// -------------------------------------------------- //
	// INSTANCE
	// -------------------------------------------------- //
	
	private static CmdFactionsHelp instance = new CmdFactionsHelp();
	public static CmdFactionsHelp get() { return instance; }
	
	// -------------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------------- //

	private CmdFactionsHelp() {
		this.aliases.addAll(Conf.cmdAliasesHelp);

		this.optionalArgs.put("page", "1");

		this.permission = Permission.HELP.getNode();
		this.disableOnLock = false;

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
		// For those that want to create their own help menu they can use this (why)
		if (Conf.useCustomHelp) {
			String pageArg = this.argAsString(0, "1");
			List<String> page = Conf.helpPages.get(pageArg);
			
			if (page == null || page.isEmpty()) {
				this.sendMessage(Lang.COMMAND_HELP_404.format(pageArg));
				return;
			}
			
			page.forEach(line -> this.sendMessage(Factions.get().getTextUtil().parse(line)));
				
			return;
		}
		
		// For those that want to use the old help menu the can use this 
		if (Conf.useOldHelp) {
			if (helpPages == null) this.updateHelp();
			
			int page = this.argAsInt(0, 1);
			sendMessage(Factions.get().getTextUtil().titleize("Factions Help (" + page + "/" + helpPages.size() + ")"));

			page -= 1;

			if (page < 0 || page >= helpPages.size()) {
				sendMessage(Lang.COMMAND_HELP_404.format(String.valueOf(page)));
				return;
			}
			sendMessage(helpPages.get(page));
			return;
		}	
		
		String id = "console";
		if (!(sender instanceof ConsoleCommandSender)) {
			id = fme.getId();
		}
		
		// Otherwise, those that want an automatically generated menu
		if (!this.helpPageCache.containsKey(id)) {
			int line = 0;
			List<List<FancyMessage>> pages = new ArrayList<>();
			List<FancyMessage> lines = new ArrayList<>();
			
			for (MCommand<?> command : CmdFactions.get().subCommands) {
				if (!(sender instanceof ConsoleCommandSender) && command.permission != null && !fme.getPlayer().hasPermission(command.permission)) continue;
				line++;
				
				String suggest = "/" + CmdFactions.get().aliases.get(0) + " " + command.aliases.get(0);
				
				FancyMessage fm = new FancyMessage();
				fm.text(command.getUseageTemplate(true));
				fm.suggest(suggest);
				fm.tooltip(suggest);
				
				lines.add(fm);
				
				if (line == 10) {
					pages.add(lines);
					lines = new ArrayList<>();
					line = 0;
				}
			}
			
			this.helpPageCache.put(id, pages);
		}
		
		int page = this.argAsInt(0, 1);
		int maxPages = this.helpPageCache.get(id).size();
		
		if (page <= 0 || page > maxPages) {
			sendMessage(Lang.COMMAND_HELP_404.format(String.valueOf(page)));
			return;
		}
		
		String title = Factions.get().getTextUtil().titleize("Factions Help (" + page + "/" + maxPages + ")");
		
		FancyMessage fm = new FancyMessage("[<] ");
		
		if (page == 1) {
			fm.tooltip(ChatColor.GRAY + "No previous page.");
			fm.color(ChatColor.GRAY);
		} else {
			int prevPage = page-1;
			fm.tooltip(ChatColor.AQUA + "Go to page " + prevPage);
			fm.style(ChatColor.BOLD);
			fm.command("/f help " + prevPage);
		}
		
		fm.then(title);
		fm.then(" [>]");
		
		if (page == maxPages) {
			fm.tooltip(ChatColor.GRAY + "No next page.");
			fm.color(ChatColor.GRAY);
		} else {
			int nextPage = page+1;
			fm.tooltip(ChatColor.AQUA + "Go to page " + nextPage);
			fm.style(ChatColor.BOLD);
			fm.command("/f help " + nextPage);
		}
		
		fm.send(this.sender);
		
		this.helpPageCache.get(id).get(page-1).forEach(line -> {
			line.send(this.sender);
		});
	}
	
	private Map<String, List<List<FancyMessage>>> helpPageCache = new HashMap<>();
	
	
	public void clearHelpPageCache(CommandSender sender) {
		String id = "console";
		if (!(sender instanceof ConsoleCommandSender)) {
			id = fme.getId();
		}
		
		this.helpPageCache.remove(id);
	}
	
	public void clearHelpPageCache(FPlayer fplayer) {
		this.clearHelpPageCache(fplayer.getPlayer());
	}
	
	public void clearHelpPageCache() {
		this.helpPageCache.clear();
	}
	
	//----------------------------------------------//
	// Old help pages
	//----------------------------------------------//

	public ArrayList<ArrayList<String>> helpPages;

	public void updateHelp() {
		this.helpPages = new ArrayList<>();
		ArrayList<String> pageLines;

		pageLines = new ArrayList<>();
		pageLines.add(CmdFactionsHelp.get().getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdList.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdShow.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdPower.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdJoin.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdLeave.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdHome.getUseageTemplate(true));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_NEXTCREATE.toString()));
		this.helpPages.add(pageLines);

		pageLines = new ArrayList<>();
		pageLines.add(CmdFactions.get().cmdCreate.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdDescription.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdTag.getUseageTemplate(true));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_INVITATIONS.toString()));
		pageLines.add(CmdFactions.get().cmdOpen.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdInvite.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdDeinvite.getUseageTemplate(true));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_HOME.toString()));
		pageLines.add(CmdFactions.get().cmdSethome.getUseageTemplate(true));
		this.helpPages.add(pageLines);

		if (VaultEngine.isSetup() && Conf.econEnabled && Conf.bankEnabled) {
			pageLines = new ArrayList<>();
			pageLines.add("");
			pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_BANK_1.toString()));
			pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_BANK_2.toString()));
			pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_BANK_3.toString()));
			pageLines.add("");
			pageLines.add(CmdFactions.get().cmdMoney.getUseageTemplate(true));
			pageLines.add("");
			pageLines.add("");
			pageLines.add("");
			this.helpPages.add(pageLines);
		}

		pageLines = new ArrayList<>();
		pageLines.add(CmdFactions.get().cmdClaim.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdAutoClaim.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdUnclaim.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdUnclaimall.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdKick.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdMod.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdColeader.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdAdmin.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdTitle.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdSB.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdSeeChunk.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdStatus.getUseageTemplate(true));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_PLAYERTITLES.toString()));
		this.helpPages.add(pageLines);

		pageLines = new ArrayList<>();
		pageLines.add(CmdFactions.get().cmdMap.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdBoom.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdOwner.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdOwnerList.getUseageTemplate(true));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_OWNERSHIP_1.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_OWNERSHIP_2.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_OWNERSHIP_3.toString()));
		this.helpPages.add(pageLines);

		pageLines = new ArrayList<>();
		pageLines.add(CmdFactions.get().cmdDisband.getUseageTemplate(true));
		pageLines.add("");
		pageLines.add(CmdFactions.get().cmdRelationAlly.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdRelationNeutral.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdRelationEnemy.getUseageTemplate(true));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_1.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_2.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_3.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_4.toString()));
		this.helpPages.add(pageLines);

		pageLines = new ArrayList<>();
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_5.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_6.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_7.toString()));
		pageLines.add(Lang.COMMAND_HELP_RELATIONS_8.toString());
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_9.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_10.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_11.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_12.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_RELATIONS_13.toString()));
		this.helpPages.add(pageLines);

		pageLines = new ArrayList<>();
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_PERMISSIONS_1.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_PERMISSIONS_2.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_PERMISSIONS_3.toString()));
		pageLines.add(Lang.COMMAND_HELP_PERMISSIONS_4.toString());
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_PERMISSIONS_5.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_PERMISSIONS_6.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_PERMISSIONS_7.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_PERMISSIONS_8.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_PERMISSIONS_9.toString()));
		this.helpPages.add(pageLines);

		pageLines = new ArrayList<>();
		pageLines.add(Lang.COMMAND_HELP_MOAR_1.toString());
		pageLines.add(CmdFactions.get().cmdBypass.getUseageTemplate(true));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_ADMIN_1.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_ADMIN_2.toString()));
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_ADMIN_3.toString()));
		pageLines.add(CmdFactions.get().cmdSafeunclaimall.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdWarunclaimall.getUseageTemplate(true));
		
		pageLines.add(Factions.get().getTextUtil().parse("<i>Note: " + CmdFactions.get().cmdUnclaim.getUseageTemplate(false) + Factions.get().getTextUtil().parse("<i>") + " works on safe/war zones as well."));
		pageLines.add(CmdFactions.get().cmdPeaceful.getUseageTemplate(true));
		this.helpPages.add(pageLines);

		pageLines = new ArrayList<>();
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_MOAR_2.toString()));
		pageLines.add(CmdFactions.get().cmdPermanent.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdPermanentPower.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdPowerBoost.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdConfig.getUseageTemplate(true));
		this.helpPages.add(pageLines);

		pageLines = new ArrayList<>();
		pageLines.add(Factions.get().getTextUtil().parse(Lang.COMMAND_HELP_MOAR_3.toString()));
		pageLines.add(CmdFactions.get().cmdLock.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdReload.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdSaveAll.getUseageTemplate(true));
		pageLines.add(CmdFactions.get().cmdVersion.getUseageTemplate(true));
		this.helpPages.add(pageLines);
	}

	@Override
	public String getUsageTranslation() {
		return Lang.COMMAND_HELP_DESCRIPTION.toString();
	}
	
}
