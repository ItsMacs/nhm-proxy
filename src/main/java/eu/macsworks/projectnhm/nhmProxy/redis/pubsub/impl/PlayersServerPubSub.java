package eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import eu.macsworks.projectnhm.nhmProxy.managers.impl.RedisManager;
import eu.macsworks.projectnhm.nhmProxy.managers.impl.ServerManager;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.NHMPubSub;
import eu.macsworks.projectnhm.nhmProxy.utils.SignatureUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;
import java.util.UUID;

public class PlayersServerPubSub extends NHMPubSub {

    private final NhmProxy mainInstance;
    private final RedisManager redisManager;
    private final JedisPool jedisPool;

    public PlayersServerPubSub() {
        super("nhm-games:player-servers");

        mainInstance = NhmProxy.getInstance();
        redisManager = mainInstance.getManager(RedisManager.class);
        jedisPool = redisManager.getRedisHandler().getPool();
    }

    public void sendToGame(Player player, String serverID, String gameID){
        Optional<ServerConnection> conn = player.getCurrentServer();

        String uuid = player.getUniqueId().toString();
        String originServerName = conn.isPresent() ? conn.get().getServerInfo().getName() : "null";
        long epoch = System.currentTimeMillis();

        String payload = String.format("%s:%s:%s:%d:%s", uuid, originServerName, serverID, epoch, gameID);
        payload = payload + ":" + SignatureUtils.sign(payload);

        String finalPayload = payload;

        redisManager.getRedisHandler().publish(getChannel(), finalPayload);
    }

}
