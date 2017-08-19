package net.redstoneore.legacyfactions.entity.persist.json;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.redstoneore.legacyfactions.FLocation;
import net.redstoneore.legacyfactions.Factions;
import net.redstoneore.legacyfactions.entity.Faction;
import net.redstoneore.legacyfactions.entity.FactionColl;
import net.redstoneore.legacyfactions.entity.persist.memory.MemoryFaction;
import net.redstoneore.legacyfactions.entity.persist.memory.MemoryFactions;
import net.redstoneore.legacyfactions.util.DiscUtil;
import net.redstoneore.legacyfactions.util.UUIDUtil;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;

public class JSONFactions extends MemoryFactions {
	
	// -------------------------------------------------- //
	// STATIC 
	// -------------------------------------------------- // 
	
    private static transient File file = new File(FactionsJSON.getDatabaseFolder(), "factions.json");
    public static Path getFactionsPath() { return Paths.get(file.getAbsolutePath()); }

    public static File getFactionsFile() {
        return file;
    }



    // -------------------------------------------- //
    // CONSTRUCTORS
    // -------------------------------------------- //

    public JSONFactions() {
        this.gson = Factions.get().gson;
        this.nextId = 1;
    }

	// -------------------------------------------------- //
	// FIELDS 
	// -------------------------------------------------- // 

    // Info on how to persist
    private Gson gson;

	// -------------------------------------------------- //
	// METHODS 
	// -------------------------------------------------- // 

    public Gson getGson() {
        return gson;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }


    
    public void forceSave() {
        forceSave(true);
    }

    public void forceSave(boolean sync) {
        final Map<String, JSONFaction> entitiesThatShouldBeSaved = new HashMap<String, JSONFaction>();
        for (Faction entity : this.factions.values()) {
            entitiesThatShouldBeSaved.put(entity.getId(), (JSONFaction) entity);
        }

        saveCore(file, entitiesThatShouldBeSaved, sync);
    }

    private boolean saveCore(File target, Map<String, JSONFaction> entities, boolean sync) {
        return DiscUtil.writeCatch(target, this.gson.toJson(entities), sync);
    }

    public void load() {
        Map<String, JSONFaction> factions = this.loadCore();
        if (factions == null) {
            return;
        }
        this.factions.putAll(factions);

        super.load();
        Factions.get().log("Loaded " + factions.size() + " Factions");
    }

    private Map<String, JSONFaction> loadCore() {
        if (!file.exists()) {
            return new HashMap<String, JSONFaction>();
        }

        String content = DiscUtil.readCatch(file);
        if (content == null) {
            return null;
        }

        Map<String, JSONFaction> data = this.gson.fromJson(content, new TypeToken<Map<String, JSONFaction>>() {
        }.getType());

        this.nextId = 1;
        // Do we have any names that need updating in claims or invites?

        int needsUpdate = 0;
        for (Entry<String, JSONFaction> entry : data.entrySet()) {
            String id = entry.getKey();
            Faction f = entry.getValue();
            f.setId(id);
            this.updateNextIdForId(id);
            needsUpdate += whichKeysNeedMigration(f.getInvites()).size();
            for (Set<String> keys : f.getClaimOwnership().values()) {
                needsUpdate += whichKeysNeedMigration(keys).size();
            }
        }

        if (needsUpdate > 0) {
            // We've got some converting to do!
            Bukkit.getLogger().log(Level.INFO, "Factions is now updating factions.json");

            // First we'll make a backup, because god forbid anybody heed a
            // warning
            File oldFile = new File(getFactionsFile(), "factions.json.old");
            try {
                oldFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            saveCore(oldFile, (Map<String, JSONFaction>) data, true);
            Bukkit.getLogger().log(Level.INFO, "Backed up your old data at " + oldFile.getAbsolutePath());

            Bukkit.getLogger().log(Level.INFO, "Please wait while Factions converts " + needsUpdate + " old player names to UUID. This may take a while.");

            // Update claim ownership

            for (String string : data.keySet()) {
                Faction f = data.get(string);
                Map<FLocation, Set<String>> claims = f.getClaimOwnership();
                for (FLocation key : claims.keySet()) {
                    Set<String> set = claims.get(key);

                    Set<String> list = whichKeysNeedMigration(set);

                    if (list.size() > 0) {
                        UUIDUtil fetcher = new UUIDUtil(new ArrayList<String>(list));
                        try {
                            Map<String, UUID> response = fetcher.call();
                            for (String value : response.keySet()) {
                                // Let's replace their old named entry with a
                                // UUID key
                                String id = response.get(value).toString();
                                set.remove(value.toLowerCase()); // Out with the
                                // old...
                                set.add(id); // And in with the new
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        claims.put(key, set); // Update
                    }
                }
            }

            // Update invites

            for (String string : data.keySet()) {
                Faction f = data.get(string);
                Set<String> invites = f.getInvites();
                Set<String> list = whichKeysNeedMigration(invites);

                if (list.size() > 0) {
                    UUIDUtil fetcher = new UUIDUtil(new ArrayList<String>(list));
                    try {
                        Map<String, UUID> response = fetcher.call();
                        for (String value : response.keySet()) {
                            // Let's replace their old named entry with a UUID
                            // key
                            String id = response.get(value).toString();
                            invites.remove(value.toLowerCase()); // Out with the
                            // old...
                            invites.add(id); // And in with the new
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            saveCore(oldFile, (Map<String, JSONFaction>) data, true); // Update the flatfile
            Bukkit.getLogger().log(Level.INFO, "Done converting factions.json to UUID.");
        }
        return data;
    }

    private Set<String> whichKeysNeedMigration(Set<String> keys) {
        HashSet<String> list = new HashSet<String>();
        for (String value : keys) {
            if (!value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                // Not a valid UUID..
                if (value.matches("[a-zA-Z0-9_]{2,16}")) {
                    // Valid playername, we'll mark this as one for conversion
                    // to UUID
                    list.add(value);
                }
            }
        }
        return list;
    }

    // -------------------------------------------- //
    // ID MANAGEMENT
    // -------------------------------------------- //

    public String getNextId() {
        while (!isIdFree(this.nextId)) {
            this.nextId += 1;
        }
        return Integer.toString(this.nextId);
    }

    public boolean isIdFree(String id) {
        return !this.factions.containsKey(id);
    }

    public boolean isIdFree(int id) {
        return this.isIdFree(Integer.toString(id));
    }

    protected synchronized void updateNextIdForId(int id) {
        if (this.nextId < id) {
            this.nextId = id + 1;
        }
    }

    protected void updateNextIdForId(String id) {
        try {
            int idAsInt = Integer.parseInt(id);
            this.updateNextIdForId(idAsInt);
        } catch (Exception ignored) {
        }
    }

    @Override
    public Faction generateFactionObject() {
        String id = getNextId();
        Faction faction = new JSONFaction(id);
        updateNextIdForId(id);
        return faction;
    }

    @Override
    public Faction generateFactionObject(String id) {
        Faction faction = new JSONFaction(id);
        return faction;
    }

    @Override
    public void convertFrom(MemoryFactions old) {
        this.factions.putAll(Maps.transformValues(old.factions, new Function<Faction, JSONFaction>() {
            @Override
            public JSONFaction apply(Faction arg0) {
                return new JSONFaction((MemoryFaction) arg0);
            }
        }));
        this.nextId = old.nextId;
        forceSave();
        FactionColl.i = this;
    }
}
