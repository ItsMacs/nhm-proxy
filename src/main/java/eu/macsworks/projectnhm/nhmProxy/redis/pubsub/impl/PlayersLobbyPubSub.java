package eu.macsworks.projectnhm.nhmProxy.redis.pubsub;

import eu.macsworks.projectnhm.nhmProxy.utils.SignatureUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

@RequiredArgsConstructor
public class PlayersLobbyPubSub extends JedisPubSub {

    @Getter
    private final String channel;

    @Override
    public void onMessage(String channel, String message) {
        if(!channel.equalsIgnoreCase(getChannel())) return;

        //Message spec is: UUID:ORIG-SERVER:EPOCH:SIGNATURE
        String payload = message.substring(0, message.lastIndexOf(":"));
        String[] splits = message.split(":");

        UUID playerUUID = UUID.fromString(splits[0]);
        String originServer = splits[1];
        long epoch = Long.parseLong(splits[2]);

        if(!SignatureUtils.isSignatureValid(payload, ))
    }

}
