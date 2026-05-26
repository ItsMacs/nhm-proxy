package eu.macsworks.projectnhm.nhmProxy.redis.pubsub.impl;

import com.velocitypowered.api.proxy.Player;
import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import eu.macsworks.projectnhm.nhmProxy.managers.impl.ServerManager;
import eu.macsworks.projectnhm.nhmProxy.redis.pubsub.NHMPubSub;
import eu.macsworks.projectnhm.nhmProxy.utils.SignatureUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PlayersGameRequestPubSub extends NHMPubSub {

    private final ServerManager serverManager;
    private final NhmProxy mainInstance;

    public PlayersGameRequestPubSub() {
        super("nhm-games:player-gamereq");

        mainInstance = NhmProxy.getInstance();
        serverManager = mainInstance.getManager(ServerManager.class);
    }

    @Override
    public void onMessage(String channel, String message) {
        if(!channel.equalsIgnoreCase(getChannel())) return;

        //Message spec is: UUID:ORIG-SERVER:EPOCH:DESIRED-GAME-MODE:DESIRED-GAME-ID/NULL:SIGNATURE
        String payload = message.substring(0, message.lastIndexOf(":"));
        String[] splits = message.split(":");

        if(splits.length != 6) return;

        UUID playerUUID = UUID.fromString(splits[0]);
        String originServer = splits[1];
        long epoch = Long.parseLong(splits[2]);
        String gameType = splits[3];
        String desiredGameID = splits[4];
        String signature = splits[5];

        if(!SignatureUtils.isSignatureValid(payload, signature)) {
            mainInstance.getLogger().error("Invalid redis signature received: {}", message);
            return;
        }

        sendPlayerToGame(playerUUID, gameType, desiredGameID);
    }

    private void sendPlayerToGame(UUID playerUUID, String gameType, @Nullable String gameID) {
        Optional<Player> player = mainInstance.getProxy().getPlayer(playerUUID);
        if(player.isEmpty()) return;

        if(gameID != null){
            serverManager.sendPlayerToGameSpecific(player.get(), gameID);
            return;
        }

        serverManager.sendPlayerToGameBest(player.get(), gameType);
    }

}
