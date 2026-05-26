package eu.macsworks.projectnhm.nhmProxy.redis;

import com.velocitypowered.api.proxy.ProxyServer;
import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import eu.macsworks.projectnhm.nhmProxy.config.Config;
import eu.macsworks.projectnhm.nhmProxy.managers.impl.RedisManager;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.NHMPubSub;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl.PlayersLobbyPubSub;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl.PlayersServerPubSub;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedisHandler {

    private final ProxyServer proxy;
    private final NhmProxy mainInstance;

    private final RedisManager redisManager;

    @Getter
    private final JedisPool pool;

    private final List<NHMPubSub> pubSubs = new ArrayList<>();

    public RedisHandler(NhmProxy mainInstance, ProxyServer proxy, Config config) {
        this.mainInstance = mainInstance;
        this.proxy = proxy;
        this.redisManager = mainInstance.getManager(RedisManager.class);

        String host = config.getString("redis.host");
        int port = config.getInt("redis.port");

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);

        this.pool = new JedisPool(poolConfig, host, port, 2000);

        subscribe(redisManager.getPlayersLobbyPubSub());
        subscribe(redisManager.getPlayersServerPubSub());
        subscribe(redisManager.getPlayersGameRequestPubSub());
    }

    public void subscribe(NHMPubSub pubSub) {
        proxy.getScheduler().buildTask(mainInstance, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(pubSub, pubSub.getChannel());
                pubSubs.add(pubSub);
            }
        }).schedule();
    }

    public Set<String> keys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanParams params = new ScanParams().match(pattern).count(1000);
        try (Jedis jedis = pool.getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                keys.addAll(result.getResult());
                cursor = result.getCursor();
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        }
        return keys;
    }

    public void publish(String channel, String message) {
        proxy.getScheduler().buildTask(mainInstance, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(channel, message);
            }
        }).schedule();
    }

    public void shutdown() {
        pubSubs.forEach(NHMPubSub::unsubscribe);

        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}