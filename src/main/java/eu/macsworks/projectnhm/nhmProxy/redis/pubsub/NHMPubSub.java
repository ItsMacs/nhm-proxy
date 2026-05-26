package eu.macsworks.projectnhm.nhmProxy.redis.pubsub;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
@Getter
public class NHMPubSub extends JedisPubSub {

    private final String channel;

}
