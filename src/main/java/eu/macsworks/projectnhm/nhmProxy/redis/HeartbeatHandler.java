package eu.macsworks.projectnhm.nhmProxy.redis;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import eu.macsworks.projectnhm.nhmProxy.pojo.payloads.HeartbeatPayload;
import eu.macsworks.projectnhm.nhmProxy.pojo.payloads.LobbyHeartbeatPayload;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeartbeatHandler {

    private static final String GAME_POD_PREFIX = "nhm-game-pods:";
    private static final String LOBBY_POD_PREFIX = "nhm-lobby-pods:";

    private final RedisHandler redisHandler;
    private final Gson gson = new Gson();

    @Getter
    private final Map<String, HeartbeatPayload> heartbeats = new HashMap<>();

    @Getter
    private final Map<String, LobbyHeartbeatPayload> lobbyHeartbeats = new HashMap<>();

    public HeartbeatHandler(RedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    public void gatherGameHeartbeats() {
        gatherInto(GAME_POD_PREFIX, HeartbeatPayload.class, heartbeats);
    }

    public void gatherLobbyHeartbeats() {
        gatherInto(LOBBY_POD_PREFIX, LobbyHeartbeatPayload.class, lobbyHeartbeats);
    }

    private <T> void gatherInto(String prefix, Class<T> type, Map<String, T> target) {
        target.clear();

        String[] keys = redisHandler.keys(prefix + "*").toArray(new String[0]);
        if (keys.length == 0) return;

        List<String> values = redisHandler.mget(keys);

        for (int i = 0; i < keys.length; i++) {
            String value = values.get(i);
            if (value == null) continue;

            String podId = keys[i].substring(prefix.length());
            try {
                T payload = gson.fromJson(value, type);
                if (payload != null) {
                    target.put(podId, payload);
                }
            } catch (JsonSyntaxException ignored) {}
        }
    }

    public List<HeartbeatPayload.GameSnapshot> getAllGames() {
        return heartbeats.values().stream()
                .flatMap(payload -> payload.games().stream())
                .toList();
    }

    public List<HeartbeatPayload> getAllGamePods(){
        return heartbeats.values().stream().toList();
    }

    public List<LobbyHeartbeatPayload> getAllLobbies(){
        return lobbyHeartbeats.values().stream().toList();
    }
}