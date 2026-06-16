package eu.macsworks.projectnhm.nhmProxy.managers.impl;

import com.velocitypowered.api.proxy.Player;
import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import eu.macsworks.projectnhm.nhmProxy.managers.NHMProxyManager;
import eu.macsworks.projectnhm.nhmProxy.pojo.payloads.HeartbeatPayload;
import eu.macsworks.projectnhm.nhmProxy.pojo.payloads.LobbyHeartbeatPayload;
import eu.macsworks.projectnhm.nhmProxy.pojo.servers.NHMServer;
import eu.macsworks.projectnhm.nhmProxy.redis.HeartbeatHandler;
import eu.macsworks.projectnhm.nhmProxy.redis.RedisHandler;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl.PlayersGameRequestPubSub;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl.PlayersLobbyPubSub;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl.PlayersServerPubSub;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class RedisManager extends NHMProxyManager {

    private RedisHandler redisHandler;
    private HeartbeatHandler heartbeatHandler;

    private PlayersLobbyPubSub playersLobbyPubSub;
    private PlayersGameRequestPubSub playersGameRequestPubSub;
    private PlayersServerPubSub playersServerPubSub;

    public RedisManager(NhmProxy mainInstance) {
        super(mainInstance);
    }

    @Override
    public void onInit(){
        playersLobbyPubSub = new PlayersLobbyPubSub();
        playersGameRequestPubSub = new PlayersGameRequestPubSub();
        playersServerPubSub = new PlayersServerPubSub();

        redisHandler = new RedisHandler(getMainInstance(), getMainInstance().getProxy(), getMainInstance().getConfig());
        heartbeatHandler = new HeartbeatHandler(redisHandler);
    }

    @Override
    public void onTick(){
        heartbeatHandler.gatherGameHeartbeats();
        heartbeatHandler.gatherLobbyHeartbeats();
    }

    @Override
    public void onDestroy(){
        redisHandler.shutdown();
    }

    public void connectPlayerToGameServer(Player player, String serverID, String gameID){
        playersServerPubSub.sendToGame(player, serverID, gameID);
    }

    public List<NHMServer> getActiveServers(boolean game){
        if(game) return getHeartbeatHandler().getAllGamePods().stream().map(HeartbeatPayload::toServer).toList();

        return getHeartbeatHandler().getAllLobbies().stream().map(LobbyHeartbeatPayload::toServer).toList();
    }

    public List<HeartbeatPayload.GameSnapshot> getAllGameSnapshots(){
        return getHeartbeatHandler().getAllGames();
    }

    public List<LobbyHeartbeatPayload> getAllLobbyHeartbeats(){
        return getHeartbeatHandler().getAllLobbies();
    }

}
