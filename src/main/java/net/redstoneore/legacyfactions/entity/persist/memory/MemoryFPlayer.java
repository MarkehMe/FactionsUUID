package net.redstoneore.legacyfactions.entity.persist.memory;

import net.redstoneore.legacyfactions.event.EventFactionsDisband;
import org.bukkit.*;
import org.bukkit.entity.Player;

import net.redstoneore.legacyfactions.*;
import net.redstoneore.legacyfactions.entity.Board;
import net.redstoneore.legacyfactions.entity.Conf;
import net.redstoneore.legacyfactions.entity.FPlayer;
import net.redstoneore.legacyfactions.entity.FPlayerColl;
import net.redstoneore.legacyfactions.entity.Faction;
import net.redstoneore.legacyfactions.entity.FactionColl;
import net.redstoneore.legacyfactions.event.EventFactionsChange;
import net.redstoneore.legacyfactions.event.EventFactionsLandChange;
import net.redstoneore.legacyfactions.event.EventFactionsChange.ChangeReason;
import net.redstoneore.legacyfactions.event.EventFactionsLandChange.LandChangeCause;
import net.redstoneore.legacyfactions.integration.essentials.EssentialsEngine;
import net.redstoneore.legacyfactions.integration.vault.VaultEngine;
import net.redstoneore.legacyfactions.integration.worldguard.WorldGuardEngine;
import net.redstoneore.legacyfactions.integration.worldguard.WorldGuardIntegration;
import net.redstoneore.legacyfactions.scoreboards.FScoreboard;
import net.redstoneore.legacyfactions.scoreboards.sidebar.FInfoSidebar;
import net.redstoneore.legacyfactions.util.RelationUtil;
import net.redstoneore.legacyfactions.util.WarmUpUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Logged in players always have exactly one FPlayer instance. Logged out players may or may not have an FPlayer
 * instance. They will always have one if they are part of a faction. This is because only players with a faction are
 * saved to disk (in order to not waste disk space).
 * <p/>
 * The FPlayer is linked to a minecraft player using the player name.
 * <p/>
 * The same instance is always returned for the same player. This means you can use the == operator. No .equals method
 * necessary.
 */

public abstract class MemoryFPlayer implements FPlayer {

    protected String factionId;
    protected Role role;
    protected String title;
    protected double power;
    protected double powerBoost;
    protected long lastPowerUpdateTime;
    protected long lastLoginTime;
    protected ChatMode chatMode;
    protected boolean ignoreAllianceChat = false;
    protected String id;
    protected String name;
    protected boolean monitorJoins;
    protected boolean spyingChat = false;
    protected boolean showScoreboard = true;
    protected WarmUpUtil.Warmup warmup;
    protected int warmupTask;
    protected boolean isAdminBypassing = false;
    protected int kills, deaths;
    protected boolean willAutoLeave = true;

    protected transient FLocation lastStoodAt = new FLocation("world", 0, 0); // Where did this player stand the last time we checked?
    protected transient boolean mapAutoUpdating;
    protected transient Faction autoClaimFor;
    protected transient boolean autoSafeZoneEnabled;
    protected transient boolean autoWarZoneEnabled;
    protected transient boolean loginPvpDisabled;
    protected transient long lastFrostwalkerMessage;


    public void login() {
        this.kills = getPlayer().getStatistic(Statistic.PLAYER_KILLS);
        this.deaths = getPlayer().getStatistic(Statistic.DEATHS);
    }

    public void logout() {
        // Ensure power is up to date
        this.getPower();
        
        // Update last login time
        this.setLastLoginTime(System.currentTimeMillis());
        
        // Store statistics 
        this.kills = getPlayer().getStatistic(Statistic.PLAYER_KILLS);
        this.deaths = getPlayer().getStatistic(Statistic.DEATHS);
        
        // Remove from stuck map
        if (Factions.get().getStuckMap().containsKey(this.getPlayer().getUniqueId())) {
            Factions.get().getStuckMap().remove(this.getPlayer().getUniqueId());
            Factions.get().getTimers().remove(this.getPlayer().getUniqueId());
        }
        
        if (!this.getFaction().isWilderness()) {
        	// Toggle
        	this.getFaction().memberLoggedOff();
            
        	// Notify members if required
            this.getFaction().getWhereOnline(true)
            	.stream()
            	.forEach(fplayer -> {
            		if (fplayer == this || !fplayer.isMonitoringJoins()) return;
            		
                    fplayer.msg(Lang.FACTION_LOGOUT, this.getName());
            	});

        }

        
    }

    public Faction getFaction() {
        if (this.factionId == null) {
            this.factionId = "0";
        }
        return FactionColl.get().getFactionById(this.factionId);
    }

    public String getFactionId() {
        return this.factionId;
    }

    public boolean hasFaction() {
        return !factionId.equals("0");
    }

    public void setFaction(Faction faction) {
        Faction oldFaction = this.getFaction();
        if (oldFaction != null) {
            oldFaction.removeFPlayer(this);
        }
        faction.addFPlayer(this);
        this.factionId = faction.getId();
    }

    public void setMonitorJoins(boolean monitor) {
        this.monitorJoins = monitor;
    }

    public boolean isMonitoringJoins() {
        return this.monitorJoins;
    }

    public Role getRole() {
        return this.role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public double getPowerBoost() {
        return this.powerBoost;
    }

    public void setPowerBoost(double powerBoost) {
        this.powerBoost = powerBoost;
    }

    public boolean willAutoLeave() {
        return this.willAutoLeave;
    }

    public void setAutoLeave(boolean willLeave) {
        this.willAutoLeave = willLeave;
        Factions.get().debug(name + " set autoLeave to " + willLeave);
    }

    public long getLastFrostwalkerMessage() {
        return this.lastFrostwalkerMessage;
    }

    public void setLastFrostwalkerMessage() {
        this.lastFrostwalkerMessage = System.currentTimeMillis();
    }

    public Faction getAutoClaimFor() {
        return autoClaimFor;
    }

    public void setAutoClaimFor(Faction faction) {
        this.autoClaimFor = faction;
        if (this.autoClaimFor != null) {
        	if ( ! this.autoClaimFor.isSafeZone()) {
        		this.autoSafeZoneEnabled = false;
        	}
        	
        	if ( ! this.autoClaimFor.isWarZone()) {
        		this.autoWarZoneEnabled = false;
        	}
        }
    }

    public boolean isAutoSafeClaimEnabled() {
        return autoSafeZoneEnabled;
    }

    public void setIsAutoSafeClaimEnabled(boolean enabled) {
        this.autoSafeZoneEnabled = enabled;
        if (enabled) {
            this.autoClaimFor = null;
            this.autoWarZoneEnabled = false;
        }
    }

    public boolean isAutoWarClaimEnabled() {
        return autoWarZoneEnabled;
    }

    public void setIsAutoWarClaimEnabled(boolean enabled) {
        this.autoWarZoneEnabled = enabled;
        if (enabled) {
            this.autoClaimFor = null;
            this.autoSafeZoneEnabled = false;
        }
    }

    public boolean isAdminBypassing() {
        return this.isAdminBypassing;
    }
    
    @Deprecated
    public boolean isVanished() {
        return EssentialsEngine.isVanished(getPlayer());
    }
    
    public boolean isVanished(FPlayer viewer) {
    	return EssentialsEngine.isVanished(this.getPlayer()) || viewer.getPlayer().canSee(this.getPlayer());
    }
    
    public void setIsAdminBypassing(boolean val) {
        this.isAdminBypassing = val;
    }

    public void setChatMode(ChatMode chatMode) {
        this.chatMode = chatMode;
    }

    public ChatMode getChatMode() {
        if (this.factionId.equals("0") || !Conf.factionOnlyChat) {
            this.chatMode = ChatMode.PUBLIC;
        }
        return chatMode;
    }

    public void setIgnoreAllianceChat(boolean ignore) {
        this.ignoreAllianceChat = ignore;
    }

    public boolean isIgnoreAllianceChat() {
        return ignoreAllianceChat;
    }

    public void setSpyingChat(boolean chatSpying) {
        this.spyingChat = chatSpying;
    }

    public boolean isSpyingChat() {
        return spyingChat;
    }

    // FIELD: account
    public String getAccountId() {
        return this.getId();
    }

    public MemoryFPlayer() {
    }

    public MemoryFPlayer(String id) {
        this.id = id;
        this.resetFactionData(false);
        this.power = Conf.powerPlayerStarting;
        this.lastPowerUpdateTime = System.currentTimeMillis();
        this.lastLoginTime = System.currentTimeMillis();
        this.mapAutoUpdating = false;
        this.autoClaimFor = null;
        this.autoSafeZoneEnabled = false;
        this.autoWarZoneEnabled = false;
        this.loginPvpDisabled = Conf.noPVPDamageToOthersForXSecondsAfterLogin > 0;
        this.powerBoost = 0.0;
        this.showScoreboard = Conf.scoreboardDefaultEnabled;
        this.kills = 0;
        this.deaths = 0;

        if (!Conf.newPlayerStartingFactionID.equals("0") && FactionColl.get().isValidFactionId(Conf.newPlayerStartingFactionID)) {
            this.factionId = Conf.newPlayerStartingFactionID;
        }
    }

    public MemoryFPlayer(MemoryFPlayer other) {
        this.factionId = other.factionId;
        this.id = other.id;
        this.power = other.power;
        this.lastLoginTime = other.lastLoginTime;
        this.mapAutoUpdating = other.mapAutoUpdating;
        this.autoClaimFor = other.autoClaimFor;
        this.autoSafeZoneEnabled = other.autoSafeZoneEnabled;
        this.autoWarZoneEnabled = other.autoWarZoneEnabled;
        this.loginPvpDisabled = other.loginPvpDisabled;
        this.powerBoost = other.powerBoost;
        this.role = other.role;
        this.title = other.title;
        this.chatMode = other.chatMode;
        this.spyingChat = other.spyingChat;
        this.lastStoodAt = other.lastStoodAt;
        this.isAdminBypassing = other.isAdminBypassing;
        this.showScoreboard = Conf.scoreboardDefaultEnabled;
        this.kills = other.kills;
        this.deaths = other.deaths;
    }

    public void resetFactionData(boolean doSpoutUpdate) {
        // clean up any territory ownership in old faction, if there is one
        if (factionId != null && FactionColl.get().isValidFactionId(this.getFactionId())) {
            Faction currentFaction = this.getFaction();
            currentFaction.removeFPlayer(this);
            if (currentFaction.isNormal()) {
                currentFaction.clearClaimOwnership(this);
            }
        }

        this.factionId = "0"; // The default neutral faction
        this.chatMode = ChatMode.PUBLIC;
        this.role = Role.NORMAL;
        this.title = "";
        this.autoClaimFor = null;
    }

    public void resetFactionData() {
        this.resetFactionData(true);
    }

    // -------------------------------------------- //
    // Getters And Setters
    // -------------------------------------------- //


    public long getLastLoginTime() {
        return lastLoginTime;
    }


    public void setLastLoginTime(long lastLoginTime) {
        losePowerFromBeingOffline();
        this.lastLoginTime = lastLoginTime;
        this.lastPowerUpdateTime = lastLoginTime;
        if (Conf.noPVPDamageToOthersForXSecondsAfterLogin > 0) {
            this.loginPvpDisabled = true;
        }
    }

    public boolean isMapAutoUpdating() {
        return mapAutoUpdating;
    }

    public void setMapAutoUpdating(boolean mapAutoUpdating) {
        this.mapAutoUpdating = mapAutoUpdating;
    }

    public boolean hasLoginPvpDisabled() {
        if (!loginPvpDisabled) {
            return false;
        }
        if (this.lastLoginTime + (Conf.noPVPDamageToOthersForXSecondsAfterLogin * 1000) < System.currentTimeMillis()) {
            this.loginPvpDisabled = false;
            return false;
        }
        return true;
    }

    public FLocation getLastStoodAt() {
        return this.lastStoodAt;
    }

    public void setLastStoodAt(FLocation flocation) {
        this.lastStoodAt = flocation;
    }

    //----------------------------------------------//
    // Title, Name, Faction Tag and Chat
    //----------------------------------------------//

    // Base:

    public String getTitle() {
        return this.hasFaction() ? title : Lang.NOFACTION_PREFIX.toString();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        if (this.name == null) {
            // Older versions of FactionsUUID don't save the name,
            // so `name` will be null the first time it's retrieved
            // after updating
            OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.fromString(getId()));
            this.name = offline.getName() != null ? offline.getName() : getId();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return this.hasFaction() ? this.getFaction().getTag() : "";
    }

    // Base concatenations:

    public String getNameAndSomething(String something) {
        String ret = this.role.getPrefix();
        if (something.length() > 0) {
            ret += something + " ";
        }
        ret += this.getName();
        return ret;
    }

    public String getNameAndTitle() {
        return this.getNameAndSomething(this.getTitle());
    }

    public String getNameAndTag() {
        return this.getNameAndSomething(this.getTag());
    }

    // Colored concatenations:
    // These are used in information messages

    public String getNameAndTitle(Faction faction) {
        return this.getColorTo(faction) + this.getNameAndTitle();
    }

    public String getNameAndTitle(MemoryFPlayer fplayer) {
        return this.getColorTo(fplayer) + this.getNameAndTitle();
    }

    // Chat Tag:
    // These are injected into the format of global chat messages.

    public String getChatTag() {
        return this.hasFaction() ? String.format(Conf.chatTagFormat, this.role.getPrefix() + this.getTag()) : "";
    }

    // Colored Chat Tag
    public String getChatTag(Faction faction) {
        return this.hasFaction() ? this.getRelationTo(faction).getColor() + getChatTag() : "";
    }

    public String getChatTag(MemoryFPlayer fplayer) {
        return this.hasFaction() ? this.getColorTo(fplayer) + getChatTag() : "";
    }

    public int getKills() {
        return isOnline() ? getPlayer().getStatistic(Statistic.PLAYER_KILLS) : this.kills;
    }

    public int getDeaths() {
        return isOnline() ? getPlayer().getStatistic(Statistic.DEATHS) : this.deaths;

    }

    // -------------------------------
    // Relation and relation colors
    // -------------------------------

    @Override
    public String describeTo(RelationParticipator that, boolean ucfirst) {
        return RelationUtil.describeThatToMe(this, that, ucfirst);
    }

    @Override
    public String describeTo(RelationParticipator that) {
        return RelationUtil.describeThatToMe(this, that);
    }

    @Override
    public Relation getRelationTo(RelationParticipator rp) {
        return RelationUtil.getRelationTo(this, rp);
    }

    @Override
    public Relation getRelationTo(RelationParticipator rp, boolean ignorePeaceful) {
        return RelationUtil.getRelationTo(this, rp, ignorePeaceful);
    }

    public Relation getRelationToLocation() {
        return Board.get().getFactionAt(new FLocation(this)).getRelationTo(this);
    }

    @Override
    public ChatColor getColorTo(RelationParticipator rp) {
        return RelationUtil.getColorOfThatToMe(this, rp);
    }

    //----------------------------------------------//
    // Health
    //----------------------------------------------//
    public void heal(int amnt) {
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.setHealth(player.getHealth() + amnt);
    }


    //----------------------------------------------//
    // Power
    //----------------------------------------------//
    public double getPower() {
        this.updatePower();
        return this.power;
    }

    public void alterPower(double delta) {
        this.power += delta;
        if (this.power > this.getPowerMax()) {
            this.power = this.getPowerMax();
        } else if (this.power < this.getPowerMin()) {
            this.power = this.getPowerMin();
        }
    }

    public double getPowerMax() {
        return Conf.powerPlayerMax + this.powerBoost;
    }

    public double getPowerMin() {
        return Conf.powerPlayerMin + this.powerBoost;
    }

    public int getPowerRounded() {
        return (int) Math.round(this.getPower());
    }

    public int getPowerMaxRounded() {
        return (int) Math.round(this.getPowerMax());
    }

    public int getPowerMinRounded() {
        return (int) Math.round(this.getPowerMin());
    }

    public void updatePower() {
        if (this.isOffline()) {
            losePowerFromBeingOffline();
            if (!Conf.powerRegenOffline) {
                return;
            }
        } else if (hasFaction() && getFaction().isPowerFrozen()) {
            return; // Don't let power regen if faction power is frozen.
        }
        long now = System.currentTimeMillis();
        long millisPassed = now - this.lastPowerUpdateTime;
        this.lastPowerUpdateTime = now;

        Player thisPlayer = this.getPlayer();
        if (thisPlayer != null && thisPlayer.isDead()) {
            return;  // don't let dead players regain power until they respawn
        }

        int millisPerMinute = 60 * 1000;
        this.alterPower(millisPassed * Conf.powerPerMinute / millisPerMinute);
    }

    public void losePowerFromBeingOffline() {
        if (Conf.powerOfflineLossPerDay > 0.0 && this.power > Conf.powerOfflineLossLimit) {
            long now = System.currentTimeMillis();
            long millisPassed = now - this.lastPowerUpdateTime;
            this.lastPowerUpdateTime = now;

            double loss = millisPassed * Conf.powerOfflineLossPerDay / (24 * 60 * 60 * 1000);
            if (this.power - loss < Conf.powerOfflineLossLimit) {
                loss = this.power;
            }
            this.alterPower(-loss);
        }
    }

    public void onDeath() {
    	this.onDeath(Conf.powerPerDeath);
    }
    
    public void onDeath(double powerLoss) {
        this.updatePower();
        this.alterPower(-powerLoss);
        if (hasFaction()) {
            getFaction().setLastDeath(System.currentTimeMillis());
        }
    }

    //----------------------------------------------//
    // Territory
    //----------------------------------------------//
    public boolean isInOwnTerritory() {
        return Board.get().getFactionAt(new FLocation(this)) == this.getFaction();
    }

    public boolean isInOthersTerritory() {
        Faction factionHere = Board.get().getFactionAt(new FLocation(this));
        return factionHere != null && factionHere.isNormal() && factionHere != this.getFaction();
    }

    public boolean isInAllyTerritory() {
        return Board.get().getFactionAt(new FLocation(this)).getRelationTo(this).isAlly();
    }

    public boolean isInNeutralTerritory() {
        return Board.get().getFactionAt(new FLocation(this)).getRelationTo(this).isNeutral();
    }

    public boolean isInEnemyTerritory() {
        return Board.get().getFactionAt(new FLocation(this)).getRelationTo(this).isEnemy();
    }

    public void sendFactionHereMessage(Faction from) {
        Faction toShow = Board.get().getFactionAt(getLastStoodAt());
        boolean showChat = true;
        if (showInfoBoard(toShow)) {
            FScoreboard.get(this).setTemporarySidebar(new FInfoSidebar(toShow));
            showChat = Conf.scoreboardInChat;
        }
        if (showChat) {
            this.sendMessage(Factions.get().getTextUtil().parse(Lang.FACTION_LEAVE.format(from.getTag(this), toShow.getTag(this))));
        }
    }

    /**
     * Check if the scoreboard should be shown. Simple method to be used by above method.
     *
     * @param toShow Faction to be shown.
     *
     * @return true if should show, otherwise false.
     */
    public boolean showInfoBoard(Faction toShow) {
        return showScoreboard && !toShow.isWarZone() && !toShow.isWilderness() && !toShow.isSafeZone() && !Conf.scoreboardInfo.isEmpty() && Conf.scoreboardInfoEnabled && FScoreboard.get(this) != null;
    }

    @Override
    public boolean showScoreboard() {
        return this.showScoreboard;
    }

    @Override
    public void setShowScoreboard(boolean show) {
        this.showScoreboard = show;
    }

    // -------------------------------
    // Actions
    // -------------------------------

    public void leave(boolean makePay) {
        Faction myFaction = this.getFaction();
        makePay = makePay && VaultEngine.shouldBeUsed() && !this.isAdminBypassing();

        if (myFaction == null) {
            resetFactionData();
            return;
        }

        boolean perm = myFaction.isPermanent();

        if (!perm && this.getRole() == Role.ADMIN && myFaction.getFPlayers().size() > 1) {
            msg(Lang.LEAVE_PASSADMIN);
            return;
        }

        if (!Conf.canLeaveWithNegativePower && this.getPower() < 0) {
            msg(Lang.LEAVE_NEGATIVEPOWER);
            return;
        }

        // if economy is enabled and they're not on the bypass list, make sure they can pay
        if (makePay && !VaultEngine.hasAtLeast(this, Conf.econCostLeave, Lang.LEAVE_TOLEAVE.toString())) {
            return;
        }
        
        EventFactionsChange event = new EventFactionsChange(this, myFaction, FactionColl.get().getWilderness(), true, ChangeReason.LEAVE);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        // then make 'em pay (if applicable)
        if (makePay && !VaultEngine.modifyMoney(this, -Conf.econCostLeave, Lang.LEAVE_TOLEAVE.toString(), Lang.LEAVE_FORLEAVE.toString())) {
            return;
        }

        // Am I the last one in the faction?
        if (myFaction.getFPlayers().size() == 1) {
            // Transfer all money
            if (VaultEngine.shouldBeUsed()) {
                VaultEngine.transferMoney(this, myFaction, this, VaultEngine.getBalance(myFaction.getAccountId()));
            }
        }

        if (myFaction.isNormal()) {
            for (FPlayer fplayer : myFaction.getWhereOnline(true)) {
                fplayer.msg(Lang.LEAVE_LEFT, this.describeTo(fplayer, true), myFaction.describeTo(fplayer));
            }

            if (Conf.logFactionLeave) {
                Factions.get().log(Lang.LEAVE_LEFT.format(this.getName(), myFaction.getTag()));
            }
        }

        myFaction.removeAnnouncements(this);
        this.resetFactionData();

        if (myFaction.isNormal() && !perm && myFaction.getFPlayers().isEmpty()) {
            EventFactionsDisband disbandEvent = new EventFactionsDisband(getPlayer(), myFaction.getId(), false,
                    EventFactionsDisband.DisbandReason.LEAVE);
            Bukkit.getPluginManager().callEvent(disbandEvent);

            // Remove this faction
            for (FPlayer fplayer : FPlayerColl.all()) {
                fplayer.msg(Lang.LEAVE_DISBANDED, myFaction.describeTo(fplayer, true));
            }

            FactionColl.get().removeFaction(myFaction.getId());
            if (Conf.logFactionDisband) {
                Factions.get().log(Lang.LEAVE_DISBANDEDLOG.format(myFaction.getTag(), myFaction.getId(), this.getName()));
            }
        }
    }

    public boolean canClaimForFaction(Faction forFaction) {
        return !forFaction.isWilderness() && (this.isAdminBypassing() || (forFaction == this.getFaction() && this.getRole().isAtLeast(Role.MODERATOR)) || (forFaction.isSafeZone() && Permission.MANAGE_SAFE_ZONE.has(getPlayer())) || (forFaction.isWarZone() && Permission.MANAGE_WAR_ZONE.has(getPlayer())));
    }

    public boolean canClaimForFactionAtLocation(Faction forFaction, Location location, boolean notifyFailure) {
        FLocation flocation = new FLocation(location);
        
        return canClaimForFactionAtLocation(forFaction, flocation, notifyFailure);
    }
    
    public boolean canClaimForFactionAtLocation(Faction forFaction, FLocation flocation, boolean notifyFailure) {
        Faction myFaction = this.getFaction();
        Faction currentFaction = Board.get().getFactionAt(flocation);
        int ownedLand = forFaction.getLandRounded();
        
        // Admin Bypass needs no further checks
        if (this.isAdminBypassing()) return true;
        
        // Can claim in safe zone?
        if (forFaction.isSafeZone() && Permission.MANAGE_SAFE_ZONE.has(getPlayer())) return true;
        
        // Claim for war zone
        if (forFaction.isWarZone() && Permission.MANAGE_WAR_ZONE.has(getPlayer())) return true;

        // Checks for WorldGuard regions in the chunk attempting to be claimed
        if (WorldGuardIntegration.get().isEnabled() && Conf.worldGuardChecking && WorldGuardEngine.checkForRegionsInChunk(flocation)) {
        	this.msg(notifyFailure, Lang.CLAIM_PROTECTED);
        	return false;
        }
        
        // Check if this is a no-claim world
        if (Conf.worldsNoClaiming.contains(flocation.getWorldName())) {
        	this.msg(notifyFailure, Lang.CLAIM_DISABLED);
        	return false;
        } 
        
        // Can only claim for own faction
        if (myFaction != forFaction) {
        	this.msg(notifyFailure, Lang.CLAIM_CANTCLAIM, forFaction.describeTo(this));
        	return false;
        }
        
        // Do we already own this?
        if (forFaction == currentFaction) {
        	this.msg(notifyFailure, Lang.CLAIM_ALREADYOWN, forFaction.describeTo(this, true));
        	return false;
        }
        
        // Are they at lease a moderator?
        if (!this.getRole().isAtLeast(Role.MODERATOR)) {
        	this.msg(notifyFailure, Lang.CLAIM_MUSTBE, Role.MODERATOR.getTranslation());
        	return false;
        }
        
        // Check for minimum members
        if (forFaction.getFPlayers().size() < Conf.claimsRequireMinFactionMembers) {
        	this.msg(notifyFailure, Lang.CLAIM_MEMBERS, Conf.claimsRequireMinFactionMembers);
        	return false;
        }
        
        // Check for safezone
        if (currentFaction.isSafeZone()) {
        	this.msg(notifyFailure, Lang.CLAIM_SAFEZONE);
        	return false;
        }
        
        // Check for warzone
        if (currentFaction.isWarZone()) {
        	this.msg(notifyFailure, Lang.CLAIM_WARZONE);
        	return false;
        } 
        
        // Check raidable can overclaim
        if (Conf.raidableAllowOverclaim && ownedLand >= forFaction.getPowerRounded()) {
        	this.msg(notifyFailure, Lang.CLAIM_POWER);
        	return false;
        }
        
        // Check for claimedLandsMax
        if (Conf.claimedLandsMax > 0 && ownedLand >= Conf.claimedLandsMax && forFaction.isNormal()) {
        	this.msg(notifyFailure, Lang.CLAIM_LIMIT);
        	return false;
        }
        
        // Check for ally claim 
        if (currentFaction.getRelationTo(forFaction) == Relation.ALLY) {
        	this.msg(notifyFailure, Lang.CLAIM_ALLY.toString());
        	return false;
        } 

        // Check if must be connected
        if (Conf.claimsMustBeConnected && !this.isAdminBypassing() && myFaction.getLandRoundedInWorld(flocation.getWorld()) > 0 && !Board.get().isConnectedLocation(flocation, myFaction) && (!Conf.claimsCanBeUnconnectedIfOwnedByOtherFaction || !currentFaction.isNormal())) {
            if (Conf.claimsCanBeUnconnectedIfOwnedByOtherFaction) {
            	this.msg(notifyFailure, Lang.CLAIM_CONTIGIOUS);
            } else {
            	this.msg(notifyFailure, Lang.CLAIM_FACTIONCONTIGUOUS);
            }
            return false;
        }
        
        // Check for buffer
        if (Conf.bufferFactions > 0 && Board.get().hasFactionWithin(flocation, myFaction, Conf.bufferFactions)) {
            this.msg(notifyFailure, Lang.CLAIM_TOOCLOSETOOTHERFACTION.format(Conf.bufferFactions));
            return false;
        }
        
        // Border check
        if (Conf.claimsCanBeOutsideBorder == false && flocation.isOutsideWorldBorder(Conf.bufferWorldBorder)) {
            if (Conf.bufferWorldBorder > 0) {
                this.msg(notifyFailure, Lang.CLAIM_OUTSIDEBORDERBUFFER.format(Conf.bufferWorldBorder));
            } else {
                this.msg(notifyFailure, Lang.CLAIM_OUTSIDEWORLDBORDER);
            }
            return false;
        }
        
        // If the faction we're trying to claim is a normal faction ...
        if (currentFaction.isNormal()) {
        	
        	// .. and i'm peaceful ...
            if (myFaction.isPeaceful()) {
            	// .. don't allow - i'm peaceful.
                this.msg(notifyFailure, Lang.CLAIM_PEACEFUL, currentFaction.getTag(this));
                return false;
            }
            
            // .. and they're peaceful ...
            if (currentFaction.isPeaceful()) {
            	// .. don't allow - they're peaceful.
                this.msg(notifyFailure, Lang.CLAIM_PEACEFULTARGET, currentFaction.getTag(this));
                return false;
            }
            
            // .. and they're strong enough to hold it
            if (!currentFaction.hasLandInflation()) {
                // ... don't allow - they're too strong
                this.msg(notifyFailure, Lang.CLAIM_THISISSPARTA, currentFaction.getTag(this));
                return false;
            }
            
            // .. raidableAllowOverclaim is false, and the current faction is not strong enough 
            if (!Conf.raidableAllowOverclaim && currentFaction.hasLandInflation()) {
                // .. don't allow it, overclaim is disabled
                this.msg(notifyFailure, Lang.CLAIM_OVERCLAIM_DISABLED);
                return false;
            }
            
            if (!Board.get().isBorderLocation(flocation)) {
                this.msg(notifyFailure, Lang.CLAIM_BORDER);
                return false;
            }
        }
        
        // can claim!
        return true;
    }
    
    public boolean attemptClaim(Faction forFaction, Location location, boolean notifyFailure, EventFactionsLandChange eventLandChange) {
    	FLocation flocation = new FLocation(location);
    	
    	return attemptClaim(forFaction, flocation, notifyFailure, eventLandChange);
    }
    
    public boolean attemptClaim(Faction forFaction, FLocation flocation, boolean notifyFailure, EventFactionsLandChange eventLandChange) {
        Faction currentFaction = Board.get().getFactionAt(flocation);
        
        int ownedLand = forFaction.getLandRounded();

        if (!this.canClaimForFactionAtLocation(forFaction, flocation, notifyFailure)) {
            return false;
        }

        // if economy is enabled and they're not on the bypass list, make sure they can pay
        boolean mustPay = VaultEngine.shouldBeUsed() && !this.isAdminBypassing() && !forFaction.isSafeZone() && !forFaction.isWarZone();
        double cost = 0.0;
        EconomyParticipator payee = null;
        if (mustPay) {
            cost = VaultEngine.calculateClaimCost(ownedLand, currentFaction.isNormal());

            if (Conf.econClaimUnconnectedFee != 0.0 && forFaction.getLandRoundedInWorld(flocation.getWorld()) > 0 && !Board.get().isConnectedLocation(flocation, forFaction)) {
                cost += Conf.econClaimUnconnectedFee;
            }

            if (Conf.bankEnabled && Conf.bankFactionPaysLandCosts && this.hasFaction()) {
                payee = this.getFaction();
            } else {
                payee = this;
            }

            if (!VaultEngine.hasAtLeast(payee, cost, Lang.CLAIM_TOCLAIM.toString())) {
                return false;
            }
        }
        
        Map<FLocation, Faction> transactions = new HashMap<FLocation, Faction>();
        transactions.put(flocation, forFaction);
        
        EventFactionsLandChange event = new EventFactionsLandChange(this, transactions, LandChangeCause.Claim);
        
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        
        
        // in case it was updated in the event 
        //flocation = event.getLocation();

        // then make 'em pay (if applicable)
        if (mustPay && !VaultEngine.modifyMoney(payee, -cost, Lang.CLAIM_TOCLAIM.toString(), Lang.CLAIM_FORCLAIM.toString())) {
            return false;
        }

        // Was an over claim
        if (currentFaction.isNormal() && currentFaction.hasLandInflation()) {
            // Give them money for over claiming.
            VaultEngine.modifyMoney(payee, Conf.econOverclaimRewardMultiplier, Lang.CLAIM_TOOVERCLAIM.toString(), Lang.CLAIM_FOROVERCLAIM.toString());
        }

        // announce success
        Set<FPlayer> informTheseFPlayers = new HashSet<FPlayer>();
        informTheseFPlayers.add(this);
        informTheseFPlayers.addAll(forFaction.getWhereOnline(true));
        for (FPlayer fp : informTheseFPlayers) {
            fp.msg(Lang.CLAIM_CLAIMED, this.describeTo(fp, true), forFaction.describeTo(fp), currentFaction.describeTo(fp));
        }

        Board.get().setFactionAt(forFaction, flocation);

        if (Conf.logLandClaims) {
            Factions.get().log(Lang.CLAIM_CLAIMEDLOG.toString(), this.getName(), flocation.getCoordString(), forFaction.getTag());
        }

        return true;
    }

    public boolean shouldBeSaved() {
        if (!this.hasFaction() && (this.getPowerRounded() == this.getPowerMaxRounded() || this.getPowerRounded() == (int) Math.round(Conf.powerPlayerStarting))) {
            return false;
        }
        return true;
    }
    
    public void msg(boolean onlyIfTrue, String str, Object... args) {
        if (onlyIfTrue) this.msg(str, args);
    }

    public void msg(String str, Object... args) {
        this.sendMessage(Factions.get().getTextUtil().parse(str, args));
    }

    public void msg(boolean onlyIfTrue, Lang translation, Object... args) {
        if(onlyIfTrue) this.msg(translation, args);
    }
    
    public void msg(Lang translation, Object... args) {
        this.msg(translation.toString(), args);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(UUID.fromString(this.getId()));
    }

    public boolean isOnline() {
        return this.getPlayer() != null;
    }

    // make sure target player should be able to detect that this player is online
    public boolean isOnlineAndVisibleTo(Player player) {
        Player target = this.getPlayer();
        return target != null && player.canSee(target);
    }

    public boolean isOffline() {
        return !isOnline();
    }

    // -------------------------------------------- //
    // Message Sending Helpers
    // -------------------------------------------- //

    public void sendMessage(String msg) {
        if (msg.contains("{null}")) {
            return; // user wants this message to not send
        }
        if (msg.contains("/n/")) {
            for (String s : msg.split("/n/")) {
                sendMessage(s);
            }
            return;
        }
        Player player = this.getPlayer();
        if (player == null) {
            return;
        }
        player.sendMessage(msg);
    }

    public void sendMessage(List<String> msgs) {
        for (String msg : msgs) {
            this.sendMessage(msg);
        }
    }

    public String getNameAndTitle(FPlayer fplayer) {
        return this.getColorTo(fplayer) + this.getNameAndTitle();
    }

    @Override
    public String getChatTag(FPlayer fplayer) {
        return this.hasFaction() ? this.getRelationTo(fplayer).getColor() + getChatTag() : "";
    }

    @Override
    public String getId() {
        return id;
    }

    public abstract void remove();

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void clearWarmup() {
        if (warmup != null) {
            Bukkit.getScheduler().cancelTask(warmupTask);
            this.stopWarmup();
        }
    }

    @Override
    public void stopWarmup() {
        warmup = null;
    }

    @Override
    public boolean isWarmingUp() {
        return warmup != null;
    }

    @Override
    public WarmUpUtil.Warmup getWarmupType() {
        return warmup;
    }

    @Override
    public void addWarmup(WarmUpUtil.Warmup warmup, int taskId) {
        if (this.warmup != null) {
            this.clearWarmup();
        }
        this.warmup = warmup;
        this.warmupTask = taskId;
    }
    
    @Override
    public MemoryFPlayer asMemoryFPlayer() {
    	return (MemoryFPlayer) this;
    }
    
}
