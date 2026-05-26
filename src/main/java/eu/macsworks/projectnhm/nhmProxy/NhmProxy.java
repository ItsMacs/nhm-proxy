package eu.macsworks.projectnhm.nhmProxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.macsworks.projectnhm.nhmProxy.api.NHMProxyLifecycledObject;
import eu.macsworks.projectnhm.nhmProxy.config.Config;
import eu.macsworks.projectnhm.nhmProxy.managers.NHMProxyManager;
import eu.macsworks.projectnhm.nhmProxy.managers.impl.RedisManager;
import eu.macsworks.projectnhm.nhmProxy.managers.impl.ServerManager;
import eu.macsworks.projectnhm.nhmProxy.redis.RedisHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Getter
public class NhmProxy {

    @Setter(AccessLevel.PRIVATE)
    @Getter
    private static NhmProxy instance;

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxy;

    @Inject
    @DataDirectory
    private Path dataDirectory;

    private Config config;

    @Getter(AccessLevel.PRIVATE)
    private final Map<Class<? extends NHMProxyManager>, NHMProxyManager> managers = new HashMap<>();

    public NhmProxy() {
        setInstance(this);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            config = new Config(dataDirectory, "config.properties", Map.of(
                    "redis.host", "localhost",
                    "redis.port", "6379"
            ));
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
        }

        addManager(new RedisManager(this));
        addManager(new ServerManager(this));
    }

    private void addManager(NHMProxyManager manager){
        managers.put(manager.getClass(), manager);

        manager.init();
    }

    private void removeManager(NHMProxyManager manager){
        managers.remove(manager.getClass());

        manager.destroy();
    }

    /**
     * Returns the (casted) requested manager. Will throw if requesting a mismatched manager between ManagerType and the actual class type.
     * @param managerType Type of manager requested
     * @return Already casted manager
     * @param <T> Typed manager
     */
    @SuppressWarnings("unchecked")
    public <T extends NHMProxyManager> T getManager(Class<T> managerType) {
        if(!managers.containsKey(managerType)){
            throw new NullPointerException(String.format("Manager %s not found", managerType));
        }

        return (T) managers.get(managerType);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        long timeDisStart = System.currentTimeMillis();

        managers.values().forEach(NHMProxyLifecycledObject::destroy);

        getLogger().info(String.format("Disabling done (%sms)",  System.currentTimeMillis() - timeDisStart));
    }
}