package edu.gemini.aspen.giapi.commands;

import java.util.EnumSet;

/**
 * Enumerated type defining the Gemini Sequence Commands
 */
public enum SequenceCommand {
    TEST("test"),
    REBOOT("reboot"),
    INIT("init"),
    DATUM("datum"),
    PARK("park"),
    VERIFY("verify"),
    END_VERIFY("endVerify"),
    GUIDE("guide"),
    END_GUIDE("endGuide"),
    APPLY("apply"),
    OBSERVE("observe"),
    END_OBSERVE("endObserve"),
    PAUSE("pause"),
    CONTINUE("continue"),
    STOP("stop"),
    STOP_CYCLE("stopCycle"),
    ABORT("abort"),
    ENGINEERING("engineering");

    private final String _name;

    private SequenceCommand(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public static SequenceCommand getFromName(String name) {
        for (SequenceCommand seq : SequenceCommand.values()) {
            if (seq.getName().equals(name)) {
                return seq;
            }
        }
        throw new IllegalArgumentException("No SequenceCommand with name: " + name);
    }

    /**
     * Returns a set of SequenceCommands that don't require configuration
     */
    public static EnumSet<SequenceCommand> commandWithNoConfig() {
        return EnumSet.complementOf(EnumSet.of(APPLY, OBSERVE, REBOOT, ENGINEERING));
    }

}
