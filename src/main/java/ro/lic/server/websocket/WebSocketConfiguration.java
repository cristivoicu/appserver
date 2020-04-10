package ro.lic.server.websocket;

import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import ro.lic.server.model.Roles;
import ro.lic.server.model.repository.UserRepository;
import ro.lic.server.model.tables.User;
import ro.lic.server.websocket.utils.UserRegistry;

import java.io.IOException;
import java.security.Principal;
import java.util.Date;
import java.util.Map;

import static ro.lic.server.constants.Constants.KMS_WS_URI_DEFAULT;
import static ro.lic.server.constants.Constants.KMS_WS_URI_PROP;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {
    @Autowired
    private UserRepository userDao;

    @Bean
    public CallHandler callHandler() {
        return new CallHandler();
    }

    @Bean
    public UserRegistry registry() {
        return new UserRegistry();
    }

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create(System.getProperty(KMS_WS_URI_PROP, KMS_WS_URI_DEFAULT));
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(callHandler(), "/websocket")
                .addInterceptors(new HandShake())
                .setHandshakeHandler(new HandShakeHandler());
    }


    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    private class HandShake implements HandshakeInterceptor {

        private Roles role = null;

        @Override
        public boolean beforeHandshake(ServerHttpRequest serverHttpRequest,
                                       ServerHttpResponse serverHttpResponse,
                                       org.springframework.web.socket.WebSocketHandler webSocketHandler,
                                       Map<String, Object> map) throws Exception {

            /*User user = new User(Roles.ADMIN, "admin",
                    BCrypt.hashpw("admin", BCrypt.gensalt()),
                    "ADMIN",
                    "ADMIN_ADDR",
                    "0700000000",
                    new Date(),
                    "00:00",
                    "00:00");

            userDao.addUser(user);*/

            String username = serverHttpRequest.getHeaders().get("username").toString();
            String password = serverHttpRequest.getHeaders().get("password").toString();

            username = username.substring(username.indexOf("[") + 1, username.indexOf("]"));
            password = password.substring(password.indexOf("[") + 1, password.indexOf("]"));

           System.out.println(String.format("User %s is trying to connect", username));

            role = userDao.authenticate(username, password);

            if (role != null) {
                System.out.println("AUTH");
                return true;
            } else {
                System.out.println("ERROR");
                serverHttpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

        }

        @Override
        public void afterHandshake(ServerHttpRequest serverHttpRequest,
                                   ServerHttpResponse serverHttpResponse,
                                   org.springframework.web.socket.WebSocketHandler webSocketHandler,
                                   Exception e) {
            // set authorisation role
            serverHttpResponse.getHeaders().add("role", role.name());
        }


    }

    /**
     * Intra aici inainte sa stabileasca conexiunea la server
     */
    public class HandShakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
            // determine the user
            Principal principal = new Principal() {
                @Override
                public String getName() {
                    return request.getHeaders().get("username").toString();
                }

            };
            return principal;
        }

        @Override
        protected void handleInvalidConnectHeader(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
            super.handleInvalidConnectHeader(request, response);
        }
    }
}
