package edu.gemini.isd.scorpio.models;

import edu.gemini.aspen.giapi.status.StatusItem;

public class StatusMapper {

    public static StatusDTO<Object> normalizeItem(StatusItem item){

        StatusDTO<Object> normalizedItem = new StatusDTO<>();

        normalizedItem.setId(StatusNameDictionary.translate(item.getName()));
        normalizedItem.setValue(item.getValue());

        normalizedItem.setTimestamp(item.getTimestamp());

        //Define a status

        return normalizedItem;
    }
}
