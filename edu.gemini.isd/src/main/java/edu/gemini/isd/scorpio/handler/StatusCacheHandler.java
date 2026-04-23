package edu.gemini.isd.scorpio.handler;

import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.StatusItem;
import edu.gemini.isd.scorpio.dto.StatusDTO;
import edu.gemini.isd.scorpio.dto.StatusMapper;
import edu.gemini.isd.scorpio.repository.StatusRepository;

import java.util.*;

/**
 * This class implements the interface StatusHandler (giapi-status-service) using a filter to only obtain the StatusItems related to Scorpio.
 * This handler is automatically registered to the StatusHandlerAggregate.
 */
public class StatusCacheHandler implements StatusHandler {
    private final Set<String> subscribedItems;
    private final StatusRepository repository;
    private final List<StatusDTO<?>> previous_repository_state;

    public StatusCacheHandler(Set<String> subscribedItems, StatusRepository repository) {
        this.subscribedItems = new LinkedHashSet<>(subscribedItems);
        this.repository = repository;
        this.previous_repository_state = repository.getAllStatus();
    }

    @Override
    public String getName() {
        return "scorpio-isd-status-cache";
    }

    /**
     * Updates the Status Item received only if it's in the subscribedItems set
     * @param item the status item in this update
     */
    @Override
    public <T> void update(StatusItem<T> item) {
        if (!subscribedItems.contains(item.getName())) {
            return;
        }

        repository.save(StatusMapper.mapToDTO(item));
    }

    /**
     * Make a snapshot of the stored StatusItems in the repository
     * @return the list of Status Items
     */
    public List<StatusDTO<?>> snapshot() {
        return repository.getAllStatus();
    }

    /**
     * Store the previous state of the repository
     * Compare the previous state with the current state and return the list of changed items
     * @return the list of Status Items
     */
    public List<StatusDTO<?>> getChangedStatus() {
        List<StatusDTO<?>> newItems = repository.getAllStatus();
        List<StatusDTO<?>> changedItems = new ArrayList<>();
        for (StatusDTO<?> item : newItems) {
            if (!previous_repository_state.contains(item)) {
                changedItems.add(item);
            }
        }
        previous_repository_state.clear();
        previous_repository_state.addAll(newItems);
        return changedItems;
    }
}
