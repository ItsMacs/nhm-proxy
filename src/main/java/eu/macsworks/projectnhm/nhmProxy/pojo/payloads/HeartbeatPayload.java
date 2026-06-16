package eu.macsworks.projectnhm.nhmProxy.pojo.payloads;

import eu.macsworks.projectnhm.nhmProxy.pojo.servers.NHMServer;

import java.util.List;
import java.util.UUID;

public record HeartbeatPayload(String serverID, String serverIP, int serverPort, int maxPlayers, List<GameSnapshot> games, double tps, long mspt, int totalPlayers){
    public record GameSnapshot(String serverID, String gameID, String gameType, List<UUID> players, int minPlayers, int maxPlayers, RedisGameState gameState){}

    public enum RedisGameState {
        LOBBY,
        IN_PROGRESS,
        ENDED
    }

    public NHMServer toServer(){
        return new NHMServer(serverID, serverIP, serverPort, maxPlayers);
    }
}