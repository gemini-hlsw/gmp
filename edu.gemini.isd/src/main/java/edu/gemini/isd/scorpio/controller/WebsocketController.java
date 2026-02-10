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

public class WebsocketController {
    private Javalin app;
    static Set<WsContext> activeSessions = new HashSet<>();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static List<StatusDTO> dataList = new ArrayList<>();
    private StatusCacheHandler statusHandler;

    public void start(StatusCacheHandler sh){
        statusHandler = sh;

        try {
            app = Javalin.create()
                    .get("/", ctx -> ctx.result("hello world"))
                    .start(7000);

            app.ws("/ws/", ws -> {
                ws.onConnect(ctx -> {
                    activeSessions.add(ctx);
                    System.out.println("Connection established! - session: " + ctx.getSessionId());
                    ctx.send("Connection established correctly!");
                });

                ws.onClose(ctx -> {
                    activeSessions.remove(ctx);
                    System.out.println("scorpio-isd: Connection closed! - session: " + ctx.getSessionId() + " - reason: " + ctx.reason());
                    ctx.send("Connection closed correctly!");
                });

                ws.onError(ctx -> {
                    activeSessions.remove(ctx);

                    System.out.println("scorpio-isd: An error occurred with the session " + ctx.getSessionId());
                });
            });

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    sendData();
                } catch (Exception e) {
                    System.err.println("scorpio-isd: There was an error - " + e.getMessage());
                }
            }, 0, 1, TimeUnit.SECONDS);
            System.out.println("scorpio-isd: Javalin started on http://localhost:7000/");
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
        if (scheduler != null) {
            scheduler.shutdown();
        }
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
     * Returns a String that contains a JSON text with the lecture of the instrument
     */
    private String processLecture(){
        Gson gson = new Gson();
        try{
            dataList.clear();
            dataList = statusHandler.snapshot();
            return gson.toJson(dataList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
