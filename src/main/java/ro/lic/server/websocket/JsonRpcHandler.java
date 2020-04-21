package ro.lic.server.websocket;

import com.google.gson.JsonObject;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;

public class JsonRpcHandler extends DefaultJsonRpcHandler<JsonObject> {
    @Override
    public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {

    }

    
}
