package net.redstoneore.legacyfactions.cmd;

import net.redstoneore.legacyfactions.Permission;
import net.redstoneore.legacyfactions.Lang;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;
import net.redstoneore.legacyfactions.util.TextUtil;

public class CmdFactionsTitle extends FCommand {

    public CmdFactionsTitle() {
        this.aliases.addAll(Conf.cmdAliasesTitle);

        this.requiredArgs.add("player name");
        this.optionalArgs.put("title", "");

        this.permission = Permission.TITLE.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = true;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        FPlayer you = this.argAsBestFPlayerMatch(0);
        if (you == null) {
            return;
        }

        args.remove(0);
        String title = TextUtil.implode(args, " ");

        if (!canIAdministerYou(fme, you)) {
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!payForCommand(Conf.econCostTitle, Lang.COMMAND_TITLE_TOCHANGE, Lang.COMMAND_TITLE_FORCHANGE)) {
            return;
        }

        if(Conf.allowColorCodesInFaction) {
            title = TextUtil.parseColor(title);
        }
        you.setTitle(title);

        // Inform
        myFaction.msg(Lang.COMMAND_TITLE_CHANGED, fme.describeTo(myFaction, true), you.describeTo(myFaction, true));
    }

    @Override
    public String getUsageTranslation() {
        return Lang.COMMAND_TITLE_DESCRIPTION.toString();
    }

}
