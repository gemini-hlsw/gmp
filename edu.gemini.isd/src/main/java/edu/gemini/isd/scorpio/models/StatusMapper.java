package edu.gemini.isd.scorpio.models;

import edu.gemini.aspen.giapi.status.StatusItem;

/**
 * This class provides a method to transform the external item into a StatusDTO.
 * In the mapping process the name is translated to a frontend format.
 */
public class StatusMapper {
    public static StatusDTO<Object> trenasformItem(StatusItem<?> item){
        StatusDTO<Object> dtoItem = new StatusDTO<>();

        dtoItem.setId(StatusNameDictionary.translate(item.getName()));
        dtoItem.setValue(item.getValue());
        dtoItem.setTimestamp(item.getTimestamp());

        return dtoItem;
    }
}
