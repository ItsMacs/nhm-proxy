package eu.macsworks.projectnhm.nhmProxy.pojo.payloads;

import eu.macsworks.projectnhm.nhmProxy.pojo.servers.NHMServer;

public record LobbyHeartbeatPayload(String serverID, String serverIP, int serverPort, double tps, long mspt, int totalPlayers, int maxPlayers){

    public NHMServer toServer(){
        return new NHMServer(serverID, serverIP, serverPort, maxPlayers);
    }

}