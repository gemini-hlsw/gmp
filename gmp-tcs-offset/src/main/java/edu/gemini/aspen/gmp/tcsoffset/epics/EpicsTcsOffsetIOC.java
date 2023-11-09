package edu.gemini.aspen.gmp.tcsoffset.epics;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import edu.gemini.epics.*;
import edu.gemini.epics.impl.ReadWriteEpicsEnumChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.gemini.aspen.gmp.tcsoffset.model.TcsOffsetIOC;
import edu.gemini.aspen.gmp.tcsoffset.model.Dir;
import edu.gemini.aspen.gmp.tcsoffset.model.CARSTATE;
import edu.gemini.aspen.gmp.tcsoffset.model.TCSSTATUS;
import edu.gemini.aspen.gmp.tcsoffset.model.TcsOffsetException;
import edu.gemini.aspen.gmp.tcsoffset.model.ChannelAccessSubscribe;



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

    /**
     * EPCIS CA which Indicates if the TCS is in Position. 
     */
    public static final String TCS_INPOSITION_CHANNEL = "sad:inPosition";
    
    /**
     * Default tcs simulation 
     */
    public static final String TCS_TOP_SIM = "tc1";

    private static final Logger LOG = Logger.getLogger(EpicsTcsOffsetIOC.class.getName());
    
    private final EpicsObserver _eo;

    private  JsonObject _tcsChLoops = new JsonObject();

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
    private HashMap<String, ReadWriteClientEpicsChannel<String>> _chLoops;

    private ReadWriteEpicsEnumChannel<Dir> _tcsApply;

    private Boolean _tcsIsInPosition;

    private CARSTATE _tcsState;

    private boolean _isWaitingForState = false;

    private boolean _isWaitingForInPos = false;

    /**
     * The _pRegex pattern is used to check if a json value of an attribute
     * has a previous dependency field. This is pointed in the configuration file
     * using the ${filedName}. Please,
     * see the edu.gemini.aspen.gmp.tcsoffset.model.tcsOffsetComponent-default.cfg file
     */
    private Pattern _pRegex = Pattern.compile("\\{\\w+\\}");

    private static final int  FIVE_SECS = 5000; // FIVE SECONDS . 
    
    private static final int  ONE_SEC = 1000; // ONE SECOND . 

    private static final int  ONE_MIN = 60000; // ONE MINUTE
    
    private static final String FRAME  = "2"; // Instrument offset
    
    private static final String VIRTUAL_TEL  = "-14"; // Virtual Telescope --> SOURCE A
    
    private TCSSTATUS _tcsStatus;
    
    private String _tcsErrorMsg;

    private String _tcsTop;

    private static final String _tcsTopConfigKey = "tcsTop";


    /**
     * Constructor. Creates and initiliazes the EPICS CA necessary to apply the TCS offsets. 
     *              To do this, it recieves a json file with the sequence actions to manage
     *              the TCS loops (open and close). t 
     *
     * @param ew1    : The EPICS Writer service.
     * @param eo     : The EPICS Observer service.
     * @param config : Json Object with the actions to manage the TCS Loops
     */
    public EpicsTcsOffsetIOC(EpicsWriter ew1, EpicsObserver eo, JsonObject config)  {


        _tcsTop = getTcsTop(config);
        _tcsOffsetChannel = _tcsTop + TCS_OFFSET_CHANNEL;
        _ew1 = ew1;
        _eo = eo;
        _tcsIsInPosition = false;
        _isWaitingForState = false;
        _tcsStatus = TCSSTATUS.OK;
        _tcsErrorMsg = null;
        _chLoops = new HashMap<>();
        parseJsonObj(config, _tcsChLoops, config);
        initializeChannels();
    }

    private String getTcsTop(JsonObject tcsChLoops) {
        if (tcsChLoops.get(_tcsTopConfigKey) != null)
            return tcsChLoops.get(_tcsTopConfigKey).getAsString();
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
        _chLoops.clear();
        _chLoops.clear();
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
            _eo.registerEpicsClient(new ChannelAccessSubscribe(this::setTcsInPos, _tcsTop + "inPosCombine"),
                                    ImmutableList.of(_tcsTop + "inPosCombine"));
            _eo.registerEpicsClient(new ChannelAccessSubscribe(this::setTcsStatus, _tcsTop + "applyC"),
                    ImmutableList.of(_tcsTop + "applyC"));

            _eo.registerEpicsClient(new ChannelAccessSubscribe(this::tcsError, _tcsTop + "ErrorVal.VAL"),
                    ImmutableList.of(_tcsTop + "ErrorVal.VAL"));

            _eo.registerEpicsClient(new ChannelAccessSubscribe(this::tcsErrorMsg, _tcsTop + "ErrorMess.VAL"),
                    ImmutableList.of(_tcsTop + "ErrorMess.VAL"));

            // Init the _chLoops hash map. 
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
     * @param tcsChLoops: Current configuration json object analyzed. 
     * @param newObj    : New json object with all pattern replacing. 
     * @param objConfig : Configuration json file created by the user. 
     */

    private void parseJsonObj(JsonObject tcsChLoops, JsonObject newObj, JsonObject objConfig) {
        Iterator<String> keys = tcsChLoops.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (tcsChLoops.get(key) instanceof  JsonObject) {
                newObj.add(createNewEntryKey(key, objConfig), new JsonObject());
                parseJsonObj(tcsChLoops.getAsJsonObject(key), newObj.get(key).getAsJsonObject(), objConfig);
            }
            else {
                newObj.add(createNewEntryKey(key, objConfig), tcsChLoops.get(key));
            }
        }
    }

    private String createNewEntryKey(String key, JsonObject objConfig)  {
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
     * Initialize the _chLoops hashmap from the _tcsChLoops configuration json file
     * defined by the user. 
     */
    private void initMaps() throws TcsOffsetException{
        createLoopChannels("openLoop", _chLoops);
        createLoopChannels("closeLoop", _chLoops);
        Set<String> keys = _chLoops.keySet();
        for (String key : keys) {
            ReadWriteClientEpicsChannel<String> epicsChannel = _chLoops.get(key);
        }
    }

    private void createLoopChannels(String loopKey, 
		                    HashMap<String, ReadWriteClientEpicsChannel<String>> map) throws TcsOffsetException {
        if (_tcsChLoops.get(loopKey) == null)
            throw new TcsOffsetException(TcsOffsetException.Error.CONFIGURATION_FILE,
                                   "Error, There is not the " + loopKey +
                                   " declared in the tcsChLoops json configuration");

        Iterator<String> keysLoop = _tcsChLoops.get(loopKey).getAsJsonObject().keySet().iterator();
        while (keysLoop.hasNext()) {
            String key = keysLoop.next();
            if (!key.contains("$"))
                map.put(key, _ew1.getStringChannel(key));
        }
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
            _tcsStatus = TCSSTATUS.getFromInt(e);
        }
    }

    /**
     * This function is associated to <tcsTop>:inPosCombine monitor and
     * this is called for any update in this monitor. 
     * @param values: Double list with the current values changed.
     *
     */

    public void setTcsInPos(List<Double> values) {
        for (Double e : values) {
            synchronized (this) {
                _tcsIsInPosition = (e == 1.0);
                if (_isWaitingForInPos)
                    this.notify();
            }
        }
    }

    /**
     * This function is associated to <tcsTop>:applyC monitor and
     * this is called for any update in this monitor. 
     * @param lStatus: Double list with the current values changed.
     *
     */

    public void setTcsStatus(List<Short> lStatus) {
        for (Short e : lStatus) {
            synchronized (this) {
                _tcsState = CARSTATE.getFromInt(e);
                if (_isWaitingForState)
                    this.notify();
            }
        }
    }

    private boolean areChannelsInit() {
        if (_trackingFrameChannel != null  &&  _offsetSizeChannel != null
            && _angleChannel != null && _virtualTelChannel != null
            && _tcsApply != null && (!_chLoops.isEmpty())) {
            return true;
        }

        return initializeChannels();
    }


    private boolean waitChange(int timeout) {
        try {
            this.wait(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
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

    private boolean waitTcs(int timeout, CARSTATE expectValue) {
        long rest, t1=0;

        if (expectValue == _tcsState)
            return true;
        synchronized (this) {
            t1 = System.currentTimeMillis();
            _isWaitingForState = true;
            waitChange(timeout);
           _isWaitingForState = false;
        }
        rest = System.currentTimeMillis() - t1;

        if (_tcsState != expectValue) {
            return false;
        }
        return true;
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

    private boolean waitTcsInPos(int timeout,  Boolean inPosition) {
        if (inPosition == _tcsIsInPosition)
            return true;
        synchronized (this) {
            _isWaitingForInPos = true;
            waitChange(timeout);
            _isWaitingForInPos = false;
        }
        if (inPosition != _tcsIsInPosition)
            return false;
        return true;
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
    private boolean waitTcsInPosBlinking () throws TcsOffsetException {
        waitTcsInPos(ONE_SEC, false);
        if (!_tcsIsInPosition && (!waitTcsInPos(ONE_MIN, true)))
            throw new TcsOffsetException(TcsOffsetException.Error.TCS_NOT_INPOS, "Tcs is not in position after applying the offset ");
        long t1=0;
        long rest=0;
        boolean inposOld = _tcsIsInPosition;
        if (_tcsIsInPosition && waitTcsInPos(ONE_SEC, false)) {
            // Blinking the tcsInPosition
            t1 = System.currentTimeMillis();
            int i=0;
            while ((inposOld != _tcsIsInPosition) && ( (System.currentTimeMillis()-t1) > FIVE_SECS )) {
                inposOld = _tcsIsInPosition;
                try {
                    Thread.sleep(ONE_SEC);
                } catch (InterruptedException e) {}
                i++;
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
        _tcsApply.setValue(Dir.START);
        if (!waitTcs(ONE_SEC, CARSTATE.BUSY))
            new TcsOffsetException(TcsOffsetException.Error.TIMEOUT,
                    "TCS was not reached the BUSY state after applying the apply");
        if (!waitTcs(ONE_SEC, CARSTATE.IDLE))
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
    private void iterateSequence(String loopKey) throws CAException, TimeoutException, TcsOffsetException {
        Iterator<String> keysLoop = _tcsChLoops.get(loopKey).getAsJsonObject().keySet().iterator();
        LOG.fine("Starting the " + loopKey +" sequence.");
        int indexCallFunc = -1;
        while (keysLoop.hasNext()) {
            String key = keysLoop.next();
            String val = _tcsChLoops.get(loopKey).getAsJsonObject().get(key).toString().replace("\"","");
            indexCallFunc = key.indexOf("$");
            if (indexCallFunc == -1) {
                if (_chLoops.get(key) != null)
                    _chLoops.get(key).setValue(val);
                else
                    LOG.warning("The next command "+key+" can not be applied");
            }
            else {
                String methodName = key.substring(indexCallFunc+1, key.length());
                try {
                    Method method = this.getClass().getDeclaredMethod(methodName);
                    method.invoke(this);
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e ) {
                    e.printStackTrace();
                    throw new TcsOffsetException(TcsOffsetException.Error.CONFIGURATION_FILE,
                            "The tcsChLoops configuration is wrong. The "+ methodName+ " is not implemented.", e);
                }
            }
        }
        LOG.fine("End the : " + loopKey + " sequence. TCS_STATE: " + _tcsState + " TCS_STATUS: " + _tcsStatus);
    }
   
    /**
     * Apply the P and Q offsets provide by the instrument.
     * @param p         : P offset value. Units arcseconds. 
     * @param q         : Q offset value. Units arcseconds. 
     */

    @Override
    public void setTcsOffset(double p, double q) throws TcsOffsetException {
        LOG.fine("Setting offset  p: "+ p + " q: " + q +" -14");
        if (!areChannelsInit())
            throw new TcsOffsetException(TcsOffsetException.Error.BINDINGCHANNEL,
                                         "Problem binding " + _tcsOffsetChannel +
                                         ".[A|B|C|D] channel. Check the " + EpicsTcsOffsetIOC.class.getName()
                                         + "-default.cfg configuration file and your network settings");

        if (_tcsState == CARSTATE.ERROR || _tcsStatus == TCSSTATUS.ERR)
            throw new TcsOffsetException(TcsOffsetException.Error.TCS_STATE,
                                         "There is an error in the TCS, please clean the TCS before continue. ");

        if (!_tcsIsInPosition && (!waitTcsInPos(FIVE_SECS, true)))
            throw new TcsOffsetException(TcsOffsetException.Error.TCS_NOT_INPOS,
                                         "Tcs is not in position before applying the offset ");
        try {
            iterateSequence("openLoop");
            // Applying P offset
            applyOffset(Double.toString(p), "90.0");
            // Applying Q offset
            applyOffset(Double.toString(q), "180.0");
            waitTcsInPosBlinking();
            iterateSequence("closeLoop");
            if (_tcsState == CARSTATE.ERROR || _tcsStatus == TCSSTATUS.ERR)
                throw new TcsOffsetException(TcsOffsetException.Error.TCS_STATE, _tcsErrorMsg);

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
