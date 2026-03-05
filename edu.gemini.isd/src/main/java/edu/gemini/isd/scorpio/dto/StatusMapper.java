package edu.gemini.isd.scorpio.dto;

import edu.gemini.aspen.giapi.status.StatusItem;
import edu.gemini.isd.scorpio.utils.StatusNameDictionary;

/**
 * This class provides a method to transform the external StatusItem into a StatusDTO.
 * In the mapping process the name is translated to a frontend format.
 */
public class StatusMapper {
    public static StatusDTO<Object> mapToDTO(StatusItem<?> item){
        StatusDTO<Object> dtoItem = new StatusDTO<>();

        dtoItem.setId(StatusNameDictionary.translate(item.getName()));
        dtoItem.setValue(item.getValue());
        dtoItem.setTimestamp(item.getTimestamp());

        return dtoItem;
    }
}
