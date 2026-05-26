package eu.macsworks.projectnhm.nhmProxy.api;


import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import org.slf4j.Logger;


public interface NHMProxyLifecycledObject {

    default String getId() {
        return getClass().getSimpleName();
    }

    default void init(){
        Logger logger = NhmProxy.getInstance().getLogger();

        long timeInitStart = System.currentTimeMillis();
        logger.info("Initializing: {}", getId());

        onInit();

        logger.info("Initialized: {}, took {}ms", getId(), System.currentTimeMillis() - timeInitStart);
    }

    default void destroy(){
        Logger logger = NhmProxy.getInstance().getLogger();

        long timeDestroyStart = System.currentTimeMillis();
        logger.info("Destroyed: {}", getId());

        onDestroy();

        logger.info("Destroyed: {}, took {}ms", getId(), System.currentTimeMillis() - timeDestroyStart);
    }

    default void onInit(){}
    default void onDestroy(){}

}
