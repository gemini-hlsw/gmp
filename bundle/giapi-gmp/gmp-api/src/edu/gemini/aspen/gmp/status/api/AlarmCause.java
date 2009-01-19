package edu.gemini.aspen.gmp.status.api;

/**
 * Cause of the Alarm
 */
public enum AlarmCause {
    ALARM_CAUSE_OK,
    ALARM_CAUSE_HIHI,
    ALARM_CAUSE_HI,
    ALARM_CAUSE_LOLO,
    ALARM_CAUSE_LO,
    ALARM_CAUSE_OTHER;

    public static final AlarmCause DEFAULT = ALARM_CAUSE_OK;
}
