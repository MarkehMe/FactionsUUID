package net.redstoneore.legacyfactions.cmd;

import net.redstoneore.legacyfactions.Permission;
import net.redstoneore.legacyfactions.Lang;
import net.redstoneore.legacyfactions.entity.Conf;

public class CmdFactionsToggleAllianceChat extends FCommand {

    // -------------------------------------------------- //
    // CONSTRUCT
    // -------------------------------------------------- //

    public CmdFactionsToggleAllianceChat() {
        this.aliases.addAll(Conf.cmdAliasesToggleAllianceChat);

        this.disableOnLock = false;

        this.permission = Permission.TOGGLE_ALLIANCE_CHAT.node;
        this.disableOnLock = false;

		this.senderMustBePlayer = true;
		this.senderMustBeMember = true;
		this.senderMustBeModerator = false;
		this.senderMustBeAdmin = false;
    }

	// -------------------------------------------------- //
	// METHODS
	// -------------------------------------------------- //

    @Override
    public void perform() {
        if (!Conf.factionOnlyChat) {
            msg(Lang.COMMAND_CHAT_DISABLED.toString());
            return;
        }

        boolean ignoring = fme.isIgnoreAllianceChat();

        msg(ignoring ? Lang.COMMAND_TOGGLEALLIANCECHAT_UNIGNORE : Lang.COMMAND_TOGGLEALLIANCECHAT_IGNORE);
        fme.setIgnoreAllianceChat(!ignoring);
    }
    
    @Override
    public String getUsageTranslation() {
        return Lang.COMMAND_TOGGLEALLIANCECHAT_DESCRIPTION.toString();
    }
    
}
