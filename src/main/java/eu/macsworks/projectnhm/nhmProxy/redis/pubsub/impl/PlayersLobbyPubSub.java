package eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl;

import com.velocitypowered.api.proxy.Player;
import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import eu.macsworks.projectnhm.nhmProxy.managers.impl.ServerManager;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.NHMPubSub;
import eu.macsworks.projectnhm.nhmProxy.utils.SignatureUtils;

import java.util.Optional;
import java.util.UUID;

public class PlayersLobbyPubSub extends NHMPubSub {

    private final NhmProxy mainInstance;
    public PlayersLobbyPubSub() {
        super("nhm-games:player-lobbys");

        mainInstance = NhmProxy.getInstance();
    }

    @Override
    public void onMessage(String channel, String message) {
        if(!channel.equalsIgnoreCase(getChannel())) return;

        //Message spec is: UUID:ORIG-SERVER:EPOCH:SIGNATURE
        String payload = message.substring(0, message.lastIndexOf(":"));
        String[] splits = message.split(":");

        if(splits.length != 4) return;

        UUID playerUUID = UUID.fromString(splits[0]);
        String originServer = splits[1];
        long epoch = Long.parseLong(splits[2]);
        String signature = splits[3];

        if(!SignatureUtils.isSignatureValid(payload, signature)) {
            mainInstance.getLogger().error("Invalid redis signature received: {}", message);
            return;
        }

        sendPlayerToLobby(playerUUID);
    }

    private void sendPlayerToLobby(UUID playerUUID) {
        Optional<Player> player = mainInstance.getProxy().getPlayer(playerUUID);
        if(player.isEmpty()) return;

        mainInstance.getManager(ServerManager.class).sendPlayerToLobby(player.get());
    }

}
