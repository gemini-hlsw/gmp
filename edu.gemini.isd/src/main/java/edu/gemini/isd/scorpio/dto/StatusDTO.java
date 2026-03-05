package edu.gemini.isd.scorpio.dto;

import java.util.Date;

/**
 * DTO to transmit the status to the frontend. Contains a generic value for different types of values.
 * @param <T>
 */
public class StatusDTO<T> {
    private String id;
    private T value;
    private Date timestamp;

    public StatusDTO() {

    }

    public void setId(String id) {
        this.id = id;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }
}
