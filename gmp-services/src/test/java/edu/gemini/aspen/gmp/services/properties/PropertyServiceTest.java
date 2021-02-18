package edu.gemini.aspen.gmp.services.properties;

import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import edu.gemini.aspen.gmp.services.MockedJmsArtifactsTestBase;
import edu.gemini.aspen.gmp.services.PropertyHolder;
import edu.gemini.aspen.gmp.services.core.ServiceException;
import edu.gemini.aspen.gmp.services.core.ServiceType;
import edu.gemini.aspen.gmp.services.jms.JmsServiceRequest;
import edu.gemini.jms.api.JmsProvider;
import org.junit.Before;
import org.junit.Test;

import javax.jms.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class PropertyServiceTest extends MockedJmsArtifactsTestBase {
    private String propertyValue = "localhost";
    private PropertyHolder propertyHolder;
    private String requestedProperty;

    @Before
    public void setUp() throws Exception {
        propertyHolder = mock(PropertyHolder.class);
        requestedProperty = "GMP_HOST_NAME";
        when(propertyHolder.getProperty(requestedProperty)).thenReturn("");
    }

    @Test
    public void testGetType() {
        PropertyHolder propertyHolder = mock(PropertyHolder.class);
        assertEquals(ServiceType.PROPERTY_SERVICE, new PropertyService(propertyHolder).getType());
    }

    @Test
    public void testProcess() throws ServiceException, JMSException {
        // TODO The complexity of this mocked tests indicates the need for refactoring
        PropertyService propertyService = new PropertyService(propertyHolder);
        createMockedObjects();
        JmsServiceRequest request = mockServiceRequest(mapMessage);
        TextMessage replyMessage = mockReplyMessage(session);

        propertyService.startJms(provider);

        propertyService.process(request);

        assertEquals(propertyValue, replyMessage.getText());
        verify(propertyHolder).getProperty(requestedProperty);
        verify(producer).send(any(Message.class));
        verify(producer).close();
    }

    private JmsServiceRequest mockServiceRequest(MapMessage msg) throws JMSException {
        JmsServiceRequest request = new JmsServiceRequest(msg);
        when(msg.getString(JmsKeys.GMP_UTIL_PROPERTY)).thenReturn(requestedProperty);
        return request;
    }

    private TextMessage mockReplyMessage(Session session) throws JMSException {
        TextMessage replyMessage = mock(TextMessage.class);
        when(replyMessage.getText()).thenReturn(propertyValue);
        when(session.createTextMessage(propertyValue)).thenReturn(replyMessage);
        return replyMessage;
    }

    @Test
    public void testEmptyRequestMessage() throws JMSException, ServiceException {
        PropertyService propertyService = new PropertyService(propertyHolder);
        JmsServiceRequest request = new JmsServiceRequest(null);
        createMockedObjects();
        // There are really no interactions here, basically we test that the session is not used
        propertyService.startJms(provider);
        propertyService.process(request);

        verifyNoInteractions(session);
    }

    @Test
    public void testNoDestination() throws JMSException, ServiceException {
        PropertyService propertyService = new PropertyService(propertyHolder);
        MapMessage requestMessage = mock(MapMessage.class);

        createMockedObjects();
        JmsServiceRequest request = mockServiceRequest(requestMessage);

        propertyService.startJms(provider);
        propertyService.process(request);
    }
}
