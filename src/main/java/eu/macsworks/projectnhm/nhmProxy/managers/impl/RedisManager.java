package eu.macsworks.projectnhm.nhmProxy.managers.impl;

import com.velocitypowered.api.proxy.Player;
import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import eu.macsworks.projectnhm.nhmProxy.managers.NHMProxyManager;
import eu.macsworks.projectnhm.nhmProxy.redis.RedisHandler;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl.PlayersGameRequestPubSub;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl.PlayersLobbyPubSub;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl.PlayersServerPubSub;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class RedisManager extends NHMProxyManager {

    private RedisHandler redisHandler;

    private PlayersLobbyPubSub playersLobbyPubSub;
    private PlayersGameRequestPubSub playersGameRequestPubSub;
    private PlayersServerPubSub playersServerPubSub;

    public RedisManager(NhmProxy mainInstance) {
        super(mainInstance);
    }

    @Override
    public void onInit(){
        redisHandler = new RedisHandler(getMainInstance(), getMainInstance().getProxy(), getMainInstance().getConfig());

        playersLobbyPubSub = new PlayersLobbyPubSub();
        playersGameRequestPubSub = new PlayersGameRequestPubSub();
        playersServerPubSub = new PlayersServerPubSub();
    }

    @Override
    public void onDestroy(){
        redisHandler.shutdown();
    }

    public void connectPlayerToGameServer(Player player, String serverID, String gameID){
        playersServerPubSub.sendToGame(player, serverID, gameID);
    }

    public List<String> getActiveServers(boolean game){
        String prefix = game ? "nhm-game-pods:" : "nhm-lobby-pods:";
        Set<String> keys = redisHandler.keys(prefix + "*");
        return keys.stream()
                .map(k -> k.substring(prefix.length()))
                .collect(Collectors.toList());
    }

    public record HeartbeatPayload(List<GameSnapshot> games, double tps, long mspt, int totalPlayers){
        record GameSnapshot(String serverID, String gameID, String gameType, List<UUID> players, int minPlayers, int maxPlayers, RedisGameState gameState){}

        enum RedisGameState {
            LOBBY,
            IN_PROGRESS,
            ENDED
        }
    }

}
