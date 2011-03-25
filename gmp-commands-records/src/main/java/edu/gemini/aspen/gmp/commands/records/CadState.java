package edu.gemini.aspen.gmp.commands.records;

import edu.gemini.aspen.giapi.commands.*;
import gov.aps.jca.CAException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
* This enum represent the three possible states a CAD record can be in, manages the transitions, and sends the commands.
*
* @author Nicolas A. Barriga
*         Date: 3/24/11
*/
enum CadState {
    CLEAR {
        @Override
        public CadState processDir(Dir dir, EpicsCad epicsCad, CommandSender cs, SequenceCommand seqCom, CarRecord car) {
            car.setBusy(epicsCad.getClid());
            car.setIdle(epicsCad.getClid());
            epicsCad.setVal(0);
            try {
                epicsCad.post();
            } catch (CAException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
            }
            switch (dir) {
                case MARK:
                    return MARKED;
                case CLEAR:
                    return CLEAR;
                case PRESET:
                case START:
                case STOP:
                    //do nothing
                    return CLEAR;
                default://just so the compiler doesn't complain
                    return CLEAR;
            }
        }
    },
    MARKED {
        @Override
        public CadState processDir(Dir dir, EpicsCad epicsCad, CommandSender cs, SequenceCommand seqCom, CarRecord car) {
            car.setBusy(epicsCad.getClid());

            HandlerResponse resp;
            switch (dir) {
                case MARK:
                    epicsCad.setVal(0);
                    try {
                        epicsCad.post();
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                    }
                    car.setIdle(epicsCad.getClid());
                    return MARKED;
                case CLEAR:
                    epicsCad.setVal(0);
                    try {
                        epicsCad.post();
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                    }
                    car.setIdle(epicsCad.getClid());
                    return CLEAR;
                case PRESET:
                    resp = doActivity(Activity.PRESET, cs, seqCom, epicsCad.getClid(), car);
                    try {
                        if (resp.getResponse().equals(HandlerResponse.Response.ACCEPTED)) {
                            epicsCad.setVal(0);
                            epicsCad.post();
                            car.setIdle(epicsCad.getClid());
                            return IS_PRESET;
                        } else {
                            epicsCad.setVal(-1);
                            epicsCad.setMess(resp.getMessage());
                            epicsCad.post();
                            car.changeState(CarRecord.Val.ERR, resp.getMessage(), -1, epicsCad.getClid());
                            return CLEAR;
                        }
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                        return CLEAR;
                    }
                case START:
                    resp = doActivity(Activity.PRESET_START, cs, seqCom, epicsCad.getClid(), car);
                    try {
                        if (resp.getResponse().equals(HandlerResponse.Response.STARTED)) {
                            epicsCad.setVal(0);
                            epicsCad.post();
                        } else if (resp.getResponse().equals(HandlerResponse.Response.COMPLETED)) {
                            epicsCad.setVal(0);
                            epicsCad.post();
                            car.setIdle(epicsCad.getClid());
                        } else {
                            epicsCad.setVal(-1);
                            epicsCad.setMess(resp.getMessage());
                            epicsCad.post();
                            car.changeState(CarRecord.Val.ERR, resp.getMessage(), -1, epicsCad.getClid());
                        }
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                    }
                    return CLEAR;
                case STOP:
                    resp = doActivity(Activity.CANCEL, cs, seqCom, epicsCad.getClid(), car);
                    try {
                        if (resp.getResponse().equals(HandlerResponse.Response.ACCEPTED)) {
                            epicsCad.setVal(0);
                            epicsCad.post();
                            car.setIdle(epicsCad.getClid());
                        } else {
                            epicsCad.setVal(-1);
                            epicsCad.setMess(resp.getMessage());
                            epicsCad.post();
                            car.changeState(CarRecord.Val.ERR, resp.getMessage(), -1, epicsCad.getClid());
                        }
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                    }
                    return CLEAR;
                default://just so the compiler doesn't complain
                    return CLEAR;
            }
        }
    },
    IS_PRESET {
        @Override
        public CadState processDir(Dir dir, EpicsCad epicsCad, CommandSender cs, SequenceCommand seqCom, CarRecord car) {
            car.setBusy(epicsCad.getClid());
            HandlerResponse resp;
            switch (dir) {
                case MARK:
                    epicsCad.setVal(0);
                    try {
                        epicsCad.post();
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                    }
                    return MARKED;
                case CLEAR:
                    epicsCad.setVal(0);
                    try {
                        epicsCad.post();
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                    }
                    return CLEAR;
                case PRESET:
                    resp = doActivity(Activity.PRESET, cs, seqCom, epicsCad.getClid(), car);
                    try {
                        if (resp.getResponse().equals(HandlerResponse.Response.ACCEPTED)) {
                            epicsCad.setVal(0);
                            epicsCad.post();
                            car.setIdle(epicsCad.getClid());
                            return IS_PRESET;
                        } else {
                            epicsCad.setVal(-1);
                            epicsCad.setMess(resp.getMessage());
                            epicsCad.post();
                            car.changeState(CarRecord.Val.ERR, resp.getMessage(), -1, epicsCad.getClid());
                            return CLEAR;
                        }
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                        return CLEAR;
                    }
                case START:
                    resp = doActivity(Activity.START, cs, seqCom, epicsCad.getClid(), car);
                    try {
                        if (resp.getResponse().equals(HandlerResponse.Response.STARTED)) {
                            epicsCad.setVal(0);
                            epicsCad.post();
                        } else if (resp.getResponse().equals(HandlerResponse.Response.COMPLETED)) {
                            epicsCad.setVal(0);
                            epicsCad.post();
                            car.setIdle(epicsCad.getClid());
                        } else {
                            epicsCad.setVal(-1);
                            epicsCad.setMess(resp.getMessage());
                            epicsCad.post();
                            car.changeState(CarRecord.Val.ERR, resp.getMessage(), -1, epicsCad.getClid());
                        }
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                    }
                    return CLEAR;
                case STOP:
                    resp = doActivity(Activity.CANCEL, cs, seqCom, epicsCad.getClid(), car);
                    try {
                        if (resp.getResponse().equals(HandlerResponse.Response.ACCEPTED)) {
                            epicsCad.setVal(0);
                            epicsCad.post();
                            car.setIdle(epicsCad.getClid());
                        } else {
                            epicsCad.setVal(-1);
                            epicsCad.setMess(resp.getMessage());
                            epicsCad.post();
                            car.changeState(CarRecord.Val.ERR, resp.getMessage(), -1, epicsCad.getClid());
                        }
                    } catch (CAException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);  //To change body of catch statement use File | Settings | File Templates.
                    }
                    return CLEAR;
                default://just so the compiler doesn't complain
                    return CLEAR;
            }
        }
    };
    private static final Logger LOG = Logger.getLogger(CadState.class.getName());

    /**
     * Process a given directive, send a command, update CAD and CAR.
     *
     * @param dir directive to process
     * @param epicsCad the CAD that received the directive
     * @param cs the command sender to use
     * @param seqCom the sequence command this CAD represents
     * @param car the CAR associated with the given CAD
     * @return the new state after processing the directive
     */
    public abstract CadState processDir(Dir dir, EpicsCad epicsCad, CommandSender cs, SequenceCommand seqCom, CarRecord car);

    private static HandlerResponse doActivity(Activity activity, CommandSender cs, SequenceCommand seqCom, Integer id, CarRecord car) {
        HandlerResponse resp = cs.sendCommand(new Command(seqCom, activity), new CadCompletionListener(id, car));
        LOG.info("Activity: "+activity+" ClientID: "+id+" Response: "+resp.getResponse().toString());
        return resp;

    }
}
