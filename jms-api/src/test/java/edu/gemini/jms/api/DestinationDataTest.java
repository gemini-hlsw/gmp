package edu.gemini.jms.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DestinationDataTest {
    @Test
    public void buildTopicDestination() {
        DestinationData destinationData = new DestinationData("topic1", DestinationType.TOPIC);
        assertEquals("topic1", destinationData.getName());
        assertEquals(DestinationType.TOPIC, destinationData.getType());
    }

    @Test
    public void buildQueueDestination() {
        DestinationData destinationData = new DestinationData("queue1", DestinationType.QUEUE);
        assertEquals("queue1", destinationData.getName());
        assertEquals(DestinationType.QUEUE, destinationData.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithNullName() {
        new DestinationData(null, DestinationType.QUEUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithEmptyName() {
        new DestinationData("", DestinationType.QUEUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildWithNullType() {
        new DestinationData("queue1", null);
    }
}
