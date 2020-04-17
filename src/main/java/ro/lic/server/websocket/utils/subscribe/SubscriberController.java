package ro.lic.server.websocket.utils.subscribe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;
import ro.lic.server.model.Status;
import ro.lic.server.model.tables.User;
import ro.lic.server.websocket.utils.UserSession;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Component
public class SubscriberController {
    private static final Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy, h:mm:ss a").setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    private List<UserSession> userListListener = new LinkedList<>();

    public void addUserListListener(UserSession session){
        userListListener.add(session);
    }

    public void removeUserListListener(UserSession session){
        userListListener.remove(session);
    }

    private void notifySubscribers(JsonObject message){
        for(UserSession session : userListListener){
            synchronized (session.getSession()){
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void notifySubscribersOnUserModified(User modifiedUser){
        JsonObject message = new JsonObject();

        message.addProperty("method", "subscribe");
        message.addProperty("event", "userUpdated");
        message.addProperty("payload", gson.toJson(modifiedUser));

        notifySubscribers(message);
    }

    public void notifySubscribersOnUserStatusModified(Status status, String username){
        JsonObject message = new JsonObject();

        message.addProperty("method", "subscribe");
        message.addProperty("event", "userStatus");
        message.addProperty("status", status.name());
        message.addProperty("username", username);

        notifySubscribers(message);
    }
}
