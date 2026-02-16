package edu.gemini.isd.scorpio.handler;

import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.StatusItem;
import edu.gemini.isd.scorpio.dto.StatusDTO;
import edu.gemini.isd.scorpio.dto.StatusMapper;
import edu.gemini.isd.scorpio.repository.StatusRepository;

import java.util.*;

/**
 * This class implements the interface StatusHandler (giapi-status-service) using a filter to only obtain the StatusItems related to Scorpio and can transmit all the obtained values with the snapshot method.
 * This handler is automatically registered to the StatusHandlerAggregate.
 */
public class StatusCacheHandler implements StatusHandler {
    private final Set<String> subscribedItems;
    private final StatusRepository repository;

    public StatusCacheHandler(Set<String> subscribedItems, StatusRepository repository) {
        this.subscribedItems = new LinkedHashSet<>(subscribedItems);
        this.repository = repository;
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
}
