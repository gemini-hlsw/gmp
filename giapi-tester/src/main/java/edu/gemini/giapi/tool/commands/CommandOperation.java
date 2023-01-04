package edu.gemini.giapi.tool.commands;

import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.aspen.gmp.commands.jms.client.CommandSenderClient;
import edu.gemini.giapi.tool.arguments.*;
import edu.gemini.giapi.tool.parser.Argument;
import edu.gemini.giapi.tool.parser.Operation;
import edu.gemini.jms.activemq.provider.ActiveMQJmsProvider;
import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;

import java.util.logging.Logger;

/**
 * Provides the operation to send sequence commands to the GMP
 */
public class CommandOperation implements Operation {
    private static final Logger LOG = Logger.getLogger(
            CommandOperation.class.getName());

    private SequenceCommandArgument _scArgument;

    private CommandSenderClient senderClient;

    private ActivityArgument _activityArgument;

    private Configuration _config = DefaultConfiguration.emptyConfiguration();

    private String host = "localhost";

    private int repetitions = 1;
    private long timeout = Long.MAX_VALUE; // Wait forever

    public CommandOperation() {
    }

    protected CommandOperation(CommandSenderClient senderClient) {
        this.senderClient = senderClient;
    }

    protected CommandOperation(CommandSenderClient senderClient, int timeout) {
        this.senderClient = senderClient;
        this.timeout = timeout;
    }

    @Override
    public int execute() throws Exception {
        CommandSenderClient senderClient = buildCommandSender();

        Command command = new Command(
                _scArgument.getSequenceCommand(),
                _activityArgument.getActivity(),
                _config);

        int result = 0;
        for (int x = 0; x < repetitions; x++) {
            WaitingCompletionListener listener = new WaitingCompletionListener();
            Stopwatch stopwatch = Stopwatch.createStarted();
            HandlerResponse response = senderClient.sendCommand(command, listener, timeout);

            LOG.info("Response Received in " + stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) + " [ms]:" + response);

            if (response == HandlerResponse.STARTED) {
                //now, wait for the answer, synchronously
                CompletionInformation completionInformation
                        = listener.waitForResponse(timeout);
                LOG.info("Completion Information: " + completionInformation);
                result = isResponseAnError(completionInformation.getHandlerResponse()) ? 1 : 0;
            }
            if (isResponseAnError(response)) {
                result = 1;
            }

        }
        return result;
    }

    private CommandSenderClient buildCommandSender() {
        if (senderClient == null) {
            String url = "tcp://" + host + ":61616";

            // This limits making a unit test
            ActiveMQJmsProvider provider = new ActiveMQJmsProvider(url);
            provider.startConnection();
            senderClient = new CommandSenderClient(provider);
        }
        return senderClient;
    }

    private boolean isResponseAnError(HandlerResponse response) {
        return response.getResponse().equals(HandlerResponse.Response.ERROR) || response.getResponse().equals(HandlerResponse.Response.NOANSWER);
    }

    @Override
    public void setArgument(Argument arg) {
        if (arg instanceof SequenceCommandArgument) {
            _scArgument = (SequenceCommandArgument) arg;
        } else if (arg instanceof ActivityArgument) {
            _activityArgument = (ActivityArgument) arg;
        } else if (arg instanceof ConfigArgument) {
            if (_config != null) {
                _config = DefaultConfiguration.copy(_config).withConfiguration(((ConfigArgument) arg).getConfiguration()).build();
            } else {
            _config = ((ConfigArgument) arg).getConfiguration();
            }
        } else if (arg instanceof TimeoutArgument) {
            timeout = ((TimeoutArgument) arg).getTimeout();
        } else if (arg instanceof HostArgument) {
            host = ((HostArgument) arg).getHost();
        } else if (arg instanceof RepetitionArgument) {
            repetitions = ((RepetitionArgument) arg).getRepetitions();
        }
    }

    @Override
    public boolean isReady() {
        return (_scArgument != null && _activityArgument != null);
    }

}
