package edu.gemini.aspen.gmp.tcsoffset.epics;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import edu.gemini.aspen.giapi.offset.OffsetType;
import edu.gemini.aspen.gmp.tcsoffset.model.*;
import edu.gemini.epics.*;
import edu.gemini.epics.impl.ReadWriteEpicsEnumChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class implements the logic to apply an offset in the IOC TCS.
 * This first version implements the logic to execute an Instrument offset
 * which is the same as gacq applies.
 */
public class EpicsTcsOffsetIOC implements TcsOffsetIOC {

    /**
     * TCS offset channel EPICS IOC 
     */
    public static final String TCS_OFFSET_CHANNEL = "poAdjust";

    /*
     * This attribute or field is declared in the caListDef json object
     * defined in the TcsOffsetComponent-default.cfg configuration file
     */
    public static final String CANAME_ATTR = "caname";

    /*
     * This attribute or field is declared in the caListDef json object
     * defined in the TcsOffsetComponent-default.cfg configuration file
     */
    public static final String EXECUTE_ATTR = "execute";

    /*
     * This attribute or field is declared in the tcsChLoops json object
     * defined in the TcsOffsetComponent-default.cfg configuration file
     */
    public static final String WASOPEN_ATTR = "ifwasopen";

    /*
     * This attribute or field is declared in the tcsChLoops json object
     * defined in the TcsOffsetComponent-default.cfg configuration file
     */
    public static final String CHECK_ATTR = "check";

    /*
     * This attribute or field is declared in the tcsChLoops json object
     * defined in the TcsOffsetComponent-default.cfg configuration file
     */
    public static final String TYPE_ATTR = "type";

    public static final String OPEN_LOOP = "openLoop";

    public static final String CLOSE_LOOP = "closeLoop";

    /**
     * Default tcs simulation 
     */
    public static final String TCS_TOP_SIM = "tc1:";

    private static final Logger LOG = Logger.getLogger(EpicsTcsOffsetIOC.class.getName());
    
    private final EpicsObserver _eo;

    private final JsonObject _jCADef = new JsonObject();

    private  JsonObject _tcsLoops = new JsonObject();

    private EpicsWriter _ew1;


    /**
     * EPICS CA for applying instrument offsets. 
     * _tcsOffsetChannel: By default the value is <tcsTop>:poAdjust
     */
    private final String _tcsOffsetChannel;

    /**
     * EPICS records to read and write the tcsOffsetChannel.
     * _trackingFrameChannel: tcsOffsetChannel.A
     * _offsetSizeChannel   : tcsOffsetChannel.B
     * _angleChannel        : tcsOffsetChannel.C
     * _virtualTelChannel   : _virtualTelChannel.D
     */
    private ReadWriteClientEpicsChannel<String> _trackingFrameChannel;
    private ReadWriteClientEpicsChannel<String> _offsetSizeChannel;
    private ReadWriteClientEpicsChannel<String> _angleChannel;
    private ReadWriteClientEpicsChannel<String> _virtualTelChannel;

    /**
     * HashMap used to store the open and close loops sequence actions. 
     */
    private HashMap<String, ChannelAccess> _caLoops = new HashMap<>();

    private ReadWriteEpicsEnumChannel<Dir> _tcsApply;

    private Boolean _tcsIsInPosition = false;

    private CarState _tcsState;


    /**
     * The _pRegex pattern is used to check if a json value of an attribute
     * has a previous dependency field. This is pointed in the configuration file
     * using the ${filedName}. Please,
     * see the edu.gemini.aspen.gmp.tcsoffset.model.tcsOffsetComponent-default.cfg file
     */
    private Pattern _pRegex = Pattern.compile("\\{\\w+\\}");

    private static final long  FIVE_SECS = 5000; // FIVE SECONDS .
    
    private static final long  ONE_SEC = 1000; // ONE SECOND .

    private static final long  ONE_MIN = 60000; // ONE MINUTE
    
    private static final String FRAME  = "2"; // Instrument offset
    
    private static final String VIRTUAL_TEL  = "-14"; // Virtual Telescope --> SOURCE A

    private static final String P_ANGLE = "90.0";

    private static final String Q_ANGLE ="180.0";

    private TcsStatus _tcsStatus = TcsStatus.OK;
    
    private String _tcsErrorMsg = null;

    private String _tcsTop;

    private static final String _tcsTopConfigKey = "tcsTop";


    /**
     * Constructor. Creates and initializes the EPICS CA necessary to apply the TCS offsets.
     *              To do this, it recieves a json file with the sequence actions to manage
     *              the TCS loops (open and close).
     *
     * @param ew1    : The EPICS Writer service.
     * @param eo     : The EPICS Observer service.
     * @param tcsLoops : Json Object with the actions to manage the TCS Loops
     */
    public EpicsTcsOffsetIOC(EpicsWriter ew1, EpicsObserver eo, JsonObject tcsLoops, JsonObject jCADef)  {

        _tcsTop = getTcsTop(jCADef);
        _tcsOffsetChannel = _tcsTop + TCS_OFFSET_CHANNEL;
        _ew1 = ew1;
        _eo = eo;
        parseJsonObj(tcsLoops, _tcsLoops, tcsLoops);
        parseJsonObj(jCADef, _jCADef, jCADef);
        initializeChannels();
    }

    private String getTcsTop(JsonObject jobj) {
        if (jobj.get(_tcsTopConfigKey) != null)
            return jobj.get(_tcsTopConfigKey).getAsString();
        LOG.warning("The tcsTop has not defined in the configuration file. It would be used tc1 by default");
        return TCS_TOP_SIM;
    }

    /**
     * Initialize all the class attribute to default value. 
     */

    private void setChannelsNull() {
        _trackingFrameChannel = null;
        _offsetSizeChannel = null;
        _angleChannel = null;
        _virtualTelChannel = null;
        _tcsApply = null;
        _caLoops.clear();
    }

    /**
     * Initialize all EPCIS TCS CA, and create the monitors.  
     */

    private boolean initializeChannels() {
        try {
            LOG.fine("Starting to initialize the TCS channels ");

            _trackingFrameChannel = _ew1.getStringChannel(_tcsOffsetChannel + ".A");
            _offsetSizeChannel = _ew1.getStringChannel(_tcsOffsetChannel +".B");
            _angleChannel = _ew1.getStringChannel(_tcsOffsetChannel +".C");
            _virtualTelChannel = _ew1.getStringChannel(_tcsOffsetChannel + ".D");
            _tcsApply = (ReadWriteEpicsEnumChannel<Dir>) _ew1.getEnumChannel( _tcsTop+"apply.DIR", Dir.class);

            // Monitor Channels
            LOG.fine("Creating monitors ");
            _eo.registerEpicsClient(new ChannelAccessSubscribe(this::setTcsInPos, _tcsTop + "inPosCombine"),
                                    ImmutableList.of(_tcsTop + "inPosCombine"));
            _eo.registerEpicsClient(new ChannelAccessSubscribe(this::setTcsStatus, _tcsTop + "applyC"),
                    ImmutableList.of(_tcsTop + "applyC"));

            _eo.registerEpicsClient(new ChannelAccessSubscribe(this::tcsError, _tcsTop + "ErrorVal.VAL"),
                    ImmutableList.of(_tcsTop + "ErrorVal.VAL"));

            _eo.registerEpicsClient(new ChannelAccessSubscribe(this::tcsErrorMsg, _tcsTop + "ErrorMess.VAL"),
                    ImmutableList.of(_tcsTop + "ErrorMess.VAL"));

            initMaps();
            LOG.fine("TCS channels initialized well. The " + _tcsOffsetChannel + " TCS channel is used to apply the offsets");
            return true;
        } catch (EpicsException e) {
            LOG.warning("Problem binding "+ _tcsOffsetChannel +
                         " channel. Check the EPICS configuration and your network settings or check if the TCS is running");
            e.printStackTrace();
            setChannelsNull();
        } catch (TcsOffsetException e) {
            LOG.warning("Error in the configuration file, please fix the problem");
            e.printStackTrace();
            setChannelsNull();
        } 
        LOG.fine("Not initialized the EPICS channels and monitors");
        return false;
    }


    /**
     * Read the json object and creates another one replacing any pattern defined by the user
     * in a previous property. 
     * @param curObj: Current configuration json object analyzed.
     * @param newObj    : New json object with all pattern replacing. 
     * @param objConfig : Configuration json file created by the user. 
     */

    private void parseJsonObj(JsonObject curObj, JsonObject newObj, JsonObject objConfig) {
        Iterator<String> keys = curObj.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (curObj.get(key) instanceof  JsonObject) {
                newObj.add(parseStr(key, objConfig), new JsonObject());
                parseJsonObj(curObj.getAsJsonObject(key), newObj.get(key).getAsJsonObject(), objConfig);
            }else if (curObj.get(key) instanceof JsonArray) {
                JsonArray newArray = new JsonArray();
                JsonArray keyArray = curObj.get(key).getAsJsonArray();
                keyArray.forEach(it -> {
                    if (it.isJsonObject()) {
                        JsonObject obj = new JsonObject();
                        parseJsonObj(it.getAsJsonObject(), obj, objConfig);
                        newArray.add(obj);
                    } else
                        newArray.add(it);
                });

                newObj.add(parseStr(key, objConfig), newArray);
            }
            else {
               String regex = "[0-9]+[\\.]?[0-9]*";
               if (!Pattern.matches(regex, curObj.get(key).getAsString()))
                   newObj.add(parseStr(key, objConfig),
                           new JsonPrimitive(parseStr(curObj.get(key).getAsString(), objConfig)));
               else
                   newObj.add(parseStr(key, objConfig),curObj.get(key));
            }
        }
    }

    private String parseStr(String key, JsonObject objConfig)  {
        Matcher m = _pRegex.matcher(key);
        String key1 = null;
        String newKey = key;
        while (m.find( )) {
            for (int i = 0; i <= m.groupCount(); i++) {
                key1 = m.group(i).replace("{", "").replace("}", "");
                if (objConfig.get(key1) != null) {
                    newKey = newKey.replace((CharSequence) m.group(i), (CharSequence) objConfig.get(key1).getAsString());
                }
                else {
                    LOG.warning("Not found the "+key1 + " key declared previously");
                }
            }
        }
        return newKey;
    }

    /**
     * Initialize the _caLoops hashmap from the _jCADef configuration. The _jCADef is a json object
     * where the user (developer) defined all the CA used to control the loops control.
     */
    private void initMaps() throws TcsOffsetException{
        LOG.fine("Initializing CA");
        Iterator<String> keys = _jCADef.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (_jCADef.get(key).isJsonObject()) {
                JsonObject e = _jCADef.get(key).getAsJsonObject();
                ChannelAccess ca = ChannelAccessFactory.createChannelAccess(e.get(CANAME_ATTR).getAsString(),
                                                        _ew1,
                                                        e.get(TYPE_ATTR).getAsInt());
                ca.connect();
                _caLoops.put(key,ca);
            }
        }
        LOG.fine("End createLoopChannels");
    }

    /**
     * This function is associated to <tcsTop>:ErrorMess.VAL monitor and
     * this is called for any update in this monitor. 
     * @param lMsg: String list with the current values changed. 
     *
     */
    
    private void tcsErrorMsg(List<String> lMsg) {
        for (String e : lMsg) 
            _tcsErrorMsg = e;
    }

    /**
     * This function is associated to <tcsTop>:ErrorVal.VAL monitor and
     * this is called for any update in this monitor.
     * @param lStatus: Short list with the current values changed.
     *
     */

    private void tcsError(List<Short> lStatus) {
        for (Short e : lStatus) {
            _tcsStatus = TcsStatus.getFromInt(e);
        }
    }

    /**
     * This function is associated to <tcsTop>:inPosCombine monitor and
     * this is called for any update in this monitor.
     * @param values: Double list with the current values changed.
     *
     */

    public synchronized void setTcsInPos(List<Double> values) {
       for (Double e : values) {
          _tcsIsInPosition = (e == 1.0);
          notify();
       }
    }

    /**
     * This function is associated to <tcsTop>:applyC monitor and
     * this is called for any update in this monitor. 
     * @param lStatus: Double list with the current values changed.
     *
     */

    public synchronized void setTcsStatus(List<Short> lStatus) {
       for (Short e : lStatus) {
           _tcsState = CarState.getFromInt(e);
           notify();
        }
    }

    private boolean areChannelsInit() {
        if (_trackingFrameChannel != null  &&  _offsetSizeChannel != null
            && _angleChannel != null && _virtualTelChannel != null
            && _tcsApply != null && (!_caLoops.isEmpty())) {
            return true;
        }

        return initializeChannels();
    }

    /**
     * This function waits X milliseconds until the TCS reaches
     * the expected value.
     * @param timeout    : timeout in milliseconds. 
     * @param expectValue: Expected value before X milliseconds pass. 
     *
     * @return           : True, if the expected value was reached before the X milliseconds. 
     *                   : False, if the expected value was not reach before the X milliseconds. 
     */

    private synchronized  boolean waitTcs(long timeout, CarState expectValue) {
        long now = System.currentTimeMillis();
        final long end = now + timeout;
        while ((expectValue != _tcsState) && (now < end)) {
            try {
                wait(end-now);
            } catch (InterruptedException ex) {
                LOG.info("Interrupted while waiting for _tcsState == " + _tcsState);
                break;
            }
            now = System.currentTimeMillis();
        }
        return _tcsState == expectValue;
    }
    /**
     * This function waits X milliseconds until the TCS reaches
     * the expected value.
     * @param timeout    : timeout in milliseconds. 
     * @param inPosition : Expected value before X milliseconds regarding on TCS in position flag.
     *
     * @return           : True, if the expected value was reached before the X milliseconds. 
     *                   : False, if the expected value was not reach before the X milliseconds. 
     */


    private synchronized boolean waitTcsInPos(long timeout, boolean inPosition) {
        long now = System.currentTimeMillis();
        final long end = now + timeout;
        while ((_tcsIsInPosition != inPosition) && (now < end)) {
            try {
                wait(end-now);
            } catch (InterruptedException ex) {
                LOG.info("Interrupted while waiting for TCS in position == " + inPosition);
                break;
            }
            now = System.currentTimeMillis();
        }
        return _tcsIsInPosition == inPosition;
    }

    /**
     * This function waits until the TCS is in position. The TCS in position flag
     * has a overshoot problem. A problem analysis of the problem was performed
     * and this function avoid An analysis of the problem was performed and 
     * this function avoids sending to the instrument that the offset was performed prematurely. 
     *
     * @return           : True, if the tcs is in position. 
     *                   : False, if the tcs is not in position. . 
     */
    private synchronized boolean waitTcsInPosBlinking () throws TcsOffsetException {
        waitTcsInPos(ONE_SEC, false);
        if (!_tcsIsInPosition && (!waitTcsInPos(ONE_MIN, true)))
            throw new TcsOffsetException(TcsOffsetException.Error.TCS_NOT_INPOS, "Tcs is not in position after applying the offset ");

        long t1=0;
        boolean inposOld = _tcsIsInPosition;

        if (_tcsIsInPosition && waitTcsInPos(ONE_SEC, false)) {
            // Blinking the tcsInPosition
            t1 = System.currentTimeMillis();
            while ((inposOld != _tcsIsInPosition) && ( (System.currentTimeMillis()-t1) > FIVE_SECS )) {
                inposOld = _tcsIsInPosition;
                try {
                    Thread.sleep(ONE_SEC);
                } catch (InterruptedException e) {}
            }
            if ((System.currentTimeMillis()-t1) > FIVE_SECS ) {
                return false;
            }
        }
        return true;
    }

    
    /**
     * Execute the TCS apply action and wait until the TCS finish the action.
     * The apply record execution is very fast, and I have never seen that the 
     * 1000 milliseconds is consumed.
     */

    private void tcsApply() throws CAException, TimeoutException {
        LOG.fine("Executing tcsApply");
        _tcsApply.setValue(Dir.START);
        if (!waitTcs(ONE_SEC, CarState.BUSY))
            new TcsOffsetException(TcsOffsetException.Error.TIMEOUT,
                    "TCS was not reached the BUSY state after applying the apply");
        if (!waitTcs(ONE_SEC, CarState.IDLE))
            new TcsOffsetException(TcsOffsetException.Error.TIMEOUT,
                    "TCS was not reached the IDLE state after applying the apply");
    }


    /**
     * Set the TCS offset values in the TCS CA (<tcsTop>:poAdjust.X).
     * @param val        :  P or Q value.
     * @param offsetAngle:  90  -> p
     *                      180 -> q
     */

    private void applyOffset(String val, String offsetAngle) throws CAException, TimeoutException {
        _trackingFrameChannel.setValue(FRAME);
        _offsetSizeChannel.setValue(val);
        _angleChannel.setValue(offsetAngle);
        _virtualTelChannel.setValue(VIRTUAL_TEL);
        tcsApply();
    }


    /**
     * This function manages the open or close loop sequence actions. The open and close sequence actions
     * are defined in the configuration file. 
     * @param loopKey: Indicates the loop sequence which will be executed.
     */

    private synchronized void iterateSequence(String loopKey) throws CAException, TimeoutException, TcsOffsetException {
        LOG.fine("Starting the " + loopKey +" sequence.");
        JsonArray keyArray = _tcsLoops.get(loopKey).getAsJsonArray();
        Iterator<JsonElement> it = _tcsLoops.get(loopKey).getAsJsonArray().iterator();
        while (it.hasNext()) {
            JsonObject jObj = it.next().getAsJsonObject();
            boolean executeAction = true;
            // Check the different attributes defined in the tcsChLoops sequence json file
            // and execute the action associated to them.
            if (jObj.has(CHECK_ATTR)) {
                JsonObject check = jObj.get(CHECK_ATTR).getAsJsonObject();
                Iterator<String> it2  = check.keySet().iterator();
                while (executeAction && it2.hasNext()) {
                    String key = it2.next();
                    executeAction = _caLoops.get(key).check(check.get(key));
                }
            } else if (jObj.has(WASOPEN_ATTR)) {
                Iterator<JsonElement> it5 = jObj.get(WASOPEN_ATTR).getAsJsonArray().iterator();
                while (executeAction && it5.hasNext()) {
                    String keyMark = it5.next().getAsString();
                    executeAction = _caLoops.get(keyMark).isMark();
                }
            } else {
                Iterator<String> it3 = jObj.keySet().iterator();
                while(it3.hasNext()){
                    String key = it3.next();
                    executeSpecialCmd(key);
                }
                executeAction = false;
            }
            // if the command defined in the tcsChLoops json object is not a special command (tcs:apply)
            // the below code will set the the code below will set the value of that AC proper to perform
            // the action required in this step of the sequence.
            if (executeAction) {
                Iterator<JsonElement> it4 = jObj.get(EXECUTE_ATTR).getAsJsonArray().iterator();
                while (it4.hasNext()){
                    JsonObject obj = it4.next().getAsJsonObject();
                    Iterator<String> keys = obj.keySet().iterator();
                    while(keys.hasNext()) {
                        String execKey = keys.next();
                        _caLoops.get(execKey).setValue(obj.get(execKey));
                        boolean mark = loopKey.equals(OPEN_LOOP) ? true : false;
                        _caLoops.get(execKey).setMark(mark);
                    }
                }
            }
        }
        if (_tcsState == CarState.ERROR || _tcsStatus == TcsStatus.ERR)
            throw new TcsOffsetException(TcsOffsetException.Error.TCS_STATE, _tcsErrorMsg);
        LOG.fine("End the : " + loopKey + " sequence. TCS_STATE: " + _tcsState + " TCS_STATUS: " + _tcsStatus);
    }

    private void executeSpecialCmd(String epicsCA) throws CAException, TimeoutException, TcsOffsetException {
        switch (epicsCA) {
            case "tcsApply":
                tcsApply();
                break;
            default:
                LOG.severe("Special EpicsCA has not been implemented");
                throw new TcsOffsetException(TcsOffsetException.Error.CONFIGURATION_FILE,
                        "The tcsChLoops configuration is wrong. The " + epicsCA + " is not implemented.");
        }
    }

    /**
     * Apply the P and Q offsets provide by the instrument.
     * @param p         : P offset value. Units arcseconds.
     * @param q         : Q offset value. Units arcseconds.
     */

    @Override
    public void setTcsOffset(double p, double q, OffsetType offsetType) throws TcsOffsetException {
        LOG.fine("Setting offset  p: " + p + " q: " + q + " -14");
        if (!areChannelsInit())
            throw new TcsOffsetException(TcsOffsetException.Error.BINDINGCHANNEL,
                                         "Problem binding " + _tcsOffsetChannel +
                                         ".[A|B|C|D] channel. Check the " + EpicsTcsOffsetIOC.class.getName()
                                         + "-default.cfg configuration file and your network settings");

        if (_tcsState == CarState.ERROR || _tcsStatus == TcsStatus.ERR)
            throw new TcsOffsetException(TcsOffsetException.Error.TCS_STATE,
                    "There is an error in the TCS, please clean the TCS before continue. ");

        synchronized (this) {
            if (!_tcsIsInPosition && (!waitTcsInPos(FIVE_SECS, true)))
                throw new TcsOffsetException(TcsOffsetException.Error.TCS_NOT_INPOS,
                        "Tcs is not in position before applying the offset ");
        }
        try {
            if (offsetType == OffsetType.ACQ)
                iterateSequence(OPEN_LOOP);
            // Applying P offset
            applyOffset(Double.toString(p), P_ANGLE);
            // Applying Q offset
            applyOffset(Double.toString(q), Q_ANGLE);
            if (offsetType == OffsetType.ACQ) {
                waitTcsInPosBlinking();
                iterateSequence(CLOSE_LOOP);
            }

        } catch (CAException  e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            setChannelsNull();
            throw new TcsOffsetException(TcsOffsetException.Error.TIMEOUT,
                                         "Timeout Error trying to set the TCS epics channel. Please check the TCS status", e);
        } catch (TimeoutException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            throw new TcsOffsetException(TcsOffsetException.Error.TIMEOUT,
                                         "Timeout Error trying to set the TCS epics channel. Please check the TCS status", e);
        } catch (IllegalStateException e) {  // when the TCS is rebooted, it is necessary to force the Epics channels initialization.
                                             // Therefore, the channels are set to null and the next request the service will try
                                             // initialized them.
            LOG.log(Level.WARNING, e.getMessage(), e);
            setChannelsNull();
            throw new TcsOffsetException(TcsOffsetException.Error.TCS_WAS_REBOOTED,
                                          "It is necessary to apply the offset again", e);
        }

    }
}
