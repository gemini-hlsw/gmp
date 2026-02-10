package edu.gemini.isd.scorpio.status;

import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.StatusItem;
import edu.gemini.isd.scorpio.models.StatusDTO;
import edu.gemini.isd.scorpio.models.StatusMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class StatusCacheHandler implements StatusHandler {

    private final Set<String> subscribedItems;
    private final ConcurrentMap<String, StatusDTO> latestValues = new ConcurrentHashMap<>();

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

        StatusDTO newStatus = StatusMapper.normalizeItem(item);
        T value = item.getValue();
        latestValues.put(name, value != null ? newStatus : null);
    }

    public  List<StatusDTO> snapshot() {
        List<StatusDTO> snapshot = new ArrayList<>();
        for (String name : subscribedItems) {
            if(latestValues.get(name) == null){
                continue;
            }
            snapshot.add(latestValues.get(name));
        }
        return snapshot;
    }
}
