package edu.gemini.aspen.giapi.commands;

/**
 * A completion listener is a call-back interface that will be invoked by the
 * GMP when completion information is received for a sequence command that was
 * previously issued.
 */
public interface CompletionListener {

    /**
     * This method gets called when completion information is received for a
     * previously issued Command.
     *
     * @param response The response containing the completion information for
     *                 the sequence command
     * @param command  The command this completion information is
     *                 associated to
     */
    void onHandlerResponse(HandlerResponse response,
                           Command command);

    /**
     * This method gets called when completion information is received for a
     * previously issued Sequence command.
     *
     * @param response The response containing the completion information for
     *                 the sequence command
     * @param command  The sequence command this completion information is
     *                 associated to
     * @param activity The activity that was associated to the command
     * @param config   Optional configuration information that was used by the
     *                 sequence command
     */
    @Deprecated
    void onHandlerResponse(HandlerResponse response,
                           SequenceCommand command,
                           Activity activity,
                           Configuration config);

}
