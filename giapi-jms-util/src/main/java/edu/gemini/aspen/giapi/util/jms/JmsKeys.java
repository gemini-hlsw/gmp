package edu.gemini.aspen.giapi.util.jms;

/**
 *
 */
public class JmsKeys {

    //Common keys
    public final static String GMP_PREFIX = "GMP";
    public final static String GMP_SEPARATOR = ".";


    //Sequence Command Keys
    public final static String GMP_COMPLETION_INFO = GMP_PREFIX + GMP_SEPARATOR + "COMPLETION_INFO";
    public final static String GMP_SEQUENCE_COMMAND_PREFIX = GMP_PREFIX + GMP_SEPARATOR + "SC" + GMP_SEPARATOR;
    public final static String GMP_ACTIVITY_PROP = "GMP_ACTIVITY_PROP";
    public final static String GMP_ACTIONID_PROP = "GMP_ACTIONID";


    //Handler Response Keys
    public final static String GMP_HANDLER_RESPONSE_KEY = "GMP_HANDLER_RESPONSE";
    public final static String GMP_HANDLER_RESPONSE_ERROR_KEY = "GMP_HANDLER_RESPONSE_ERROR";

    public final static String GMP_SEQUENCE_COMMAND_KEY = "GMP_SEQUENCE_COMMAND";
    public final static String GMP_ACTIVITY_KEY = "GMP_ACTIVITY";


    //Services Requests keys
    public final static String GMP_UTIL_REQUEST_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "UTIL_REQUEST_DESTINATION";
    public final static String GMP_UTIL_REQUEST_TYPE = "REQUEST_TYPE";
    public final static int GMP_UTIL_REQUEST_PROPERTY = 0;
    public final static String GMP_UTIL_PROPERTY = "PROPERTY";

    //Logging Keys
    public final static String GMP_SERVICES_LOG_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "LOGGING_DESTINATION";
    public final static String GMP_SERVICES_LOG_LEVEL = "LEVEL";

    //Status Keys
    public final static String GMP_STATUS_DESTINATION_PREFIX = GMP_PREFIX + GMP_SEPARATOR + "STATUS" + GMP_SEPARATOR;

    //Heartbeat Keys
    public final static String GMP_HEARTBEAT_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "HEARTBEAT";

    //Gateway Keys

    private final static String GW_PREFIX = "GW";

    //Gateway Status Keys
    public final static String GW_STATUS_REQUEST_DESTINATION = GW_PREFIX + GMP_SEPARATOR + "STATUS_REQUEST";
    public final static String GW_STATUS_REQUEST_TYPE_PROPERTY = "STATUS_REQUEST_TYPE";
    public final static String GW_STATUS_REQUEST_TYPE_ITEM = "STATUS_REQUEST_TYPE_ITEM";
    public final static String GW_STATUS_REQUEST_TYPE_NAMES = "STATUS_REQUEST_TYPE_NAMES";
    public final static String GW_STATUS_REQUEST_TYPE_ALL = "STATUS_REQUEST_TYPE_ALL";


    //Gateway Command Keys
    public final static String GW_COMMAND_TOPIC = GW_PREFIX + GMP_SEPARATOR + "SC";
    public final static String GW_COMMAND_REPLY_QUEUE = GW_PREFIX + GMP_SEPARATOR + "COMMAND_REPLY";


    //EPICS Interface Keys
    public final static String GMP_GEMINI_EPICS_REQUEST_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "EPICS_REQUEST_DESTINATION";
    public final static String GMP_GEMINI_EPICS_CHANNEL_PROPERTY = "EPICS_CHANNEL";
    public final static String GMP_GEMINI_EPICS_TOPIC_PREFIX = GMP_PREFIX + GMP_SEPARATOR + "EPICS" + GMP_SEPARATOR;
    public final static String GMP_GEMINI_EPICS_GET_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "EPICS_GET_DESTINATION";

    //Observation Events Keys
    public final static String GMP_DATA_OBSEVENT_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "OBSEVENT_DESTINATION";
    public final static String GMP_DATA_OBSEVENT_NAME = "OBSEVENT_NAME";
    public final static String GMP_DATA_OBSEVENT_FILENAME = "OBSEVENT_FILENAME";
    //File Events Keys
    public final static String GMP_DATA_FILEEVENT_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "FILEEVENT_DESTINATION";
    public final static String GMP_DATA_FILEEVENT_TYPE = "TYPE";
    public final static String GMP_DATA_FILEEVENT_FILENAME = "FILENAME";
    public final static String GMP_DATA_FILEEVENT_DATALABEL = "DATALABEL";
    public final static String GMP_DATA_FILEEVENT_HINT = "HINT";


    //PCS Updates Keys
    public final static String GMP_PCS_UPDATE_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "PCS_UPDATE_DESTINATION";

    //TCS Context Keys
    public final static String GMP_TCS_CONTEXT_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "TCS_CONTEXT_DESTINATION";

    //TCS Offset Keys
    public final static String GMP_TCS_OFFSET_DESTINATION = GMP_PREFIX + GMP_SEPARATOR + "TCS_OFFSET_DESTINATION";

}
