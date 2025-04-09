package edu.gemini.aspen.gmp.commands.model.executors;

import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.commands.SequenceCommand;
import edu.gemini.aspen.giapi.status.impl.BasicStatus;
import edu.gemini.aspen.giapi.status.setter.StatusSetter;
import edu.gemini.aspen.gmp.commands.handlers.CommandHandlers;
import edu.gemini.aspen.gmp.commands.model.Action;
import edu.gemini.aspen.gmp.commands.model.ActionMessageBuilder;
import edu.gemini.aspen.gmp.commands.model.ActionSender;
import edu.gemini.aspen.gmp.commands.model.SequenceCommandException;
import edu.gemini.aspen.gmp.commands.model.impl.ActionManager;
import edu.gemini.aspen.gmp.commands.model.reboot.LinuxRebootManager;
import edu.gemini.gmp.top.Top;

import javax.jms.JMSException;
import java.util.logging.Logger;

/**
 * This is a high order Sequence Command Executor. It will delegate
 * the actual execution to a more specific executor.
 * <p/>
 * As this class is exported as an OSGi service it will be used by ActionManager
 */
public class SequenceCommandExecutorStrategy implements SequenceCommandExecutor {
    private static final Logger LOG = Logger.getLogger(SequenceCommandExecutorStrategy.class.getName());

    private final SequenceCommandExecutor _defaultExecutor;
    private final SequenceCommandExecutor _applyExecutor;
    private final StatusSetter statusSetter;
    private final String currentCommandStatus;
    private final String lastCommandStatus;
    private final String currentCommandTimestamp;
    private SequenceCommandExecutor _rebootExecutor;

    private Action lastAction = null;

    /**
     * Construct the executor specifying the ActionMessageBuilder to use.
     *
     * @param builder ActionMessageBuilder to be used.
     * @param manager the Action Manager that keeps track of the actions
     */
    public SequenceCommandExecutorStrategy(ActionMessageBuilder builder,
                                           ActionManager manager,
                                           CommandHandlers commandHandlers,
                                           StatusSetter statusSetter,
                                           Top top,
                                           String instrumentStartupScript) {
        this.statusSetter = statusSetter;
        currentCommandStatus = top.buildStatusItemName("gmp:currentCommand");
        lastCommandStatus = top.buildStatusItemName("gmp:lastCommand");
        currentCommandTimestamp = top.buildStatusItemName("gmp:currentCommandTimestamp");
        _defaultExecutor = new DefaultSenderExecutor(builder);
        _applyExecutor = new ApplySenderExecutor(builder, manager, commandHandlers);
        _rebootExecutor = new RebootSenderExecutor(new LinuxRebootManager(instrumentStartupScript));
    }

    @Override
    public HandlerResponse execute(Action action, ActionSender sender) {
        if (action == null) {
            throw new SequenceCommandException("Null action received for execution");
        }

        Command command = action.getCommand();
        try {
            statusSetter.setStatusItem(new BasicStatus<String>(currentCommandStatus, formatCommand(action)));
            statusSetter.setStatusItem(new BasicStatus<Integer>(currentCommandTimestamp, (int)System.currentTimeMillis()));
            if (lastAction != null) {
                statusSetter.setStatusItem(new BasicStatus<String>(lastCommandStatus, formatCommand(lastAction)));
            }
        } catch (JMSException e) {
            LOG.warning("Exception publishing command status " + e.getMessage());
        }
        lastAction = action;
        LOG.info("About to execute action ID " + action.getId());
        return findCommandExecutor(command.getSequenceCommand()).execute(action, sender);
    }

    private String formatCommand(Action action) {
        return String.format("|%d|%s|%s|%s|", action.getId(), action.getCommand().getSequenceCommand().toString(), action.getCommand().getActivity().getName(), action.getCommand().getConfiguration().toString());
    }

    private SequenceCommandExecutor findCommandExecutor(SequenceCommand sequenceCommand) {
        switch (sequenceCommand) {
            case APPLY:
                return _applyExecutor;
            case REBOOT:
                return _rebootExecutor;
            default:
                return _defaultExecutor;
        }
    }
}
