package com.haloai.hud.hudendpoint.arwaylib.render.strategy;

/**
 * @author Created by Mo Bing(mobing@haloai.com) on 23/10/2016.
 */
public class ConfigurableRenderStrategy extends  RenderStrategy{

    public ConfigurableRenderStrategy(String configString) {
    }

    @Override
    public int getRoadClass(int _roadClassSDK) {
        return 0;
    }

    @Override
    public void updateCurrentRoadInfo(int roadClass, int mpDistance,int pathDistance) {

    }

    @Override
    public RenderParams getCurrentRenderParams() {
        return null;
    }

    @Override
    public void updateAnimation(AnimationType type) {

    }
}
