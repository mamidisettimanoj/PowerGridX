package com.powergridx.services;

import com.powergridx.models.GridEvent;
import com.powergridx.models.HouseholdRecord;
import com.powergridx.models.ZoneRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Holds the loaded datasets as one immutable snapshot.
 * The snapshot is published only after everything has been loaded, so request threads never see partial data.
 */
public final class DataStore {
    private static final Logger log = LoggerFactory.getLogger(DataStore.class);

    public static final class Snapshot {
        public final List<HouseholdRecord> household;
        public final List<ZoneRecord> zones;
        public final List<GridEvent> events;                 // chronological
        public final DataQualityReport householdQuality;
        public final DataQualityReport zoneQuality;
        public final Instant loadedAt = Instant.now();

        Snapshot(DataIngestionService.Dataset<HouseholdRecord> h, DataIngestionService.Dataset<ZoneRecord> z) {
            this.household = Collections.unmodifiableList(h.records);
            this.zones = Collections.unmodifiableList(z.records);
            this.events = Collections.unmodifiableList(GridEvent.detect(h.records));
            this.householdQuality = h.quality;
            this.zoneQuality = z.quality;
        }
    }

    private static volatile Snapshot snapshot;

    private DataStore() {}

    /** Loads both CSV files. Throws if a file is missing or unusable so the server never starts with empty data. */
    public static void load() throws IOException {
        Path householdFile = DataIngestionService.resolveDataFile(DataIngestionService.HOUSEHOLD_FILE);
        Path zoneFile = DataIngestionService.resolveDataFile(DataIngestionService.ZONE_FILE);
        load(householdFile, zoneFile);
    }

    public static void load(Path householdFile, Path zoneFile) throws IOException {
        var household = DataIngestionService.loadHouseholdData(householdFile);
        var zones = DataIngestionService.loadZoneData(zoneFile);
        if (household.records.isEmpty() || zones.records.isEmpty()) {
            throw new IOException("No usable rows found (household=" + household.records.size() + ", zones=" + zones.records.size() + ")");
        }
        snapshot = new Snapshot(household, zones);
        log.info("Datasets ready: {} household rows, {} zone rows, {} grid events",
            snapshot.household.size(), snapshot.zones.size(), snapshot.events.size());
    }

    public static boolean isReady() {
        return snapshot != null;
    }

    public static Snapshot get() {
        Snapshot s = snapshot;
        if (s == null) throw new IllegalStateException("Datasets are not loaded yet");
        return s;
    }
}
