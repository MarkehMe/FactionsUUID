package net.redstoneore.legacyfactions.entity.persist.memory.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import net.redstoneore.legacyfactions.FLocation;
import net.redstoneore.legacyfactions.Factions;
import net.redstoneore.legacyfactions.entity.Board;
import net.redstoneore.legacyfactions.entity.persist.memory.MemoryBoard;
import net.redstoneore.legacyfactions.util.DiscUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;


public class JSONBoard extends MemoryBoard {
	
	// -------------------------------------------------- //
	// STATIC 
	// -------------------------------------------------- // 
	
	private static transient Path file = Paths.get(FactionsJSON.getDatabasePath().toString(), "board.json");
	public static Path getJsonFile() { return file; }
	
	// -------------------------------------------------- //
	// PERSISTANCE
	// -------------------------------------------------- //

	public Map<String, Map<String, String>> dumpAsSaveFormat() {
		Map<String, Map<String, String>> worldCoordIds = new HashMap<String, Map<String, String>>();

		String worldName, coords;
		String id;

		for (Entry<FLocation, String> entry : flocationIds.entrySet()) {
			worldName = entry.getKey().getWorldName();
			coords = entry.getKey().getCoordString();
			id = entry.getValue();
			if (!worldCoordIds.containsKey(worldName)) {
				worldCoordIds.put(worldName, new TreeMap<String, String>());
			}

			worldCoordIds.get(worldName).put(coords, id);
		}

		return worldCoordIds;
	}

	public void loadFromSaveFormat(Map<String, Map<String, String>> worldCoordIds) {
		flocationIds.clear();

		String worldName;
		String[] coords;
		int x, z;
		String factionId;

		for (Entry<String, Map<String, String>> entry : worldCoordIds.entrySet()) {
			worldName = entry.getKey();
			for (Entry<String, String> entry2 : entry.getValue().entrySet()) {
				coords = entry2.getKey().trim().split("[,\\s]+");
				x = Integer.parseInt(coords[0]);
				z = Integer.parseInt(coords[1]);
				factionId = entry2.getValue();
				flocationIds.put(new FLocation(worldName, x, z), factionId);
			}
		}
	}
	
	public String toJson() {
		try {
			return Factions.get().getObjectMapper().writeValueAsString(this.dumpAsSaveFormat());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void fromJson(String json) {		
		try {
			Map<String, Map<String, String>> worldCoordIds = Factions.get().getObjectMapper().readValue(json, new TypeReference<Map<String, Map<String, String>>>() {});
			this.loadFromSaveFormat(worldCoordIds);
			Factions.get().log("Loaded " + flocationIds.size() + " board locations");	
		} catch (IOException e) {
			Factions.get().log("Failed to load board locations");	
			e.printStackTrace();
		}
	}

	public void forceSave() {
		this.forceSave(true);
	}

	public void forceSave(boolean sync) {
		DiscUtil.writeCatch(file, this.toJson(), sync);
	}

	public boolean load() {
		Factions.get().log("Loading board from disk");
		
		if (!Files.exists(file)) {
			Factions.get().log("No board to load from disk. Creating new file.");
			forceSave();
			return true;
		}
		
		try {
			this.fromJson(DiscUtil.read(file));
		} catch (IOException e) {
			e.printStackTrace();
			Factions.get().log("Failed to load the board from disk.");
		}

		return true;
	}

	@Override
	public void convertFrom(MemoryBoard old) {
		this.flocationIds = old.flocationIds;
		forceSave();
		Board.instance = this;
	}

	@Override
	public String getPersistType() {
		return FactionsJSON.get().getType().name();
	}
	
}
