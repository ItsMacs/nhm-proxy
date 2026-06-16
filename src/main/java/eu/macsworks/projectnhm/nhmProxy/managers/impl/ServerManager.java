package eu.macsworks.projectnhm.nhmProxy.managers.impl;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import eu.macsworks.projectnhm.nhmProxy.managers.NHMProxyManager;
import eu.macsworks.projectnhm.nhmProxy.pojo.payloads.HeartbeatPayload;
import eu.macsworks.projectnhm.nhmProxy.pojo.servers.NHMServer;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.*;

public class ServerManager extends NHMProxyManager {

    private final List<NHMServer> lobbyPods = new ArrayList<>();
    private final List<NHMServer> gamePods = new ArrayList<>();

    private final List<HeartbeatPayload.GameSnapshot> games = new ArrayList<>();

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
        games.clear();
        games.addAll(redisManager.getAllGameSnapshots());

        manageDeadPods();
    }

    @Override
    public void onDestroy(){

    }

    private void manageDeadPods(){
        List<NHMServer> activeServers = new ArrayList<>();

        List<NHMServer> activeLobbyPods = redisManager.getActiveServers(false);
        List<NHMServer> activeGamePods = redisManager.getActiveServers(true);

        activeServers.addAll(activeLobbyPods);
        activeServers.addAll(activeGamePods);

        activeLobbyPods.stream().filter(s -> !lobbyPods.contains(s)).forEach(this::addTrackedLobbyPod);
        activeGamePods.stream().filter(s -> !gamePods.contains(s)).forEach(this::addTrackedGamePod);

        List<String> missingServers = new ArrayList<>();
        missingServers.addAll(lobbyPods.stream().map(NHMServer::serverID).filter(s -> !activeServers.contains(s)).toList());
        missingServers.addAll(gamePods.stream().map(NHMServer::serverID).filter(s -> !activeServers.contains(s)).toList());

        missingServers.forEach(missingServerID -> {
            Optional<RegisteredServer> missingServer = getMainInstance().getProxy().getServer(missingServerID);
            if(missingServer.isEmpty()){ //What the actual fuck
                return;
            }

            //We have to reroute these players to the best lobby server and nuke the old lobby ref
            missingServer.get().getPlayersConnected().forEach(this::sendPlayerToLobby);

            getMainInstance().getProxy().unregisterServer(missingServer.get().getServerInfo());
            lobbyPods.removeIf(server -> server.serverID().equals(missingServerID));
            gamePods.removeIf(server -> server.serverID().equals(missingServerID));
        });
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
        Optional<HeartbeatPayload.GameSnapshot> foundGame = games.stream().filter(snapshot -> snapshot.gameID().equalsIgnoreCase(gameID)).findFirst();

        sendPlayerToGame(player, foundGame);
    }

    public void sendPlayerToGameBest(Player player, String gameType){
        Optional<HeartbeatPayload.GameSnapshot> foundGame = getBestGameServer(gameType);

        sendPlayerToGame(player, foundGame);
    }

    private void sendPlayerToGame(Player player, Optional<HeartbeatPayload.GameSnapshot> gameSnapshot){
        if(gameSnapshot.isEmpty()){
            player.sendMessage(Component.text("Error: Game not found. Wait a few seconds and retry."));
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

    public void addTrackedLobbyPod(NHMServer server){
        lobbyPods.add(server);

        registerServer(server);
    }

    public void addTrackedGamePod(NHMServer server){
        gamePods.add(server);

        registerServer(server);
    }

    private void registerServer(NHMServer server){
        NhmProxy.getInstance().getProxy().registerServer(new ServerInfo(server.serverID(), InetSocketAddress.createUnresolved(server.serverIP(), server.serverPort())));
    }

    /**
     * Returns the fullest (whilst not fully full) lobby server and automatically removes the ones that aren't found by
     * the proxy from the list. Recurses until either a server is found, or the list becomes empty.
     * @return Hopefully, the fullest non-full lobby server in the network
     */
    public Optional<RegisteredServer> getBestLobbyServer(){
        Optional<NHMServer> srv = lobbyPods.stream()
                .filter(server -> !server.isFull() && server.getProxyServerInstance().isPresent())
                .max(Comparator.comparingInt(NHMServer::getPlayerCount));

        if(srv.isEmpty()) return Optional.empty();

        if(srv.get().getProxyServerInstance().isEmpty()){
            lobbyPods.remove(srv.get());
            return getBestLobbyServer();
        }

        return srv.get().getProxyServerInstance();
    }

    public Optional<HeartbeatPayload.GameSnapshot> getBestGameServer(String gameMode){
        return games.stream()
                .filter(snapshot -> snapshot.players().size() < snapshot.maxPlayers()
                        && snapshot.gameType().equalsIgnoreCase(gameMode)
                        && snapshot.gameState() == HeartbeatPayload.RedisGameState.LOBBY)

                .max(Comparator.comparingInt(snapshot -> snapshot.players().size()));
    }

}
