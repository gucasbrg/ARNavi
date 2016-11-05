package com.haloai.hud.hudendpoint.arwaylib.modeldataengine;

import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;

import com.amap.api.maps.model.LatLng;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.model.AMapNaviLink;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviStep;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.haloai.hud.hudendpoint.arwaylib.render.strategy.IRenderStrategy;
import com.haloai.hud.hudendpoint.arwaylib.utils.ARWayProjection;
import com.haloai.hud.hudendpoint.arwaylib.utils.Douglas;
import com.haloai.hud.hudendpoint.arwaylib.utils.EnlargedCrossProcess;
import com.haloai.hud.hudendpoint.arwaylib.utils.MathUtils;
import com.haloai.hud.hudendpoint.arwaylib.utils.PrintUtils;
import com.haloai.hud.hudendpoint.arwaylib.utils.jni_data.LatLngOutSide;
import com.haloai.hud.hudendpoint.arwaylib.utils.jni_data.LinkInfoOutside;
import com.haloai.hud.hudendpoint.arwaylib.utils.jni_data.Size2iOutside;
import com.haloai.hud.utils.HaloLogger;

import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Created by Mo Bing(mobing@haloai.com) on 22/10/2016.
 */
public class AMapNaviPathDataProcessor implements INaviPathDataProcessor<AMapNavi, AMapNaviPath, NaviInfo, AMapNaviLocation>, IDynamicLoader.IDynamicLoadNotifer {
    //constant
    private static final String TAG                           = "AMapNaviPathDataProcessor";
    private static final double DEFAULT_OPENGL_Z              = 0;//被追随物体的Z轴高度,用于构建Vector3中的Z
    private static final float  TIME_15_20                    = 32;//15级数据到20级数据转换的系数
    private static final double RAREFY_PIXEL_COUNT            = 1;//道格拉斯抽析的像素个数
    private static final int    DEFAULT_LEVEL                 = 15;//默认转换等级15(需要转换成20)
    private static final int    ANIM_DURATION_REDUNDAN        = 100;//动画默认延长时间避免停顿
    private static final int    CROSS_COUNT_INIT              = 3;//初始拉取路网数据的路口个数
    private static final double NEED_OPENGL_LENGTH            = 50;//摄像头高度角度不考虑时视口需要显示的opengl长度
    private static final double FACTOR_LEVEL20_OPENGL_2_METER = 16.5;//20级下opengl到米的转换系数 20级别下--1opengl~=16.5meter
    private static final double SEGMENT_OPENGL_LEGNTH         = 10;//每一个小段对应的opengl长度(也就是说需要四段20/5)

    //Cache all navigation path data.That two member can not change address,because renderer is use that too.
    private INaviPathDataProvider mNaviPathDataProvider = new AMapNaviPathDataProvider();
    private IRoadNetDataProvider  mRoadNetDataProvider  = new RoadNetDataProvider();

    //listener and notifier
    private IRenderStrategy mRenderStrategy;

    //middle data
    private AMapNavi            mAMapNavi;
    private List<LatLng>        mPathLatLng;
    private List<Vector3>       mPathVector3;
    private List<Vector3>       mDouglasPath;
    private List<List<Vector3>> mRenderPaths;
    //private List<Integer>       mPointIndexsToKeep;
    private List<Integer>       mStepLengths;
    private List<Integer>       mStepPointIndexs;
    private double              mOffsetX;
    private double              mOffsetY;
    private boolean             mIsPathInited;
    private int                 mTotalSize;

    //real-time data
    private int mCurIndexInPath;
    private int mCurStep;

    //animation
    private Vector3 mFromPos     = null;
    private Vector3 mToPos       = null;
    private double  mFromDegrees = 0;
    private double  mToDegrees   = 0;
    private long    mPreTime     = 0;

    //dynamic load
    private int           mCurLevelNeedMeter;
    private List<Integer> mSplitPointIndexs;
    private int           mCurIndexInSplitPoints;
    private double        METER_2_OPENGL;
    private double        NEED_LOAD_METER;
    private double        mLeftMeterLength;
    private List<Integer> mAlreadyLoadStep = new ArrayList<>();
    private int mPreDynamicStartIndex;
    private int mPreDynamicEndIndex;

    //Road Net
    private EnlargedCrossProcess mEnlargedCrossProcess = new EnlargedCrossProcess();
    private double PIXEL_2_LATLNG;
    private int                 mPreStartBreak      = 0;
    private int                 mPreEndBreak        = 0;
    private int                 mPreStepIndex       = 0;
    private List<List<Vector3>> mBranchPaths        = new ArrayList<>();
    private List<Integer>       mBranchInPathIndexs = new ArrayList<>();

    //proportion mapping
    private ProportionMappingEngine mProportionMappingEngine;

    //ylqtest
    private IDynamicLoader mDynamicLoader = new DynamicLoader();

    public AMapNaviPathDataProcessor() {
        mDynamicLoader.setIDynamicLoadNotifer(this);
    }

    @Override
    public void reset() {
        mCurIndexInPath = 0;
        mCurStep = 0;
        mOffsetX = 0;
        mOffsetY = 0;
        mFromPos = null;
        mToPos = null;
        mFromDegrees = 0;
        mToDegrees = 0;
        mPreTime = 0;
        mIsPathInited = false;
        mTotalSize = 0;
        mCurIndexInSplitPoints = 0;
        mLeftMeterLength = 0;
        mPreStartBreak = 0;
        mPreEndBreak = 0;
        mPreStepIndex = 0;
        mPreDynamicStartIndex = 0;
        mPreDynamicEndIndex = 0;
        mAlreadyLoadStep.clear();
        mBranchPaths.clear();
        mBranchInPathIndexs.clear();
        mCurLevelNeedMeter = (int) (NEED_OPENGL_LENGTH * FACTOR_LEVEL20_OPENGL_2_METER);
        mRoadNetDataProvider.reset();
        mNaviPathDataProvider.reset();
    }

    @Override
    /**
     * @param aMapNaviPath 导航路径
     * @return 1:路径处理正常,可以调用getNaviPathDataProvider获取数据 -1:异常情况
     */
    public int setPath(AMapNavi amapNavi, AMapNaviPath aMapNaviPath) {
        //0.reset all data
        reset();
        //1.check data legal
        HaloLogger.logE(TAG, "initPath check data legal");
        if (amapNavi == null || aMapNaviPath == null) {
            return -1;
        }
        mAMapNavi = amapNavi;

        List<Vector3> path_vector3 = new ArrayList<>();
        List<LatLng> path_latlng = new ArrayList<>();
        List<Integer> step_lengths = new ArrayList<>();
        List<Integer> stepPointIndexs = new ArrayList<>();
        for (AMapNaviStep step : aMapNaviPath.getSteps()) {
            if (step != null) {
                step_lengths.add(step.getCoords().size());
                for (int i = 0; i < step.getCoords().size(); i++) {
                    NaviLatLng coord = step.getCoords().get(i);
                    LatLng latLng = new LatLng(coord.getLatitude(), coord.getLongitude());
                    path_latlng.add(latLng);
                    ARWayProjection.PointD pd = ARWayProjection.toOpenGLLocation(latLng, DEFAULT_LEVEL);
                    path_vector3.add(new Vector3(pd.x, -pd.y, DEFAULT_OPENGL_Z));
                    if (i == step.getCoords().size() - 1) {
                        stepPointIndexs.add(path_latlng.size() - 1);
                    }
                }
            }
        }
        mPreDynamicStartIndex = 0;
        mPreDynamicEndIndex = mDynamicLoader.updateOriginPath(path_latlng, 20) + 1;
        HaloLogger.logE("ylq", "first end index = " + mPreDynamicEndIndex);
        mStepLengths = step_lengths;
        mStepPointIndexs = stepPointIndexs;
        mTotalSize = path_latlng.size();

        //2.data pre handle
        HaloLogger.logE(TAG, "initPath data pre handle");
        //move to screen center
        mOffsetX = path_vector3.get(0).x - 0;
        mOffsetY = path_vector3.get(0).y - 0;
        for (int i = 0; i < path_vector3.size(); i++) {
            path_vector3.get(i).x -= mOffsetX;
            path_vector3.get(i).y -= mOffsetY;
        }
        //init render path(bigger and rarefy)
        //目前抽析放在映射引擎中实现
        /*List<PointF> returnPath = new ArrayList<>();
        List<PointF> originalPath = new ArrayList<>();
        for (Vector3 v : path_vector3) {
            originalPath.add(new PointF((float) v.x, (float) v.y));
        }
        List<Integer> pointIndexsToKeep = new ArrayList<>();*/
        /*Douglas.rarefyGetPointFs(pointIndexsToKeep, returnPath, originalPath, RAREFY_PIXEL_COUNT / ARWayProjection.K);
        List<Vector3> douglasPath = new ArrayList();
        for (PointF p : returnPath) {
            douglasPath.add(new Vector3(p.x * TIME_15_20, p.y * TIME_15_20, DEFAULT_OPENGL_Z));
        }*/
        List<Vector3> douglasPath = new ArrayList();
        for (Vector3 p : path_vector3) {
            douglasPath.add(new Vector3(p.x * TIME_15_20, p.y * TIME_15_20, DEFAULT_OPENGL_Z));
        }
        //delete break point 去折点
        //..................
        //bigger ori path
        for (Vector3 v : path_vector3) {
            v.x *= TIME_15_20;
            v.y *= TIME_15_20;
        }
        //求20级下像素与经纬度的对应关系一个像素对应多少经纬度单位
        double latlng_dist = MathUtils.calculateDistance(
                path_latlng.get(0).latitude, path_latlng.get(0).longitude,
                path_latlng.get(1).latitude, path_latlng.get(1).longitude);
        Point _p0 = ARWayProjection.toScreenLocation(path_latlng.get(0));
        Point _p1 = ARWayProjection.toScreenLocation(path_latlng.get(1));
        double pixel_dist = MathUtils.calculateDistance(_p0.x, _p0.y, _p1.x, _p1.y);
        PIXEL_2_LATLNG = latlng_dist / pixel_dist;

        mPathVector3 = path_vector3;
        mPathLatLng = path_latlng;
        mDouglasPath = douglasPath;
        //mPointIndexsToKeep = pointIndexsToKeep;

        //calc and save the car need to rotate degrees
        Vector3 p1 = mDouglasPath.get(0);
        Vector3 p2 = mDouglasPath.get(1);
        for (int i = 1; i < mDouglasPath.size(); i++) {
            if (!p1.equals(mDouglasPath.get(i))) {
                p2 = mDouglasPath.get(i);
                break;
            }
        }
        double rotateZ = (Math.toDegrees(MathUtils.getRadian(p1.x, p1.y, p2.x, p2.y)) + 270) % 360;
        mNaviPathDataProvider.setObjStartOrientation(rotateZ);

        //split douglasPath with mCurLevelNeedMeter and save the split point index to list.
        List<Integer> splitPointIndexs = new ArrayList<>();
        splitPointIndexs.add(0);
        double addUpLength = 0;
        for (int i = 0; i < mDouglasPath.size() - 1; i++) {
            Vector3 v1 = mDouglasPath.get(i);
            Vector3 v2 = mDouglasPath.get(i + 1);
            addUpLength += (MathUtils.calculateDistance(v1.x, v1.y, v2.x, v2.y));
            if (addUpLength >= SEGMENT_OPENGL_LEGNTH) {
                splitPointIndexs.add(i + 1);
                addUpLength = 0;
            } else if (i == mDouglasPath.size() - 2) {
                splitPointIndexs.add(i + 1);
                break;
            }
        }
        mSplitPointIndexs = splitPointIndexs;

        //finally set path to provider and call back to renderer in Provider.
        //segmentCount = NEED_OPENGL_LENGTH / SEGMENT_OPENGL_LEGNTH + 1;
        /*List<List<Vector3>> renderPath = new ArrayList<>();
        double showLength = 0;
        for (int i = 0; i < mSplitPointIndexs.size() - 1; i++) {
            int start = mSplitPointIndexs.get(i);
            int end = mSplitPointIndexs.get(i + 1);
            renderPath.add(mDouglasPath.subList(start, end + 1));
            double partLength = 0;
            for (int j = 0; j < renderPath.get(renderPath.size() - 1).size() - 1; j++) {
                Vector3 v1 = renderPath.get(renderPath.size() - 1).get(j);
                Vector3 v2 = renderPath.get(renderPath.size() - 1).get(j + 1);
                partLength += (MathUtils.calculateDistance(v1.x, v1.y, v2.x, v2.y));
            }
            showLength += partLength;
            if (showLength >= NEED_OPENGL_LENGTH || i == mSplitPointIndexs.size() - 2) {
                mCurIndexInSplitPoints = i;
                break;
            }
        }
        double totalLen = 0;
        for (int i = 0; i < mDouglasPath.size() - 1; i++) {
            Vector3 v1 = mDouglasPath.get(i);
            Vector3 v2 = mDouglasPath.get(i + 1);
            totalLen += MathUtils.calculateDistance(v1.x, v1.y, v2.x, v2.y);
        }
        //表示一个opengl单位表示多少米--  20级别下--1opengl~=16.5meter
        METER_2_OPENGL = aMapNaviPath.getAllLength() / totalLen;
        NEED_LOAD_METER = METER_2_OPENGL * SEGMENT_OPENGL_LEGNTH ;
        mLeftMeterLength = aMapNaviPath.getAllLength();

        mRenderPaths = renderPath;
        mNaviPathDataProvider.initPath(mRenderPaths);*/

        mRenderPaths = new ArrayList<>();
        //mRenderPaths.add(mDouglasPath);
        mProportionMappingEngine = new ProportionMappingEngine(mPathLatLng);
        mProportionMappingEngine.rarefyDouglas(mStepPointIndexs, RAREFY_PIXEL_COUNT / ARWayProjection.K, DEFAULT_LEVEL);

        //动态加载 0--endIndex
        HaloLogger.logE(TAG, "mPathLatLng path start");
        for (LatLng latlng : mPathLatLng) {
            HaloLogger.logE(TAG, latlng.latitude + "," + latlng.longitude);
        }
        HaloLogger.logE(TAG, "mPathLatLng path end");

        HaloLogger.logE("ylq", "step indexs size = " + mStepPointIndexs.size());
        for (int i = 0; i < mPreDynamicEndIndex; i++) {
            if (mStepPointIndexs.contains(i)) {
                HaloLogger.logE("ylq", "i=" + i + ",index=" + mStepPointIndexs.indexOf(i));
                processSteps(mStepPointIndexs.indexOf(i));
            }
        }
        /*for (int i = 0; i < mStepPointIndexs.size(); i++) {
            processSteps(i);
        }*/
        HaloLogger.logE(TAG, "mProportionMappingEngine.getRenderPath screen start");
        for (LatLng latlng : mProportionMappingEngine.getRenderPath()) {
            HaloLogger.logE(TAG, latlng.latitude + "," + latlng.longitude);
        }
        HaloLogger.logE(TAG, "mProportionMappingEngine.getRenderPath screen end");
        List<Vector3> mainRoad = new ArrayList<>();
        for (LatLng latlng : mProportionMappingEngine.mapping(0, mPreDynamicEndIndex)) {
            ARWayProjection.PointD pd = ARWayProjection.toOpenGLLocation(new LatLng(latlng.latitude, latlng.longitude), DEFAULT_LEVEL);
            mainRoad.add(new Vector3((pd.x - mOffsetX) * TIME_15_20, (-pd.y - mOffsetY) * TIME_15_20, DEFAULT_OPENGL_Z));
        }
        mRenderPaths.add(mainRoad);
        mRenderPaths.addAll(mBranchPaths);
        mNaviPathDataProvider.initPath(mRenderPaths);

        //显示第一根蚯蚓线
        processGuildLine(mStepPointIndexs.get(0));

        //4.call processSteps(IRoadNetDataProcessor to get data and create IRoadNetDataProvider something)
        //HaloLogger.logE(TAG, "initPath call processSteps(IRoadNetDataProcessor to get data and create IRoadNetDataProvider something)");
        //processSteps(0,1,2)
        //mRoadNetChangeNotifier.onRoadNetDataChange();

        mIsPathInited = true;
        return 1;
    }

    @Override
    public void loadNewRoad(int startIndex, int endIndex) {
        Log.e("ylq", "startIndex:" + startIndex + " endIndex" + endIndex);
        Log.e("ylq", "remove start");
        //1.删除掉mBranchPaths以及mBranchInPathIndexs中已经不在start和end之间的部分
        for (int i = mPreDynamicStartIndex; i < startIndex; i++) {
            if (mBranchInPathIndexs.contains(i)) {
                int index = mBranchInPathIndexs.indexOf(i);
                int removeIndex = mBranchInPathIndexs.remove(index);
                mBranchPaths.remove(index);
                Log.e("ylq", "i=" + i);
                Log.e("ylq", "index=" + index);
                Log.e("ylq", "removeIndex=" + removeIndex);
            }
        }
        Log.e("ylq", "remove end");
        //2.拉取新的部分的路网数据
        for (int i = startIndex; i < endIndex; i++) {
            if (mStepPointIndexs.contains(i)) {
                processSteps(mStepPointIndexs.indexOf(i));
            }
        }
        //3.拉取对应的主路数据
        List<Vector3> dynamicPath = new ArrayList<>();
        for (LatLng latlng : mProportionMappingEngine.mapping(startIndex, endIndex)) {
            ARWayProjection.PointD pd = ARWayProjection.toOpenGLLocation(new LatLng(latlng.latitude, latlng.longitude), DEFAULT_LEVEL);
            dynamicPath.add(new Vector3((pd.x - mOffsetX) * TIME_15_20, (-pd.y - mOffsetY) * TIME_15_20, DEFAULT_OPENGL_Z));
        }
        mRenderPaths.clear();
        mRenderPaths.add(dynamicPath);
        mRenderPaths.addAll(mBranchPaths);
        mNaviPathDataProvider.updatePath(mRenderPaths);

        //4.更新蚯蚓线,因为主路被替换了所以需要刷新下
        processGuildLine(mStepPointIndexs.get(mCurStep));

        mPreDynamicEndIndex = endIndex;
        mPreDynamicStartIndex = startIndex;
    }

    @Override
    public void setLocation(AMapNaviLocation location, Vector3 animPos, double animDegrees) {
        if (mIsPathInited) {
            //call DataProvider to update anim with cur location
            if (mFromPos == null) {
                mFromPos = convertLocation(location, mCurIndexInPath);
                mFromDegrees = MathUtils.convertAMapBearing2OpenglBearing(location.getBearing());
                mPreTime = location.getTime();
            } else {
                long duration = location.getTime() - mPreTime;
                mPreTime = location.getTime();
                if (mToPos != null) {
                    mFromPos = animPos;
                    mFromDegrees = Math.toDegrees(animDegrees);
                    mFromDegrees = mFromDegrees < 0 ? mFromDegrees + 360 : mFromDegrees;
                }
                mToPos = convertLocation(location, mCurIndexInPath);
                mToDegrees = MathUtils.convertAMapBearing2OpenglBearing(location.getBearing());
                mNaviPathDataProvider.setAnim(mFromPos, mToPos, mToDegrees - mFromDegrees, duration + ANIM_DURATION_REDUNDAN);
            }
        }
    }

    @Override
    public void setNaviInfo(NaviInfo naviInfo) {
        if (mIsPathInited && naviInfo != null) {
            //0.calc and save useful data.
            mCurIndexInPath = getIndexInPath(naviInfo.getCurPoint(), naviInfo.getCurStep());
            mDynamicLoader.updateCurPoint(mCurIndexInPath);
            //1.Calculate the distance of maneuver point and get road class.
            int distanceOfMP = naviInfo.getCurStepRetainDistance();
            AMapNaviLink curLink = mAMapNavi.getNaviPath().getSteps().get(naviInfo.getCurStep())
                    .getLinks().get(naviInfo.getCurLink());
            mRenderStrategy.updateCurrentRoadInfo(curLink.getRoadClass(), distanceOfMP);
            /*//2.dynamic load with current path retain distance
            HaloLogger.logE(TAG,"mLeftMeterLength - naviInfo.getPathRetainDistance() = "+(mLeftMeterLength - naviInfo.getPathRetainDistance()));
            HaloLogger.logE(TAG,"NEED_LOAD_METER = "+NEED_LOAD_METER);
            if(mLeftMeterLength - naviInfo.getPathRetainDistance() >= NEED_LOAD_METER){
                if(mCurIndexInSplitPoints<mSplitPointIndexs.size()-1) {
                    //dynamic load
                    HaloLogger.logE(TAG,"dynamic load");
                    int start = mSplitPointIndexs.get(mCurIndexInSplitPoints);
                    int end = mSplitPointIndexs.get(mCurIndexInSplitPoints + 1);
                    HaloLogger.logE(TAG,"start = "+start);
                    HaloLogger.logE(TAG,"end = "+end);
                    mNaviPathDataProvider.updatePath(mDouglasPath.subList(start, end + 1));
                    mCurIndexInSplitPoints++;
                    mLeftMeterLength = naviInfo.getPathRetainDistance();
                }
            }*/
            if (naviInfo.getCurStep() > mCurStep) {
                //3.Guide line change
                processGuildLine(mStepPointIndexs.get(naviInfo.getCurStep()));
                mCurStep = naviInfo.getCurStep();
            }
        }
    }

    private void processGuildLine(int curIndexInPath) {
//        List<LatLng> guildLine = mProportionMappingEngine.mappingGuide(curIndexInPath);
        List<ARWayProjection.PointD> guildLine = mProportionMappingEngine.mappingGuideV(curIndexInPath);
        if (guildLine != null) {
            List<Vector3> guildLineVector3 = new ArrayList<>();
            for (ARWayProjection.PointD pointD : guildLine) {
                //ARWayProjection.PointD pointD = ARWayProjection.toOpenGLLocation(latlng, DEFAULT_LEVEL);
                Vector3 v = new Vector3((pointD.x - mOffsetX) * TIME_15_20, (-pointD.y - mOffsetY) * TIME_15_20, DEFAULT_OPENGL_Z);
                guildLineVector3.add(v);
            }
            mNaviPathDataProvider.setGuildLine(guildLineVector3);
        }
    }

    @Override
    public boolean setRenderStrategy(IRenderStrategy renderStrategy) {
        this.mRenderStrategy = renderStrategy;
        return true;
    }

    @Override
    public boolean setRoadNetChangeNotifier(IRoadNetDataProvider.IRoadNetDataNotifier roadNetChangeNotifier) {
        mRoadNetDataProvider.setRoadNetChangeNotifier(roadNetChangeNotifier);
        return mRoadNetDataProvider != null;
    }

    @Override
    public boolean setNaviPathChangeNotifier(INaviPathDataProvider.INaviPathDataChangeNotifer naviPathChangeNotifier) {
        mNaviPathDataProvider.setNaviPathChangeNotifier(naviPathChangeNotifier);
        return mNaviPathDataProvider != null;
    }

    @Override
    public INaviPathDataProvider getNaviPathDataProvider() {
        return mNaviPathDataProvider;
    }

    @Override
    public IRoadNetDataProvider getRoadNetDataProvider() {
        return mRoadNetDataProvider;
    }

    /**
     * TODO:暂时不对路网数据进行处理
     * 访问路网模块获取指定steps的路网数据
     */
    private void processSteps(int... stepIndexs) {
        HaloLogger.logE(TAG, "process steps start");
        Log.e("ylq", "add start");
        for (Integer stepIndex : stepIndexs) {
            if (mAlreadyLoadStep.contains(stepIndex)) {
                continue;
            }
            mAlreadyLoadStep.add(stepIndex);
            //1.根据stepIndex构建数据
            //  1.机动点前后道路link形式(暂时由一条link表示整个导航路)
            //  2.每个link的info
            //  3.机动点经纬度
            //  *4.切割机动点前后400*400范围的点,且取到边缘点
            //  *5.使用返回的数据替代原先的数据(主路部分)
            Size2iOutside szCover = new Size2iOutside();
            szCover.width = 800;
            szCover.height = 800;

            List<List<LatLngOutSide>> links = new ArrayList<>();
            List<LatLngOutSide> link = new ArrayList<>();
            LatLngOutSide centerLatLng = new LatLngOutSide();
            centerLatLng.lat = mPathLatLng.get(mStepPointIndexs.get(stepIndex)).latitude;
            centerLatLng.lng = mPathLatLng.get(mStepPointIndexs.get(stepIndex)).longitude;
            LatLng[] point8 = new LatLng[8];
            int[] se = getPartPathFromCover(szCover, stepIndex, mPathLatLng, centerLatLng, link, point8);
            int breakStart = se[0];
            int breakEnd = se[1];
            if (breakStart < mPreEndBreak) {
                //1.合并两个step,问题是按照当前算法路线越长越复杂,越容易匹配到错误的道路
                /*LatLng lastEnd = mPathLatLng.get(mPreEndBreak);
                LatLng thisStart = mPathLatLng.get(breakStart);
                szCover.width = (int) (szCover.width*2-(Math.max(Math.abs(lastEnd.latitude - thisStart.latitude), Math.abs(lastEnd.longitude - thisStart.longitude)))/PIXEL_2_LATLNG);
                szCover.height = (int) (szCover.height*2-(Math.max(Math.abs(lastEnd.latitude - thisStart.latitude), Math.abs(lastEnd.longitude - thisStart.longitude)))/PIXEL_2_LATLNG);
                HaloLogger.logE(TAG,"width="+szCover.width);
                HaloLogger.logE(TAG,"height="+szCover.height);
                //将扩充后的窗口中的点也添加到link中
                for(int i=breakStart;i>=mPreStartBreak;i--){
                    link.add(0,new LatLngOutSide(mPathLatLng.get(i).latitude,mPathLatLng.get(i).longitude));
                }
                breakStart = mPreStartBreak;
                mPreEndBreak = breakEnd;*/
                //2.暂时先跳过该路口不做处理,因为合并处理时得不到岔路,问题是可能会跳过多个路口,导致路口显示过少
                //continue;
                //3.当覆盖时,缩小窗口,同时缩短Path到新窗口的边缘
                LatLngOutSide preCenterLatLng = new LatLngOutSide();
                preCenterLatLng.lat = mPathLatLng.get(mStepPointIndexs.get(mPreStepIndex)).latitude;
                preCenterLatLng.lng = mPathLatLng.get(mStepPointIndexs.get(mPreStepIndex)).longitude;
                double offsetCover = szCover.width - (Math.max(Math.abs(centerLatLng.lat - preCenterLatLng.lat), Math.abs(centerLatLng.lng - preCenterLatLng.lng))) / PIXEL_2_LATLNG;
                HaloLogger.logE(TAG, "szCover.width=" + szCover.width);
                HaloLogger.logE(TAG, "max=" + (Math.max(Math.abs(centerLatLng.lat - preCenterLatLng.lat), Math.abs(centerLatLng.lng - preCenterLatLng.lng))) / PIXEL_2_LATLNG);
                HaloLogger.logE(TAG, "offsetCover=" + offsetCover);
                if (offsetCover >= szCover.width / 2) {
                    continue;
                }
                szCover.width -= 2 * offsetCover;
                szCover.height -= 2 * offsetCover;
                se = getPartPathFromCover(szCover, stepIndex, mPathLatLng, centerLatLng, link, point8);
                breakStart = se[0];
                breakEnd = se[1];
            }

            /*HaloLogger.logE(TAG, "cover cross start");
            for (LatLng latlng : point8) {
                HaloLogger.logE(TAG, latlng.latitude + "," + latlng.longitude);
            }
            HaloLogger.logE(TAG, "cover cross end");*/

            HaloLogger.logE(TAG, "width=" + szCover.width + ",height=" + szCover.height);
            if (breakEnd == 0) {
                breakEnd = mPathLatLng.size() - 1;
            }
            HaloLogger.logE(TAG, "breakStart=" + breakStart + ",breakEnd=" + breakEnd);
            links.add(link);

            List<LinkInfoOutside> linkInfos = new ArrayList<>();

            LatLngOutSide centerPoint = new LatLngOutSide();
            centerPoint.lat = centerLatLng.lat;
            centerPoint.lng = centerLatLng.lng;

            String filePath = "/sdcard/haloaimapdata_32.hmd";

            List<List<LatLngOutSide>> crossLinks = new ArrayList<>();
            List<LatLngOutSide> mainRoad = new ArrayList<>();
            List<Integer> crossPointIndexs = new ArrayList<>();

            HaloLogger.logE(TAG, "into jni");
            int res = mEnlargedCrossProcess.updateCrossLinks(links, linkInfos, centerPoint, szCover, filePath, crossLinks, mainRoad, crossPointIndexs);
            HaloLogger.logE(TAG, "outto jni");
            HaloLogger.logE(TAG, "res=" + res + ",and cross links size=" + crossLinks.size());

            if (res == 0 && crossLinks.size() > 0) {
                HaloLogger.logE(TAG, "jni get road net success,crossLinks size=" + crossLinks.size() + ",mainRoad size=" + mainRoad.size());
                mPreStartBreak = breakStart;
                mPreEndBreak = breakEnd;
                mPreStepIndex = stepIndex;
                //2.将经纬度数据处理转换成Vector3数据,将主路拼接到原主路上,将其他link添加到路网中
                //2.1处理岔路--抽析--转换--填充到集合中
                for (int i = 0; i < crossLinks.size(); i++) {
                    List<LatLngOutSide> crossLink = crossLinks.get(i);
                    //抽析岔路
                    List<Vector3> crossLinkVector3 = new ArrayList<>();
                    List<PointF> returnPath = new ArrayList<>();
                    List<PointF> originalPath = new ArrayList<>();
                    List<Vector3> pathV3 = new ArrayList<>();
                    for (LatLngOutSide latlng : crossLink) {
                        ARWayProjection.PointD pd = ARWayProjection.toOpenGLLocation(new LatLng(latlng.lat, latlng.lng), DEFAULT_LEVEL);
                        pathV3.add(new Vector3(pd.x, -pd.y, DEFAULT_OPENGL_Z));
                    }
                    for (Vector3 v : pathV3) {
                        originalPath.add(new PointF((float) v.x, (float) v.y));
                    }
                    Douglas.rarefyGetPointFs(new ArrayList<Integer>(), returnPath, originalPath, RAREFY_PIXEL_COUNT / ARWayProjection.K);
                    HaloLogger.logE(TAG, "ori size = " + originalPath.size());
                    HaloLogger.logE(TAG, "ret size = " + returnPath.size());
                    for (PointF p : returnPath) {
                        crossLinkVector3.add(new Vector3((p.x - mOffsetX) * TIME_15_20, (p.y - mOffsetY) * TIME_15_20, DEFAULT_OPENGL_Z));
                    }
                    /*List<Vector3> crossLinkVector3 = new ArrayList<>();
                    for (LatLngOutSide latlng : crossLink) {
                        ARWayProjection.PointD pd = ARWayProjection.toOpenGLLocation(new LatLng(latlng.lat, latlng.lng), DEFAULT_LEVEL);
                        crossLinkVector3.add(new Vector3((pd.x - mOffsetX) * TIME_15_20, (-pd.y - mOffsetY) * TIME_15_20, DEFAULT_OPENGL_Z));
                    }*/
                    HaloLogger.logE(TAG, "crossLink cross start");
                    for (LatLngOutSide latlng : crossLink) {
                        HaloLogger.logE(TAG, latlng.lat + "," + latlng.lng);
                    }
                    HaloLogger.logE(TAG, "crossLink cross end");
                    //此links代表的是岔路
                    mBranchPaths.add(crossLinkVector3);
                    mBranchInPathIndexs.add(mStepPointIndexs.get(stepIndex));
                    Log.e("ylq", "add index = " + mStepPointIndexs.get(stepIndex));
                }
                /*HaloLogger.logE(TAG, "crossLink cross start");
                for (LatLngOutSide latlng : link) {
                    HaloLogger.logE(TAG, latlng.lat + "," + latlng.lng);
                }
                HaloLogger.logE(TAG, "crossLink cross end");
                HaloLogger.logE(TAG, "crossLink cross start");
                for (LatLngOutSide latlng : mainRoad) {
                    HaloLogger.logE(TAG, latlng.lat + "," + latlng.lng);
                }
                HaloLogger.logE(TAG, "crossLink cross end");*/
                //2.2处理新的中心点下表
                PrintUtils.printList(crossPointIndexs, "test_center", "cross indexs");
                int newCenterIndex = crossPointIndexs.remove(crossPointIndexs.size() - 1);
                if(!crossPointIndexs.contains(newCenterIndex)){
                    for(int i=0;i<crossPointIndexs.size();i++){
                        if(crossPointIndexs.get(i)>newCenterIndex){
                            crossPointIndexs.add(i,newCenterIndex);
                        }
                    }
                }
                //mProportionMappingEngine.mappingC(mStepPointIndexs.get(stepIndex),newCenterIndex);
                //2.2处理主路以及对主路部分进行抽析
                List<LatLng> subPath = new ArrayList<>();
                for (LatLngOutSide latlng : mainRoad) {
                    subPath.add(new LatLng(latlng.lat, latlng.lng));
                }
                HaloLogger.logE(TAG, "new center index = " + newCenterIndex);
                mProportionMappingEngine.mapping(subPath, breakStart, breakEnd, crossPointIndexs);

                /*HaloLogger.logE(TAG, "jiaodian cross start");
                for (int i = 0; i < crossPointIndexs.size(); i++) {
                    HaloLogger.logE(TAG, mainRoad.get(crossPointIndexs.get(i)).lat + "," + mainRoad.get(crossPointIndexs.get(i)).lng);
                }
                HaloLogger.logE(TAG, "jiaodian cross end");*/
            }
        }
        Log.e("ylq", "add end");
        HaloLogger.logE(TAG, "process steps end");
    }

    /**
     * 根据覆盖区域大小,中心点角标,以及原Path求出该覆盖区域的部分Path
     * 并填充到out中
     *
     * @param szCover
     * @param stepIndex
     * @param path
     * @param centerLatLng
     * @param link         [out]
     * @return
     */
    private int[] getPartPathFromCover(Size2iOutside szCover, int stepIndex, List<LatLng> path, LatLngOutSide centerLatLng, List<LatLngOutSide> link, LatLng[] point8) {
        link.clear();
        double latlng_width = szCover.width * PIXEL_2_LATLNG;
        double latlng_height = szCover.height * PIXEL_2_LATLNG;
        //LatLng[] point8 = new LatLng[8];
        //上,右,下,左
        point8[0] = new LatLng(centerLatLng.lat - latlng_width / 2, centerLatLng.lng - latlng_height / 2);
        point8[1] = new LatLng(centerLatLng.lat + latlng_width / 2, centerLatLng.lng - latlng_height / 2);
        point8[2] = new LatLng(centerLatLng.lat + latlng_width / 2, centerLatLng.lng - latlng_height / 2);

        point8[3] = new LatLng(centerLatLng.lat + latlng_width / 2, centerLatLng.lng + latlng_height / 2);
        point8[4] = new LatLng(centerLatLng.lat + latlng_width / 2, centerLatLng.lng + latlng_height / 2);
        point8[5] = new LatLng(centerLatLng.lat - latlng_width / 2, centerLatLng.lng + latlng_height / 2);
        point8[6] = new LatLng(centerLatLng.lat - latlng_width / 2, centerLatLng.lng + latlng_height / 2);
        point8[7] = new LatLng(centerLatLng.lat - latlng_width / 2, centerLatLng.lng - latlng_height / 2);
        /*HaloLogger.logE(TAG, "crossLink cross start");
        for (LatLng latlng : point8) {
            HaloLogger.logE(TAG, latlng.latitude + "," + latlng.longitude);
        }
        HaloLogger.logE(TAG, "crossLink cross end");*/
        link.add(new LatLngOutSide(centerLatLng.lat, centerLatLng.lng));
        //breakStart:JNI部分数据返回后用于拼接抽析数据部分的开始下标
        //breakEnd:结束下标
        int breakStart = 0;
        for (int i = mStepPointIndexs.get(stepIndex) - 1; i >= 0; i--) {
            LatLng latlng = path.get(i);
            double offsetLat = Math.abs(centerLatLng.lat - latlng.latitude);
            double offsetLng = Math.abs(centerLatLng.lng - latlng.longitude);
            if (offsetLat >= latlng_width / 2 || offsetLng >= latlng_height / 2) {
                if (offsetLat == latlng_width / 2 || offsetLng == latlng_height / 2) {
                    link.add(0, new LatLngOutSide(latlng.latitude, latlng.longitude));
                    breakStart = i <= 0 ? 0 : i - 1;
                } else {
                    LatLng preLatLng = path.get(i + 1);
                    for (int j = 0; j < point8.length; j += 2) {
                        LatLng lineStart = point8[j];
                        LatLng lineEnd = point8[j + 1];
                        Vector3 result = new Vector3();
                        int res = MathUtils.getIntersection(new Vector3(latlng.latitude, latlng.longitude, 0),
                                                            new Vector3(preLatLng.latitude, preLatLng.longitude, 0),
                                                            new Vector3(lineStart.latitude, lineStart.longitude, 0),
                                                            new Vector3(lineEnd.latitude, lineEnd.longitude, 0),
                                                            result);
                        if (res == 1) {
                            link.add(0, new LatLngOutSide(result.x, result.y));
                            breakStart = i;
                            break;
                        }
                    }
                }
                break;
            } else if (i == 0) {
                breakStart = 0;
                link.add(0, new LatLngOutSide(latlng.latitude, latlng.longitude));
                break;
            } else {
                link.add(0, new LatLngOutSide(latlng.latitude, latlng.longitude));
            }
        }
        int breakEnd = 0;
        for (int i = mStepPointIndexs.get(stepIndex) + 1; i < path.size(); i++) {
            LatLng latlng = path.get(i);
            double offsetLat = Math.abs(centerLatLng.lat - latlng.latitude);
            double offsetLng = Math.abs(centerLatLng.lng - latlng.longitude);
            if (offsetLat >= latlng_width / 2 || offsetLng >= latlng_height / 2) {
                if (offsetLat == latlng_width / 2 || offsetLng == latlng_height / 2) {
                    link.add(new LatLngOutSide(latlng.latitude, latlng.longitude));
                    breakEnd = i >= path.size() - 1 ? path.size() - 1 : i + 1;
                    HaloLogger.logE(TAG, "offset bigger than width or height,offsetLat == latlng_width/2");
                } else {
                    LatLng preLatLng = path.get(i - 1);
                    for (int j = 0; j < point8.length; j += 2) {
                        LatLng lineStart = point8[j];
                        LatLng lineEnd = point8[j + 1];
                        Vector3 result = new Vector3();
                        int res = MathUtils.getIntersection(new Vector3(latlng.latitude, latlng.longitude, 0),
                                                            new Vector3(preLatLng.latitude, preLatLng.longitude, 0),
                                                            new Vector3(lineStart.latitude, lineStart.longitude, 0),
                                                            new Vector3(lineEnd.latitude, lineEnd.longitude, 0),
                                                            result);
                        if (res == 1) {
                            link.add(new LatLngOutSide(result.x, result.y));
                            breakEnd = i;
                            break;
                        }
                    }
                    HaloLogger.logE(TAG, "offset bigger than width or height,res == ???");
                }
                break;
            } else if (i == path.size() - 1) {
                HaloLogger.logE(TAG, "offset bigger than width or height,i == path.size()-1");
                breakEnd = path.size() - 1;
                link.add(new LatLngOutSide(latlng.latitude, latlng.longitude));
                break;
            } else {
                link.add(new LatLngOutSide(latlng.latitude, latlng.longitude));
            }
        }
        return new int[]{breakStart, breakEnd};
    }

    /**
     * 默认准备三级数据,(20,18,16)当渲染层每申请一层数据,除了将数据返回外都判断
     * 是否需要替换数据,例如渲染层申请了16级的数据,那么此时就需要将20级数据替换成15级
     * TODO 这种方案仅适用于渲染层会顺序申请数据,而不是跨等级申请
     */
    private void prepareLevelData() {

    }

    private int fromOriIndex2RenderIndex(List<Integer> pointIndexsToKeep, int oriIndex, boolean behindMe) {
        for (int i = 0; i < pointIndexsToKeep.size(); i++) {

            if (!(pointIndexsToKeep.get(i) < oriIndex)) {
                if (behindMe) {
                    return i;
                } else {
                    return i == 0 ? 0 : i - 1;
                }
            }
        }
        return oriIndex;
    }

    /**
     * 求当前点在path中的哪个位置
     *
     * @param currentPoint
     * @param currentStep
     * @return
     */
    private int getIndexInPath(int currentPoint, int currentStep) {
        int currentIndex = 0;
        for (int i = 0; i < currentStep && mStepLengths != null; i++) {
            currentIndex += mStepLengths.get(i);
        }
        currentIndex += currentPoint;
        if (currentIndex >= mTotalSize) {
            currentIndex = mTotalSize - 1;
        }
        return currentIndex;
    }

    /**
     * 处理由一个location转换成Rajawali可用的vector3的过程以及其中的一些数据处理
     *
     * @param location
     * @param curIndex
     * @return
     */
    private Vector3 convertLocation(AMapNaviLocation location, int curIndex) {
        LatLng latlng = new LatLng(location.getCoord().getLatitude(), location.getCoord().getLongitude());
        //latlng = mProportionMappingEngine.mapping(latlng, curIndex);
        //ARWayProjection.PointD pointD = ARWayProjection.toOpenGLLocation(latlng, DEFAULT_LEVEL);
        //不使用上面那种先映射到LatLng,再转换成opengl坐标,而是采用下面这种直接映射到opengl坐标的原因:
        //  如果映射的结果是LatLng,那么因为计算产生的微小误差就会被放大(经纬度对经度要求非常高),因此此处采取
        //  取映射后的opengl坐标,这样因为LatLng产生的误差就会被转换成opengl之前消除
        ARWayProjection.PointD pointD = mProportionMappingEngine.mappingV(latlng, curIndex);
        Vector3 v = new Vector3(
                (pointD.x - mOffsetX) * TIME_15_20,
                (-pointD.y - mOffsetY) * TIME_15_20,
                DEFAULT_OPENGL_Z);
        /*for (int i = 0; i < mPointIndexsToKeep.size(); i++) {
            if (!(mPointIndexsToKeep.get(i) < curIndex)) {
                Vector3 line_start = null;
                Vector3 line_end = null;
                if (mPointIndexsToKeep.get(i) == curIndex && i != mPointIndexsToKeep.size() - 1) {
                    line_start = mDouglasPath.get(i);
                    line_end = mDouglasPath.get(i + 1);
                } else if (mPointIndexsToKeep.get(i) > curIndex && i != 0) {
                    line_start = mDouglasPath.get(i - 1);
                    line_end = mDouglasPath.get(i);
                }
                PointF pProjection = new PointF();
                MathUtils.getProjectivePoint(new PointF((float) line_start.x, (float) line_start.y),
                                             new PointF((float) line_end.x, (float) line_end.y),
                                             new PointF((float) v.x, (float) v.y),
                                             pProjection);
                v.x = pProjection.x;
                v.y = pProjection.y;
                break;
            }
        }*/

        //根据不同的道路等级,拿到的GPS转换到的opengl点也需要做响应的转换,默认情况下factor=1
        int factor = mNaviPathDataProvider.getCurDataLevelFactor();
        v.x /= factor;
        v.y /= factor;
        v.z /= factor;
        double offsetX = mNaviPathDataProvider.getCurOffsetX();
        double offsetY = mNaviPathDataProvider.getCurOffsetY();
        v.x -= offsetX;
        v.y -= offsetY;
        return v;
    }
}
