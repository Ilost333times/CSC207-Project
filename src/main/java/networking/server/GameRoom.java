package networking.server;

import entity.Player;
import networking.MessageRouter;
import networking.client.GameClient;
import org.java_websocket.WebSocket;

import java.util.HashMap;

public class GameRoom {
    MessageRouter<WebSocket> router;
    HashMap<Player, GameClient> clients;

    public GameRoom(MessageRouter<WebSocket> router,  HashMap<Player, GameClient> clients) {
        this.router = router;
        this.clients = clients;
    }

    public MessageRouter<WebSocket> getRouter() {
        return router;
    }

    // I don't know if this should be here, will check later
    public void sendToPlayer(Player player, String message) {
        GameClient client = clients.get(player);
        if (client != null) {
            client.send(message);
        }
    }
}
