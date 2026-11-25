package networking.server;

import org.java_websocket.WebSocket;

import java.util.HashMap;

public class RoomManager {
    HashMap<String, GameRoom> rooms;
    HashMap<WebSocket, GameRoom> roomsByWebSocket;

    //TODO: add proper constructor
    public RoomManager() {
        rooms = new HashMap<>();
        roomsByWebSocket = new HashMap<>();
    }

    public GameRoom getGameRoom(WebSocket socket) {
        return roomsByWebSocket.get(socket);
    }

    public GameRoom getGameRoom(String roomName) {
        return rooms.get(roomName);
    }
}
