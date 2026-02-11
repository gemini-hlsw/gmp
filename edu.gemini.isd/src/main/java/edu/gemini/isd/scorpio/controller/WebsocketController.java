package edu.gemini.isd.scorpio.controller;

import com.google.gson.Gson;
import edu.gemini.isd.scorpio.models.StatusDTO;
import edu.gemini.isd.scorpio.status.StatusCacheHandler;
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
    private Javalin app;
    private final Set<WsContext> activeSessions = new HashSet<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private StatusCacheHandler statusHandler;

    public void start(StatusCacheHandler sh){
        statusHandler = sh;

        try {
            app = Javalin.create().start(7000);

            app.ws("/ws/", ws -> {
                ws.onConnect(ctx -> {
                    activeSessions.add(ctx);
                    LOG.info("Connection established! - session: " + ctx.getSessionId());
                    ctx.send("Connection established correctly!");
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

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    sendData();
                } catch (Exception e) {
                    LOG.warning("scorpio-isd: There was an error - " + e.getMessage());
                }
            }, 0, 1, TimeUnit.SECONDS);
            System.out.println("scorpio-isd: Javalin-websocket started on ws://localhost:7000/ws");
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        } finally {
            app = null;
        }
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
        scheduler.shutdown();
    }

    /**
     * Send the data to every session connected with the WebSocket
     */
    private void sendData(){
        if (activeSessions.isEmpty()){
            return;
        }

        for (WsContext c : activeSessions) {
            try{
                c.send(processLecture());
                System.out.println("Data sent to the session: " + c.getSessionId());
            } catch (RuntimeException e) {
                System.out.println("An error occurred while sending a message to " + c.getSessionId() + " - Reason: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * This method bring the current instrument status and serializes the lecture
     * @return String that contains a JSON text with the lecture of the instrument
     * @throws RuntimeException if an error occurs during the lecture
     */
    private String processLecture(){
        Gson gson = new Gson();
        try{
            List<StatusDTO<?>> newLecture = statusHandler.snapshot();
            return gson.toJson(newLecture);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
