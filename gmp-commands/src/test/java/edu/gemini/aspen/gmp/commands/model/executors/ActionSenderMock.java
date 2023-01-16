package edu.gemini.aspen.gmp.commands.model.executors;

import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.aspen.gmp.commands.model.*;

import static edu.gemini.aspen.giapi.commands.ConfigPath.configPath;
import static edu.gemini.aspen.giapi.commands.DefaultConfiguration.configurationBuilder;

abstract class ActionSenderMock implements ActionSender {
  private int callsCounter = 0;

  public int getCallsCounter() { return callsCounter; }

  @Override
  public HandlerResponse send(ActionMessage message) throws SequenceCommandException {
      return send(message, 0);
  }

  @Override
  public HandlerResponse send(ActionMessage message, long timeout) throws SequenceCommandException {
      callsCounter++;

      DefaultConfiguration.Builder configBuilder = configurationBuilder();
      for (String key : message.getDataElements().keySet()) {
          configBuilder.withConfiguration(key, message.getDataElements().get(key).toString());
      }
      Configuration applyConfig = configBuilder.build();
      Configuration subConfigurationS1 = applyConfig.getSubConfiguration(configPath("X:S1"));
      Configuration subConfigurationS2 = applyConfig.getSubConfiguration(configPath("X:S2"));

      return responseFor(subConfigurationS1, subConfigurationS2);
  }

  public abstract HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2);
}

