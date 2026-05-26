package eu.macsworks.projectnhm.nhmProxy.pojo;

import java.util.List;
import java.util.UUID;

public record LobbyHeartbeatPayload(double tps, long mspt, int totalPlayers, int maxPlayers){

}