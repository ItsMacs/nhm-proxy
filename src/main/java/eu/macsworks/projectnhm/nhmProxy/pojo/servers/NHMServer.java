package eu.macsworks.projectnhm.nhmProxy.pojo.servers;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.macsworks.projectnhm.nhmProxy.NhmProxy;

import java.util.Optional;

public record NHMServer(String serverID, String serverIP, int serverPort, int maxPlayers) {

    public int getPlayerCount() {
        return getProxyServerInstance().map(registeredServer -> registeredServer.getPlayersConnected().size()).orElse(-1);
    }

    public boolean isFull() {
        return getPlayerCount() >= maxPlayers;
    }

    public Optional<RegisteredServer> getProxyServerInstance() {
        return NhmProxy.getInstance().getProxy().getServer(serverID);
    }

}
