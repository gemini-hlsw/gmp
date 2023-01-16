package edu.gemini.aspen.gmp.commands.model.executors;

import com.google.common.base.Stopwatch;
import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.aspen.gmp.commands.handlers.CommandHandlers;
import edu.gemini.aspen.gmp.commands.model.*;
import edu.gemini.aspen.gmp.commands.model.ActionMessageBuilder;
import edu.gemini.aspen.gmp.commands.model.impl.ActionManager;
import edu.gemini.aspen.gmp.commands.model.impl.HandlerResponseAnalyzer;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Sequence Command executor for the APPLY Sequence Command. It's job
 * APPLY is more complex than normal sequence commands since an instrument can
 * have more than one handler to deal with a particular APPLY configuration.
 * <br>
 * This executor attempts to decompose the APPLY configuration to find
 * the handler(s) that will process it.
 */
public class ApplySenderExecutor implements SequenceCommandExecutor {
    private static final Logger LOG = Logger.getLogger(ApplySenderExecutor.class.getName());

    private final ActionMessageBuilder _actionMessageBuilder;
    private final ActionManager _actionManager;
    private final CommandHandlers commandHandlers;

    static final String ERROR_MSG = "No configuration present for Apply Sequence command";

    public ApplySenderExecutor(ActionMessageBuilder builder, ActionManager manager, CommandHandlers commandHandlers) {
        _actionMessageBuilder = builder;
        _actionManager = manager;
        this.commandHandlers = commandHandlers;
    }

    @Override
    public HandlerResponse execute(Action action, ActionSender sender) {
        Configuration config = action.getCommand().getConfiguration();
        if (config.isEmpty()) {
            return HandlerResponse.createError(ERROR_MSG);
        } else {

            int expectedResponses = countExpectedResponses(config, ConfigPath.EMPTY_PATH);
            LOG.fine("Action " + action + " expects " + expectedResponses + " responses");

            if (!canBeFullyHandled(config)) {
              LOG.severe("Action " + action + " cannot be fully handled, there are missing handlers. return NOANSWER");
              
              return HandlerResponse.NOANSWER;
            }
            for (int i = 0; i < expectedResponses; i++) {
                _actionManager.increaseRequiredResponses(action);
            }

            return getResponse(action, config, ConfigPath.EMPTY_PATH, sender);
        }
    }

    protected boolean canBeFullyHandled(Configuration config) {
       return _canBeFullyHandled(false, config, ConfigPath.EMPTY_PATH);
    }

    private boolean _canBeFullyHandled(boolean current, Configuration config, ConfigPath path) {

        ConfigPathNavigator navigator = new ConfigPathNavigator(config);
        Set<ConfigPath> configPathSet = navigator.getChildPaths(path);
        boolean ct = current;

        if (configPathSet.isEmpty()) {
            return false;
        }

        //this analyzer will get the result answer from this part of the configuration
        List<ConfigPath> applyHandlers = commandHandlers.getApplyHandlers();
        if (applyHandlers.isEmpty()) return false;

        for (ConfigPath cp : configPathSet) {
            //get the sub-configuration
            Configuration c = config.getSubConfiguration(cp);

            if (applyHandlers.isEmpty() || applyHandlers.contains(cp)) {
                current = true;
            } else {
                // If there are no handlers registered go straight to the sub-handlers
                return _canBeFullyHandled(current, c, cp);
            }
        }
        return current;
    }

    protected int countExpectedResponses(Configuration config, ConfigPath path) {
       return _countExpectedResponses(0, config, path);
    }

    private int _countExpectedResponses(int counter, Configuration config, ConfigPath path) {

        ConfigPathNavigator navigator = new ConfigPathNavigator(config);
        Set<ConfigPath> configPathSet = navigator.getChildPaths(path);
        int ct = counter;

        if (configPathSet.isEmpty()) {
            return 1;
        }

        //this analyzer will get the result answer from this part of the configuration
        List<ConfigPath> applyHandlers = commandHandlers.getApplyHandlers();

        for (ConfigPath cp : configPathSet) {
            //get the sub-configuration
            Configuration c = config.getSubConfiguration(cp);

            if (applyHandlers.isEmpty() || applyHandlers.contains(cp)) {
                ct = ct + 1;
            } else {
                // If there are no handlers registered go straight to the sub-handlers
                return _countExpectedResponses(ct, c, cp);
            }
        }
        return ct;
    }

    /**
     * Auxiliary method to recursively decompose a Configuration to be sent to
     * the appropriate handlers.
     *
     * @param action The action to be sent
     * @param config current configuration being analyzed
     * @param path   current path level in the configuration
     * @param sender A Map Sender object that will send this message and will
     *               get an answer.
     * @return a HandlerResponse representing the result of sending the
     *         configuration. If there are no handlers for this configuration,
     *         this call will try to decompose the configuration in smaller units
     *         in an attempt to see if it can be handled by other handlers.
     */
    private HandlerResponse getResponse(Action action, Configuration config,
                                        ConfigPath path, ActionSender sender) {

        ConfigPathNavigator navigator = new ConfigPathNavigator(config);
        Set<ConfigPath> configPathSet = navigator.getChildPaths(path);

        if (configPathSet.isEmpty()) {
            LOG.info("Action " + action + " has empty path set, respond NOANSWER");
            return HandlerResponse.NOANSWER;
        }

        //this analyzer will get the result answer from this part of the configuration
        HandlerResponseAnalyzer analyzer = new HandlerResponseAnalyzer();
        List<ConfigPath> applyHandlers = commandHandlers.getApplyHandlers();

        for (ConfigPath cp : configPathSet) {
            //get the sub-configuration
            Configuration c = config.getSubConfiguration(cp);

            HandlerResponse response = null;
            if (applyHandlers.isEmpty() || applyHandlers.contains(cp)) {
                LOG.info("Attempt to send apply for configuration " + c + " with id " + action.getId() + " and timeout " + action.getTimeout());
                ActionMessage am = _actionMessageBuilder.buildActionMessage(action, cp);
                Stopwatch s = Stopwatch.createStarted();
                response = sender.send(am, action.getTimeout());
                LOG.finer("Response for apply was " + response + " took " + s.stop().elapsed(TimeUnit.MILLISECONDS) + " [ms]");

                // if the response is COMPLETED remove waiting for a response
                if (response == HandlerResponse.COMPLETED) {
                    LOG.info("Action immediately completed: " + action.getId());
                    _actionManager.decreaseRequiredResponses(action);
                }

                //if there are no handlers, recursively decompose this config in
                //smaller units if possible, and return the answer.
                if (response == HandlerResponse.NOANSWER) {
                    response = getResponse(action, c, cp, sender);
                }

                //if the answer is still NOANSWER, return immediately, there is no one
                //that can process this part of the configuration.
                if (response == HandlerResponse.NOANSWER) {
                    return response;
                }
            } else {
                LOG.finer("No handler for " + cp + " go to next sub-level..");
                // If there are no handlers registered go straight to the sub-handlers
                response = getResponse(action, c, cp, sender);
            }
            analyzer.addResponse(response);
        }
        return analyzer.getSummaryResponse();
    }

}
