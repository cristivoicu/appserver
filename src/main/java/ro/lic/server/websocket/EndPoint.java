package ro.lic.server.websocket;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.http.WebSocket;

@Component
@ServerEndpoint(value = "/signal/{username}")
public class EndPoint{
    @OnOpen
    public void onOpen(Session session,
                       @PathParam("username") String username) throws IOException {
        // Get session and WebSocket connection
        System.out.println(session.getUserProperties().toString());

        session.getUserPrincipal();



    }

    @OnClose
    public void onClose(Session session) throws IOException {
        // WebSocket connection closes
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Do error handling here
    }
}
