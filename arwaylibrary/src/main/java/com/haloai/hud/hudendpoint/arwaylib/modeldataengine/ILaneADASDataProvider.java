package com.haloai.hud.hudendpoint.arwaylib.modeldataengine;

/**
 * author       : 龙;
 * date         : 2016/11/19;
 * email        : helong@haloai.com;
 * package_name : com.haloai.hud.hudendpoint.arwaylib.modeldataengine;
 * project_name : hudlauncher;
 */
public interface ILaneADASDataProvider {
    interface ILaneADASNotifier {
        void setLaneADASDataProvider(ILaneADASDataProvider adasDataProvider);
    }
}
