package edu.gemini.isd.scorpio.repository;

import edu.gemini.isd.scorpio.dto.StatusDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This class have a Map to storage the StatusItems in memory.
 */
public class StatusRepository {
    private final ConcurrentMap<String, StatusDTO<?>> repository = new ConcurrentHashMap<>();

    public void save(StatusDTO<?> item){
        if (item != null){
            repository.put(item.getId(), item);
        }
    }

    public List<StatusDTO<?>> getAllStatus() {
        return new ArrayList<>(repository.values());
    }
}
