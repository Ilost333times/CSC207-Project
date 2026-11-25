package app;

import networking.MessageRouter;
import networking.server.GameServer;
import networking.server.RoomManager;
import org.java_websocket.WebSocket;


public class ServerMain {
    public static void main(String[] args) {
        //Make Routers
        MessageRouter<WebSocket> lobbyRouter = new MessageRouter<>();

        //Make Room
        RoomManager roomManager = new RoomManager();

        GameServer server = new GameServer(8080, lobbyRouter, roomManager);
        server.start();

        //TODO: Build Server Lobby Managers, Use Case Interactors, Entities, etc.
    }
}
