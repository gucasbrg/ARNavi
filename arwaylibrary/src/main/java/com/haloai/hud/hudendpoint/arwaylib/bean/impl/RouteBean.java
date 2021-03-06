package com.haloai.hud.hudendpoint.arwaylib.bean.impl;

import android.graphics.Bitmap;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.Projection;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviStep;
import com.amap.api.navi.model.NaviLatLng;
import com.haloai.hud.hudendpoint.arwaylib.bean.SuperBean;
import com.haloai.hud.hudendpoint.arwaylib.utils.DrawUtils;
import com.haloai.hud.utils.HaloLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author       : 龙;
 * date         : 2016/5/5;
 * email        : helong@haloai.com;
 * package_name : com.haloai.hud.hudendpoint.arwaylib.bean.impl;
 * project_name : hudlauncher;
 */
public class RouteBean extends SuperBean {
    private static final int CORRECTING_DISTANCE = 50;
    private static final int GPS_SET_COUNTER = 3;

    private List<NaviLatLng>              mPathLatLngs     = new ArrayList<NaviLatLng>();
    private List<Integer>                 mCroodsInSteps   = new ArrayList<Integer>();
    private Map<String, List<NaviLatLng>> mRoadNameLatLngs = new HashMap<String, List<NaviLatLng>>();


    private Projection       mProjection      = null;
    private AMapNaviLocation mCurrentLocation = null;
    private AMapNaviLocation mPreLocation     = null;
    private AMapNaviPath     mAMapNaviPath    = null;
    private Bitmap           mCrossImage      = null;

    private boolean mMayBeErrorLocation = true;

    private boolean      mCanDrawHudway   = false;
    private int          mCurrentPoint    = 0;
    private int          mCurrentStep     = 0;
    private int          mCurrentDistance = 0;
    private double       mRealStartPointX = 0f;
    private double       mRealStartPointY = 0f;
    private String       mNextRoadName    = null;
    private NextRoadType mNextRoadType    = null;
    private float        mDegrees         = 0f;

    private int mGpsNumberSetCounter = 1;
    private int mGpsNumber = 0;
    private int mGpsNumberSum = 0;

    private static final int NOT_MATCH_PATH_NUBER = 1;
    private int mIsNotMactchPathCounter = 0;
    private boolean mIsMatchNaviPath = true;

    public enum NextRoadType {
        LEFT,
        BACK,
        NONE,
        RIGHT
    }

    /**
     * without mCanDrawHudway
     */
    @Override
    public void reset() {
        mCurrentLocation = null;
        mPreLocation = null;
        mAMapNaviPath = null;
        mCrossImage = null;
        mCurrentPoint = 0;
        mCurrentStep = 0;
        mCurrentDistance = 0;
        mMayBeErrorLocation = true;
        mNextRoadName = null;
        mNextRoadType = null;
        mDegrees = 0f;
        mPathLatLngs.clear();
        mCroodsInSteps.clear();
        mRoadNameLatLngs.clear();
        mGpsNumber = 0;
        mGpsNumberSetCounter = 1;
        mGpsNumberSum = 0;
    }

    /***
     * 进行了连续几次取平均操作
     * */
    public RouteBean setGpsNumber(int gpsNumber) {
        mGpsNumber = gpsNumber;
        //平均处理
        /*++mGpsNumberSetCounter;
        mGpsNumberSum += gpsNumber;
        mGpsNumber = mGpsNumberSum / mGpsNumberSetCounter;
        if ( mGpsNumberSetCounter > GPS_SET_COUNTER) {
            mGpsNumberSetCounter = 0;
            mGpsNumberSum = 0;
        }*/
        return this;
    }

    public int getGpsNumber() {
        return mGpsNumber;
    }

    public boolean isMayBeErrorLocation() {
        return mMayBeErrorLocation;
    }

    public int getCurrentDistance() {
        return mCurrentDistance;
    }

    public List<Integer> getCroodsInSteps() {
        return mCroodsInSteps;
    }

    public List<NaviLatLng> getPathLatLngs() {
        return mPathLatLngs;
    }

    public AMapNaviPath getPath() {
        return mAMapNaviPath;
    }

    public RouteBean setPath(AMapNaviPath AMapNaviPath) {
        reset();
        mAMapNaviPath = AMapNaviPath;
        List<AMapNaviStep> naviStepList = AMapNaviPath.getSteps();
        for (int i = 0; i < naviStepList.size(); i++) {
            mCroodsInSteps.add(naviStepList.get(i).getCoords().size());
        }

        for (AMapNaviStep aMapNaviStep : naviStepList) {
            mPathLatLngs.addAll(aMapNaviStep.getCoords());
//            for (AMapNaviLink link : aMapNaviStep.getLinks()) {
//                if (mRoadNameLatLngs.containsKey(link.getRoadName())) {
//                    List<NaviLatLng> value = mRoadNameLatLngs.get(link.getRoadName());
//                    value.addAll(link.getCoords());
//                    mRoadNameLatLngs.put(link.getRoadName(), value);
//                } else {
//                    mRoadNameLatLngs.put(link.getRoadName(), link.getCoords());
//                }
//            }
        }

        return this;
    }

    public boolean isCanDrawHudway() {
        return mCanDrawHudway;
    }

    public RouteBean setCanDrawARway(boolean canDrawHudway) {
        mCanDrawHudway = canDrawHudway;
        return this;
    }

    public Bitmap getCrossImage() {
        return mCrossImage;
    }

    public RouteBean setCrossImage(Bitmap crossImage) {
        mCrossImage = crossImage;
        return this;
    }

    public AMapNaviLocation getCurrentLocation() {
        return mCurrentLocation;
    }

    public AMapNaviLocation getPreLocation() {
        return mPreLocation;
    }

    public boolean isMatchNaviPath() {
        return mIsMatchNaviPath;
    }

    public RouteBean setCurrentLocation(AMapNaviLocation currentLocation) {
        mPreLocation = mCurrentLocation;
        mCurrentLocation = currentLocation;
        if(!currentLocation.isMatchNaviPath()){
            if(++mIsNotMactchPathCounter > NOT_MATCH_PATH_NUBER){
                mIsNotMactchPathCounter = 0;
                mIsMatchNaviPath = false;
            }
        }else {
            if (!mIsMatchNaviPath){
                mIsMatchNaviPath = true;
            }
        }
        if (mPathLatLngs == null || mPathLatLngs.size()<=0){
            return this;
        }
        if (mCurrentDistance < CORRECTING_DISTANCE) {
            if (mPreLocation != null) {
                //如果distance小于1m，就判定为不是车的移动而是location持续返回的误差，就不将其加入到mCurrentDistance中
                float distance = AMapUtils.calculateLineDistance(
                        DrawUtils.naviLatLng2LatLng(mCurrentLocation.getCoord()),
                        DrawUtils.naviLatLng2LatLng(mPreLocation.getCoord()));
                distance = distance < 1 ? 0 : distance;
                this.mCurrentDistance += distance;
            }
            mMayBeErrorLocation = mCurrentDistance <= CORRECTING_DISTANCE;
        } else {
            mMayBeErrorLocation = false;
        }
        return this;
    }

    public int getCurrentPoint() {
        return mCurrentPoint;
    }

    public RouteBean setCurrentPoint(int currentPoint) {
        mCurrentPoint = currentPoint;
        return this;
    }

    public int getCurrentStep() {
        return mCurrentStep;
    }

    public RouteBean setCurrentStep(int currentStep) {
        mCurrentStep = currentStep;
        return this;
    }

    public Projection getProjection() {
        return mProjection;
    }

    public RouteBean setProjection(Projection projection) {
        mProjection = projection;
        return this;
    }

    public double getRealStartPointX() {
        return mRealStartPointX;
    }

    public double getRealStartPointY() {
        return mRealStartPointY;
    }

    public RouteBean setNextRoadNameAndType(String nextRoadName, NextRoadType nextRoadType) {
        mNextRoadName = nextRoadName;
        mNextRoadType = nextRoadType;
        return this;
    }

    public String getNextRoadName() {
        return mNextRoadName;
    }

    public NextRoadType getNextRoadType() {
        return mNextRoadType;
    }

    public Map<String, List<NaviLatLng>> getRoadNameLatLngs() {
        return mRoadNameLatLngs;
    }

    public RouteBean setDegrees(float degrees) {
        mDegrees = degrees;
        return this;
    }

    public float getDegrees(){
        return mDegrees;
    }
}
