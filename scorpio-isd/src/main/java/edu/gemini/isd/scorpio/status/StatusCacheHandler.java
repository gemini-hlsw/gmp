package edu.gemini.isd.scorpio.status;

import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.StatusItem;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StatusCacheHandler implements StatusHandler {

    private final Set<String> subscribedItems;
    private final ConcurrentMap<String, String> latestValues = new ConcurrentHashMap<>();

    public StatusCacheHandler(Set<String> subscribedItems) {
        this.subscribedItems = new LinkedHashSet<>(subscribedItems);
    }

    @Override
    public String getName() {
        return "scorpio-isd-status-cache";
    }

    @Override
    public <T> void update(StatusItem<T> item) {
        String name = item.getName();
        if (!subscribedItems.contains(name)) {
            return;
        }
        T value = item.getValue();
        latestValues.put(name, value != null ? value.toString() : null);
    }

    public Map<String, String> snapshot() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (String name : subscribedItems) {
            snapshot.put(name, latestValues.get(name));
        }
        return snapshot;
    }
}
