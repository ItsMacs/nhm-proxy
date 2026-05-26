package eu.macsworks.projectnhm.nhmProxy.managers.impl;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import eu.macsworks.projectnhm.nhmProxy.managers.NHMProxyManager;
import net.kyori.adventure.text.Component;

import java.util.*;

public class ServerManager extends NHMProxyManager {

    private final List<String> lobbyPods = new ArrayList<>();
    private final List<String> gamePods = new ArrayList<>();

    private final List<RedisManager.HeartbeatPayload.GameSnapshot> games = new ArrayList<>();

    private final RedisManager redisManager;

    public ServerManager(NhmProxy mainInstance) {
        super(mainInstance);

        redisManager = mainInstance.getManager(RedisManager.class);
    }

    @Override
    public void onInit(){

    }

    @Override
    public void onTick(){
        List<String> activeServers = new ArrayList<>();

        activeServers.addAll(redisManager.getActiveServers(false));
        activeServers.addAll(redisManager.getActiveServers(true));

        List<String> missingServers = new ArrayList<>();
        missingServers.addAll(lobbyPods.stream().filter(s -> !activeServers.contains(s)).toList());
        missingServers.addAll(gamePods.stream().filter(s -> !activeServers.contains(s)).toList());

        missingServers.forEach(missingServerID -> {
            Optional<RegisteredServer> missingServer = getMainInstance().getProxy().getServer(missingServerID);
            if(missingServer.isEmpty()){ //What the actual fuck
                return;
            }

            //We have to reroute these players to the best lobby server and nuke the old lobby ref
            missingServer.get().getPlayersConnected().forEach(this::sendPlayerToLobby);

            getMainInstance().getProxy().unregisterServer(missingServer.get().getServerInfo());
            lobbyPods.remove(missingServerID);
            gamePods.remove(missingServerID);
        });
    }

    @Override
    public void onDestroy(){

    }

    public void sendPlayerToLobby(Player player){
        Optional<RegisteredServer> bestLobby = getBestLobbyServer();
        if(bestLobby.isEmpty()){
            player.sendMessage(Component.text("FATAL: No lobby servers available! Contact an administrator."));
            return;
        }

        player.createConnectionRequest(bestLobby.get()).fireAndForget();
    }

    public void sendPlayerToGameSpecific(Player player, String gameID){
        Optional<RedisManager.HeartbeatPayload.GameSnapshot> foundGame = games.stream().filter(snapshot -> snapshot.gameID().equalsIgnoreCase(gameID)).findFirst();

        sendPlayerToGame(player, foundGame);
    }

    public void sendPlayerToGameBest(Player player, String gameType){
        Optional<RedisManager.HeartbeatPayload.GameSnapshot> foundGame = getBestGameServer(gameType);

        sendPlayerToGame(player, foundGame);
    }

    private void sendPlayerToGame(Player player, Optional<RedisManager.HeartbeatPayload.GameSnapshot> gameSnapshot){
        if(gameSnapshot.isEmpty()){
            player.sendMessage(Component.text(String.format("Error: Game not found. Wait a few seconds and retry.")));
            return;
        }

        Optional<RegisteredServer> gamePod = getMainInstance().getProxy().getServer(gameSnapshot.get().serverID());
        if(gamePod.isEmpty()){
            player.sendMessage(Component.text(String.format("Error: Server %s not reachable. Wait a few seconds and retry.", gameSnapshot.get().serverID())));
            return;
        }

        redisManager.connectPlayerToGameServer(player, gameSnapshot.get().serverID(), gameSnapshot.get().gameID());
        player.createConnectionRequest(gamePod.get()).fireAndForget();
    }

    public void addTrackedLobbyPod(String server){
        lobbyPods.add(server);
    }

    public void addTrackedGamePod(String server){
        gamePods.add(server);
    }

    public Optional<RegisteredServer> getBestLobbyServer(){
        return getLobbyServerPods().stream().max(Comparator.comparingInt(server -> server.getPlayersConnected().size()));
    }

    public Optional<RedisManager.HeartbeatPayload.GameSnapshot> getBestGameServer(String gameMode){
        return games.stream()
                .filter(snapshot -> snapshot.players().size() < snapshot.maxPlayers()
                        && snapshot.gameType().equalsIgnoreCase(gameMode)
                        && snapshot.gameState() == RedisManager.HeartbeatPayload.RedisGameState.LOBBY)

                .max(Comparator.comparingInt(snapshot -> snapshot.players().size()));
    }

    public List<RegisteredServer> getLobbyServerPods(){
        return getMainInstance().getProxy().getAllServers()
                .stream()
                .filter(server -> lobbyPods.contains(server.getServerInfo().getName()))
                .toList();
    }

    public List<RegisteredServer> getGameServerPods(){
        return getMainInstance().getProxy().getAllServers()
                .stream()
                .filter(server -> gamePods.contains(server.getServerInfo().getName()))
                .toList();
    }

}
