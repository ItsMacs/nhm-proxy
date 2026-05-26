package eu.macsworks.projectnhm.nhmProxy.pojo;

import java.util.List;
import java.util.UUID;

public record HeartbeatPayload(List<GameSnapshot> games, double tps, long mspt, int totalPlayers){
    public record GameSnapshot(String serverID, String gameID, String gameType, List<UUID> players, int minPlayers, int maxPlayers, RedisGameState gameState){}

    public enum RedisGameState {
        LOBBY,
        IN_PROGRESS,
        ENDED
    }
}