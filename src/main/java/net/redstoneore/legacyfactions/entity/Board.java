package net.redstoneore.legacyfactions.entity;

import java.util.ArrayList;
import java.util.Set;

import org.bukkit.World;

import net.redstoneore.legacyfactions.FLocation;
import net.redstoneore.legacyfactions.entity.persist.json.JSONBoard;
import net.redstoneore.legacyfactions.locality.Locality;


public abstract class Board {
	
	protected static Board i = getImpl();
	private static Board getImpl() {
		switch (Conf.backEnd) {
			case JSON:
				return new JSONBoard();
		}
		return null;
	}
	public static Board get() { return i; }
	
	//----------------------------------------------//
	// Get and Set
	//----------------------------------------------//
	
	public abstract String getIdAt(FLocation flocation);
	
	public abstract String getIdAt(Locality flocation);

	public abstract Faction getFactionAt(FLocation flocation);
	
	public abstract Faction getFactionAt(Locality locality);

	public abstract void setIdAt(String id, FLocation flocation);

	public abstract void setFactionAt(Faction faction, FLocation flocation);

	public abstract void removeAt(FLocation flocation);

	public abstract Set<FLocation> getAllClaims(String factionId);

	public abstract Set<FLocation> getAllClaims(Faction faction);

	public abstract Set<FLocation> getAllClaims();

	// not to be confused with claims, ownership referring to further member-specific ownership of a claim
	public abstract void clearOwnershipAt(FLocation flocation);

	public abstract void unclaimAll(String factionId);

	// Is this coord NOT completely surrounded by coords claimed by the same faction?
	// Simpler: Is there any nearby coord with a faction other than the faction here?
	public abstract boolean isBorderLocation(FLocation flocation);

	// Is this coord connected to any coord claimed by the specified faction?
	public abstract boolean isConnectedLocation(FLocation flocation, Faction faction);

	public abstract boolean hasFactionWithin(FLocation flocation, Faction faction, int radius);

	//----------------------------------------------//
	// Cleaner. Remove orphaned foreign keys
	//----------------------------------------------//

	public abstract void clean();

	//----------------------------------------------//
	// Coord count
	//----------------------------------------------//

	public abstract int getFactionCoordCount(String factionId);

	public abstract int getFactionCoordCount(Faction faction);

	/**
	 * Deprecated, use {@link #getFactionCoordCountInWorld(Faction, World)}
	 * @param faction
	 * @param worldName
	 * @return
	 */
	@Deprecated
	public abstract int getFactionCoordCountInWorld(Faction faction, String worldName);
	
	public abstract int getFactionCoordCountInWorld(Faction faction, World world);

	//----------------------------------------------//
	// Map generation
	//----------------------------------------------//

	/**
	 * The map is relative to a coord and a faction north is in the direction of decreasing x east is in the direction
	 * of decreasing z
	 */
	public abstract ArrayList<String> getMap(Faction faction, FLocation flocation, double inDegrees);

	public abstract void forceSave();

	public abstract void forceSave(boolean sync);

	public abstract boolean load();
	
}
