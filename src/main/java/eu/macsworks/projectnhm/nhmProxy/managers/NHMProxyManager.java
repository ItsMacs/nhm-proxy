package eu.macsworks.projectnhm.nhmProxy.managers;

import eu.macsworks.projectnhm.nhmProxy.NhmProxy;
import eu.macsworks.projectnhm.nhmProxy.api.NHMProxyLifecycledObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class NHMProxyManager implements NHMProxyLifecycledObject {

    private final NhmProxy mainInstance;

    public void onTick(){}

}
