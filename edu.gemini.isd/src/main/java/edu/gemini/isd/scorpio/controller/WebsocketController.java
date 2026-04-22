package edu.gemini.isd.scorpio.controller;

import com.google.gson.Gson;
import edu.gemini.isd.scorpio.dto.StatusDTO;
import edu.gemini.isd.scorpio.handler.StatusCacheHandler;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class instances a Javalin websocket to start the transmission of the Scorpio Status.
 * To establish constant communication with the client uses an ScheduledExecutorService that transmit data every 1 second.
 */
public class WebsocketController {
    private final Logger LOG = Logger.getLogger(WebsocketController.class.getName());

    private final Set<WsContext> activeSessions = new HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Gson gson = new Gson();
    private Javalin app;

    private final StatusCacheHandler statusCacheHandler;

    public WebsocketController(StatusCacheHandler statusCacheHandler) {
        this.statusCacheHandler = statusCacheHandler;
    }

    public void start(int port){
        try {
            app = Javalin.create().start(port);
            setupWebsocket();

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    sendData();
                } catch (Exception e) {
                    LOG.warning("scorpio-isd: There was an error - " + e.getMessage());
                }
            }, 0, 1, TimeUnit.SECONDS);

            System.out.println("scorpio-isd: Javalin-websocket started on ws://localhost:7000/ws");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            app = null;
        }
    }

    public void stop() {
        if (app != null) {
            app.stop();
            app = null;
        }
        scheduler.shutdown();
    }

    /**
     * Setup the basic events for clients of the websocket (OnConnect, OnClose and OnError)
     */
    private void setupWebsocket(){
        app.ws("/ws/", ws -> {
            ws.onConnect(ctx -> {
                activeSessions.add(ctx);
                LOG.info("Connection established! - session: " + ctx.getSessionId());
                List<StatusDTO<?>> newItems = statusCacheHandler.snapshot();
                String statusPayload = serializeItems(newItems);
                ctx.send(statusPayload);
            });

            ws.onClose(ctx -> {
                activeSessions.remove(ctx);
                LOG.info("scorpio-isd: Connection closed! - session: " + ctx.getSessionId() + " - reason: " + ctx.reason());
                ctx.send("Connection closed correctly!");
            });

            ws.onError(ctx -> {
                activeSessions.remove(ctx);
                LOG.warning("scorpio-isd: An error occurred with the session " + ctx.getSessionId() + "- error:" + ctx.error());
            });
        });
    }

    /**
     * This method bring the current instrument status from the StatusHandler.
     * Sends the stored items to every client that has established a session with the WebSocket after a serialization process.
     */
    private void sendData(){
        if (activeSessions.isEmpty()){
            return;
        }

        List<StatusDTO<?>> newItems = statusCacheHandler.getChangedStatus();
        String statusPayload = serializeItems(newItems);

        if (newItems.isEmpty()){
            return;
        }
        
        // Start the delivery of status to every client
        for (WsContext c : activeSessions) {
            try{
                c.send(statusPayload);
                //System.out.println("Data sent to the session: " + c.getSessionId());
            } catch (RuntimeException e) {
                LOG.warning("An error occurred while sending a message to " + c.getSessionId() + " - Reason: " + e.getMessage());
            }
        }
    }

    /**
     * This method serializes object received to a JSON
     * @param statusList object containing the status items stored by the repository
     * @return serialized JSON of status items
     */
    private String serializeItems(List<StatusDTO<?>> statusList){
        return gson.toJson(statusList);
    }
}
