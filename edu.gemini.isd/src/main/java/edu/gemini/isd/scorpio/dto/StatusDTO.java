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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StatusDTO<?>)) return false;
        StatusDTO<?> other = (StatusDTO<?>) obj;
        return java.util.Objects.equals(id, other.id)
                && java.util.Objects.equals(value, other.value)
                && java.util.Objects.equals(timestamp, other.timestamp);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, value, timestamp);
    }

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
