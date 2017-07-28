# 1.4.1
* API: Internal ecomonmy API rebuilt, economy issues should be gone
* API: `msg` is ambigious and has been depreacted and replaced with `sendMessage`
* API: More JavaDocs have been added!

# 1.4.0
* Improvement: Added coleader, you can disable it by not giving the `/f coleader` permission
* Improvement: Allow server owners to let factions use colour codes in /f title and /f desc
* Improvement: Add ability for server owners to toggle the ability for factions to color the desc and title.
* Improvement: Added truces
* Bugfix: Fix NPE when running /f mod without args (#17)
* Bugfix: Scoreboard bug - the scoreboard could be blank on first join
* Docs: Documentation Created
* Internal code cleanup

# 1.2.1
* API: New API Event: EventFactionsChatModeChange
* API: ChatMode, FLocation and Relation improvements
* Bugfix: Placeholders sometimes didn't work
* Improvement: Placeholders now support HolographicDisplays
* Improvement: chat command can be routed to VentureChat with new conf option factionChatChannelRoute

# 1.2.0
* Feature: Command aliases now configurable in conf.json
* Bugfix: Error message now shows with relation command if we can't find the faction. It used to do nothing.
* Bugfix: Enums are no longer dumb with Gson
* Bugfix: Loads of performance improvements 
* Bugfix: More translations added 
* Bugfix: Fixed an upstream bug with invalid/null arrays breaking commands
* Bugfix: Fixed an upstream bug where the console couldn’t list factions
* Bugfix: Fixed deprecated code thats removed in new build of Spigot
* Improvement: Config.yml is no longer used, it is now all in the one configuration file (conf.json)
* Improvement: Improvements to max relations (you will need to redo these in the conf.json file)
* Improvement: Placeholders across major placeholder plugins: https://github.com/redstone/LegacyFactions/wiki/Placeholders
* API: New API Method: Faction#hasMaxRelations
* API: New API Event: EventFactionsWarpUse
* API: New Placeholder API

# 1.1.0
* New API Event: EventFactionsWarpCreate
* New API Event: EventFactionsWarpDelete
* 'top' command now shows buttons when there is no criteria specified. 
* Intgration with MVdW Placeholders API
* Lots of internal improvements
* Fix an upstream language bug in #919 (https://github.com/drtshock/Factions/issues/919)
* Fix an upstream bug for isFactionsCommand
* Fixes a bug with directory not existing
* Adding permission factions.warp.passwords
