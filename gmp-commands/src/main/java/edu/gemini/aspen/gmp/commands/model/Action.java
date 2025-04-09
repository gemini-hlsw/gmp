package edu.gemini.aspen.gmp.commands.model;

import com.google.common.base.Preconditions;
import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.CommandSender;
import edu.gemini.aspen.giapi.commands.CompletionListener;
import edu.gemini.aspen.giapi.commands.HandlerResponse;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Actions are used internally by ActionManager to keep track
 * of sequence command progress.
 *
 * The Action's ID must grow monotonically with each new action,
 * and there rules are defined to accept responses
 * to Actions only on certain allowed order.
 *
 * ActionManagerImpl takes care of handling that logic
 */
public final class Action implements Comparable<Action> {
    private static AtomicInteger ID = new AtomicInteger();

    private final int _actionId;
    private final Command _command;
    private final CompletionListener _listener;
    private final long timeout;

    public Action(Command command, CompletionListener listener) {
        this(command, listener, CommandSender.DEFAULT_COMMAND_RESPONSE_TIMEOUT);
    }

    public Action(Command command, CompletionListener listener, long timeout) {
        Preconditions.checkArgument(command != null, "Action's command cannot be null");
        Preconditions.checkArgument(listener != null, "Action's listener cannot be null");
        this._command = command;
        this._listener = listener;
        this.timeout = timeout;
        _actionId = ID.incrementAndGet();
    }

    public Action(int actionId, Command command, CompletionListener listener, long timeout) {
        Preconditions.checkArgument(command != null, "Action's command cannot be null");
        Preconditions.checkArgument(listener != null, "Action's listener cannot be null");
        this._command = command;
        this._listener = listener;
        this.timeout = timeout;

        _actionId = actionId;
    }

    public static int getCurrentId() {
        return ID.get();
    }

    public int getId() {
        return _actionId;
    }

    public long getTimeout() {
        return timeout;
    }

    public Command getCommand() {
        return _command;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Action action = (Action) o;

        if (_actionId != action._actionId) {
            return false;
        }
        if (!_command.equals(action._command)) {
            return false;
        }
        if (!_listener.equals(action._listener)) {
            return false;
        }
        //the objects are equals
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = _actionId;
        result = 31 * result + _command.hashCode();
        result = 31 * result + _listener.hashCode();
        return result;
    }

    @Override
    public int compareTo(Action other) {
        return _actionId - other.getId();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(_actionId).append(":").
                append(_command);
        sb.append(" {").append(_listener).append("}");
        sb.append("]");
        return sb.toString();
    }

    public void sendResponseToListeners(final HandlerResponse response) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                _listener.onHandlerResponse(response,
                                        _command);
            }
        }).start();
    }
}
