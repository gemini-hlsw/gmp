package edu.gemini.isd.scorpio.models;

import java.util.Date;

public class StatusDTO<T> {
    private String id;
    private T value;
    private DataStatus status;
    private Date timestamp;

    public StatusDTO() {

    }

    public void setId(String id) {
        this.id = id;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setStatus(DataStatus status) {
        this.status = status;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
