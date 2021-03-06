package com.amap.navi.demo.util;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.AvoidXfermode;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Bitmap.Config;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Message;
import android.util.Log;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.Projection;
import com.amap.api.maps.model.LatLng;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviStep;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.navi.demo.activity.BaseActivity;
import com.amap.navi.demo.activity.IndexActivity;

/**
 * 根据传入的坐标集合绘制HUDWay路线 通过当前位置实时更新路线
 *
 * @author 龙
 */
public class HUDWayBitmapFactory2 {

    private final List<NaviLatLng> mPathLatLngs             = new ArrayList<NaviLatLng>();
    private final List<Integer>    mCroodsInSteps           = new ArrayList<Integer>();
    private final List<NaviLatLng> mTrafficLightNaviLatLngs = new ArrayList<NaviLatLng>();
    private final List<Float>      mPointsDistance          = new ArrayList<Float>();
    private final List<Boolean>    mIsRedLine               = new ArrayList<Boolean>();
    private final List<Point>      mRightSidePoints         = new ArrayList<Point>();
    private final List<Point>      mLeftSidePoints          = new ArrayList<Point>();
    private final List<Point>      mTempPoints              = new ArrayList<Point>();

    //计算当前点到集合中的currentIndex间的距离
    private static int   HUDWAY_LENGTH_IN_SCREEN = 350;
    //参照物点之间的距离
    private static final int   HUDWAY_POINT_INTERVAL   = 40;
    //路线的放大倍数
    private static float MAGNIFIED_TIME          = 1f;
    //路线错乱的容忍值当点的Y坐标大于起始点的Y坐标+TOLERATE_VALUE,代表绘制该点可能会出现错乱的情况
    private static final int   TOLERATE_VALUE          = 70;

    private int BITMAP_WIDTH       = 0;//475;
    private int BITMAP_HEIGHT      = 0;//270 + 80;
    private int OUTSIDE_LINE_WIDTH = 0;//350;
    private int MIDDLE_LINE_WIDTH  = 0;//330;
    private int INSIDE_LINE_WIDTH  = 0;//310;
    private int CIRCLE_LINE_WIDTH  = 0;//45;

    private int              mCurrentIndex             = 1;
    private float            mCurPoint2NextPointDist   = 0f;
    private Projection       mProjection               = null;
    private Context          mContext                  = null;
    private Path             mRectPath                 = new Path();
    private Paint            mPaint4CrossImageCanvas   = new Paint();
    private Paint            mPaintFilterBitmapColor   = new Paint();
    private Bitmap           mCrossImage               = null;
    private Bitmap           mCrossImageTarget         = null;
    private AMapNaviLocation mCrossImageLastLatLng     = null;
    private int              mCrossImageRetainDistance = 0;
    //已经走过的距离(距离初始路口放大图)
    private float            mCrossImageDist           = 0f;
	private int m300Index = 0;
	private boolean mFirst = false;
	private boolean m300InPaths = true;
	private int mCount = 0;
	private float mDegrees;

    public HUDWayBitmapFactory2(Context context, Projection projection) {
        this.mContext = context;
        this.mProjection = projection;
        this.mPaint4CrossImageCanvas.setAlpha(100);
        this.mPaint4CrossImageCanvas.setAntiAlias(true);
        this.mPaintFilterBitmapColor.setDither(true);
        this.mPaintFilterBitmapColor.setFilterBitmap(true);
        this.mPaintFilterBitmapColor.setARGB(0, 0, 0, 0);
        this.mPaintFilterBitmapColor.setXfermode(new AvoidXfermode(0x000000, 10, AvoidXfermode.Mode.TARGET));
    }

    /**
     * init line width with bitmap_width and bitmap_height
     *
     * @param bitmap_width  bitmap`s width
     * @param bitmap_height bitmap`s height
     */
    public void initDrawLine(int bitmap_width, int bitmap_height) {
        this.BITMAP_WIDTH = formatAsEvenNumber(bitmap_width);
        this.BITMAP_HEIGHT = formatAsEvenNumber(bitmap_height);
        this.OUTSIDE_LINE_WIDTH = formatAsEvenNumber(Math.round(this.BITMAP_WIDTH * 0.037f));// 350/475
        this.MIDDLE_LINE_WIDTH = formatAsEvenNumber(Math.round(this.BITMAP_WIDTH * 0.025f));//0.495f 330/475
        this.INSIDE_LINE_WIDTH = formatAsEvenNumber(Math.round(this.BITMAP_WIDTH * 0.013f));// 310/474
        this.CIRCLE_LINE_WIDTH = formatAsEvenNumber(Math.round(this.BITMAP_WIDTH * 0.015f)) + 2;// 45/475
    }

    private int formatAsEvenNumber(int number) {
        if (IsOddNumber(number)) {
            return number - 1;
        } else {
            return number;
        }
    }

    private boolean IsOddNumber(int n) {
        return n % 2 != 0;
    }

    /**
     * set the lat lng list for route and screen points
     *
     * @param naviStepList
     */
    public void setRouteLatLngAndScreenPoints(List<AMapNaviStep> naviStepList) {
        init();
        for (int i = 0; i < naviStepList.size(); i++) {
            mCroodsInSteps.add(naviStepList.get(i).getCoords().size());
        }

        for (AMapNaviStep aMapNaviStep : naviStepList) {
            mPathLatLngs.addAll(aMapNaviStep.getCoords());
        }

        for (int i = 0; i < mPathLatLngs.size() - 1; i++) {
            mPointsDistance.add(AMapUtils.calculateLineDistance(naviLatLng2LatLng(mPathLatLngs.get(i)), naviLatLng2LatLng(mPathLatLngs.get(i + 1))));
            if (mPointsDistance.get(i) <= 30) {
                mIsRedLine.add(true);
            } else {
                mIsRedLine.add(false);
            }
        }
    }

    /**
     * initialization status
     */
    private void init() {
        this.mPathLatLngs.clear();
        this.mCroodsInSteps.clear();
        this.mTrafficLightNaviLatLngs.clear();
        this.mPointsDistance.clear();
        this.mIsRedLine.clear();
        this.mRightSidePoints.clear();
        this.mLeftSidePoints.clear();
        this.mTempPoints.clear();
        this.mRectPath.reset();
        this.mCurrentIndex = 1;
        this.mCrossImageLastLatLng = null;
        this.mCrossImageRetainDistance = 0;
        this.mCrossImageDist = 0;
    }

    /**
     * draw hudway in the canvas
     *
     * @param can                the canvas for draw
     * @param location           the current location for car
     * @param mayBeErrorLocation true:the location may be error location , so do not to draw hudway . false:can draw hudway
     * @param crossImage         cross image to draw
     * @param realStartPoint     real point in screen without error
     */
    public void drawHudway(Canvas can, AMapNaviLocation location) {

    	HUDWAY_LENGTH_IN_SCREEN = IndexActivity.route_length;
    	MAGNIFIED_TIME = IndexActivity.mDraw_scale;
    	mFirst = true;
    	m300InPaths = true;
        //if location is null.
        if (location == null) {
            return;
        }

        // get the points for draw bitmap
        List<Point> currentPoints = getCurrentPoints(location);

        //Log.e("pointss", currentPoints+"");
        // if currentPoints is null or it`s size is zero , return
        if (currentPoints == null || currentPoints.size() <= 1) {
            return;
        }

        //if the point1 is look like point2 , remove it.
//        for (int i = 1; i < currentPoints.size(); i++) {
//            Point p1 = currentPoints.get(i - 1);
//            Point p2 = currentPoints.get(i);
//            if (Math.abs(p1.y - p2.y) < 3 && Math.abs(p1.x - p2.x) < 3) {
//                currentPoints.remove(i);
//                i--;
//            }
//        }

        //to cover last frame image
        can.drawColor(Color.BLACK);

        //create a bitmap to draw
        Bitmap hudwayBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);

        //create a mPaint and set attribute
        Paint paint = new Paint();
        Canvas canvas = new Canvas(hudwayBitmap);
        paint.setColor(Color.BLACK);
        canvas.drawPaint(paint);

        paint.setStrokeWidth(OUTSIDE_LINE_WIDTH);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);

        float[] pointsXY = new float[currentPoints.size() * 2];
        // get the points x and y
        for (int i = 0; i < pointsXY.length; i++) {
            if (i % 2 == 0) {
                pointsXY[i] = currentPoints.get(i / 2).x;
            } else {
                pointsXY[i] = currentPoints.get(i / 2).y;
            }
        }

        // move to screen center , here set 1120-130,1120 is the center
        float offsetX = BITMAP_WIDTH / 2 - pointsXY[0];
        float offsetY = BITMAP_HEIGHT - pointsXY[1]-100;
        for (int i = 0; i < pointsXY.length; i++) {
            pointsXY[i] = i % 2 == 0 ? pointsXY[i] + offsetX : pointsXY[i]
                    + offsetY;
        }

        // save this distance point to point, 2~1, 3~2 .....
        float[] distance = new float[pointsXY.length - 2];
        for (int i = 0; i < distance.length; i++) {
            distance[i] = pointsXY[i + 2] - pointsXY[i];
        }

        // Magnified N times
        // start point is constant，another points will change
        for (int i = 2; i < pointsXY.length; i++) {
            pointsXY[i] = pointsXY[i - 2] + distance[i - 2] * MAGNIFIED_TIME;
        }

        //remove the point in list if it may be error to draw
        Point firstPoint = new Point((int) pointsXY[0], (int) pointsXY[1]);
        mTempPoints.clear();
        mTempPoints.add(firstPoint);
        for (int i = 1; i < pointsXY.length / 2; i++) {
            Point tempPoint = new Point();
            if (pointsXY[i * 2 + 1] > firstPoint.y + TOLERATE_VALUE) {
                //TODO 计算得到的舍弃点的补偿点的坐标在某些情况下有问题（去深圳湾的掉头时，补偿点会画到屏幕右侧）
                //                tempPoint.y = firstPoint.y;
                //                tempPoint.x = (int) (tempPoint.y*(pointsXY[i*2]+pointsXY[i*2-2])/(pointsXY[i*2+1]+pointsXY[i*2-1]));
                //                mTempPoints.add(tempPoint);
                break;
            } else {
                tempPoint.x = (int) pointsXY[i * 2];
                tempPoint.y = (int) pointsXY[i * 2 + 1];
                mTempPoints.add(tempPoint);
            }
        }
        
        if(mTempPoints.size()<=1){
        	return;
        }

        pointsXY = new float[mTempPoints.size() * 2];
        for (int i = 0; i < mTempPoints.size(); i++) {
            pointsXY[i * 2] = mTempPoints.get(i).x;
            pointsXY[i * 2 + 1] = mTempPoints.get(i).y;
        }

        // here we must be 3D turn around first ,and rotate the path second.
        // first:3D turn around and set matrix
//        setRotateMatrix4Canvas(pointsXY[0], pointsXY[1], -100.0f, 50f, canvas);

        // if the line is vertical
        mDegrees = 0;
	        if (pointsXY[2] == pointsXY[0]) {
	            if (pointsXY[3] <= pointsXY[1]) {
	                mDegrees = 0;
	            } else {
	                mDegrees = 180;
	            }
	            // if the line is horizontal
	        } else if (pointsXY[3] == pointsXY[1]) {
	            if (pointsXY[2] <= pointsXY[0]) {
	                mDegrees = 90;
	            } else {
	                mDegrees = 270;
	            }
	        } else {
	            // if the line is not a vertical or horizontal,we should be to
	            // calculate the degrees
	            // cosA = (c*c + b*b - a*a)/(2*b*c)
	            // A = acos(A)/2/PI*360
	            double c = Math.sqrt(Math.pow(Math.abs(pointsXY[0] - pointsXY[2]),
	                                          2.0) + Math.pow(Math.abs(pointsXY[1] - pointsXY[3]), 2.0));
	            double b = Math.abs(pointsXY[1] - pointsXY[3]);
	            double a = Math.abs(pointsXY[0] - pointsXY[2]);
	            mDegrees = (float) (Math.acos((c * c + b * b - a * a) / (2 * b * c))
	                    / 2 / Math.PI * 360);
	            if (pointsXY[2] >= pointsXY[0] && pointsXY[3] >= pointsXY[1]) {
	                mDegrees += 180;
	            } else if (pointsXY[2] <= pointsXY[0] && pointsXY[3] >= pointsXY[1]) {
	                mDegrees = (90 - mDegrees) + 90;
	            } else if (pointsXY[2] <= pointsXY[0] && pointsXY[3] <= pointsXY[1]) {
	                mDegrees += 0;
	            } else if (pointsXY[2] >= pointsXY[0] && pointsXY[3] <= pointsXY[1]) {
	
	                mDegrees = 270 + (90 - mDegrees);
	            }
	        }

        //rotate the path because the fakeLocation is created by our caculate , so it may be has a little error.
        
        canvas.rotate(mDegrees, pointsXY[0], pointsXY[1]);


        // draw the path
        Path basePath = new Path();
        Path redPointPath = new Path();
        Path circlePath = new Path();

		/*//将数组中的内容倒置，改善线的显示效果
        for(int i=0 ; i<pointsXY.length/2/2 ; i++){
			float tempX = pointsXY[i*2];
			float tempY = pointsXY[i*2+1];
			pointsXY[i*2]=pointsXY[(pointsXY.length/2-1-i)*2];
			pointsXY[i*2+1]=pointsXY[(pointsXY.length/2-1-i)*2+1];
			pointsXY[(pointsXY.length/2-1-i)*2]=tempX;
			pointsXY[(pointsXY.length/2-1-i)*2+1]=tempY;
		}*/

        basePath.moveTo(pointsXY[0], pointsXY[1]);
        redPointPath.moveTo(pointsXY[0], pointsXY[1]);
        
        circlePath.addCircle(pointsXY[0], pointsXY[1], 6, Direction.CCW);
        for (int i = 0; i < pointsXY.length / 2 - 1; i++) {
        	if(this.m300InPaths || (i + 1) * 2!=this.m300Index*2){
        		circlePath.addCircle(pointsXY[(i + 1) * 2], pointsXY[(i + 1) * 2 + 1], 6, Direction.CCW);
        	}
            if (mCurrentIndex + i - 1 >= mPointsDistance.size()) {
                return;
            }
            float lineLength = i == 0 ? mCurPoint2NextPointDist : mPointsDistance.get(mCurrentIndex + i - 1);

            basePath.lineTo(pointsXY[(i + 1) * 2], pointsXY[(i + 1) * 2 + 1]);
            //TODO draw cicrle path
            /*if (i < pointsXY.length / 2 - 1 - 1) {
                // circlePath is the circle route , so it less one point to
				// shorter than outside line and inside line
				circlePath.lineTo(pointsXY[(i + 1) * 2],
						pointsXY[(i + 1) * 2 + 1]);
			}

			//add circle to circlePath for draw can move path
			//every 50 we will add a point,from end to start
			int pointCount=(int) (lineLength/HUDWAY_POINT_INTERVAL);
			//if the line length greater than HUDWAY_POINT_INTERVAL
			if(pointCount>0){
				float step_x = (pointsXY[(i + 1) * 2]-pointsXY[(i ) * 2])/lineLength*HUDWAY_POINT_INTERVAL;
				float step_y = (pointsXY[(i + 1) * 2 + 1]-pointsXY[(i) * 2 + 1])/lineLength*HUDWAY_POINT_INTERVAL;
				for(int j = 0 ; j < pointCount ; j ++){
					circlePath.addCircle(pointsXY[(i + 1) * 2]-(j+1)*step_x,pointsXY[(i+1 ) * 2 + 1]-(j+1)*step_y,10, Path.Direction.CW);
				}
			}*/

            //TODO draw red path
            /*if (mIsRedLine.get(mCurrentIndex + i - 1)) {
                redPointPath.lineTo(pointsXY[(i + 1) * 2], pointsXY[(i + 1) * 2 + 1]);
            } else {
                redPointPath.moveTo(pointsXY[(i + 1) * 2], pointsXY[(i + 1) * 2 + 1]);
                if (lineLength > 50) {
                    int pointCount = (int) (lineLength / HUDWAY_POINT_INTERVAL);
                    //if the line length greater than HUDWAY_POINT_INTERVAL
                    if (pointCount > 0) {
                        float step_x = (pointsXY[(i + 1) * 2] - pointsXY[(i) * 2]) / lineLength * HUDWAY_POINT_INTERVAL;
                        float step_y = (pointsXY[(i + 1) * 2 + 1] - pointsXY[(i) * 2 + 1]) / lineLength * HUDWAY_POINT_INTERVAL;
                        for (int j = 0; j < pointCount; j++) {
                            circlePath.addCircle(pointsXY[(i + 1) * 2] - (j + 1) * step_x, pointsXY[(i + 1) * 2 + 1] - (j + 1) * step_y, CIRCLE_LINE_WIDTH, Path.Direction.CW);
                        }
                    }
                }
            }*/
        }

        // set corner path for right angle(直角)

        //if the location point may be a error point ,do not to draw path and to draw text to warning user.

        //TODO TEST Do not to draw route when cross image is showed.
        /*if(crossImage!=null){
            paint.setColor(Color.BLACK);
        }*/
        // draw outside line
        canvas.drawPath(basePath, paint);
        
        //draw black line
        paint.setStrokeWidth(MIDDLE_LINE_WIDTH);
        paint.setColor(Color.BLACK);
        canvas.drawPath(basePath, paint);
        
        paint.setColor(Color.RED);
        canvas.drawPath(circlePath, paint);
        
        paint.setColor(Color.BLUE);
        try{
	        if(this.m300Index*2+1 < currentPoints.size()*2){
		        float cx = pointsXY[this.m300Index*2];
		        float cy = pointsXY[this.m300Index*2+1];
		        canvas.drawCircle(cx, cy, 5, paint);
	        }
        }catch(Exception e){
        	
        }

        //delete black color
        canvas.drawPaint(mPaintFilterBitmapColor);
        
        //create faker cross image
        if(BaseActivity.mFlushFakerCross){
        	try{
        		synchronized (HUDWayBitmapFactory2.class) {
        			int x = (int) pointsXY[(this.m300Index-1)*2];
        			int y = (int) pointsXY[(this.m300Index-1)*2+1];
        			
        			Bitmap temp = Bitmap.createBitmap(400, 400, Config.ARGB_8888);
        			Canvas canv = new Canvas(temp);
        			List<Point> points = new ArrayList<Point>();
        			points.add(0,new Point(x,y));
        			//向前取点
        			for(int i=this.m300Index-2;i>=0;i--){
        				int q = (int) pointsXY[i*2];
        				int w = (int) pointsXY[i*2+1];
        				points.add(0,new Point(q,w));
        				if(w-y>400){
        					break;
        				}
        			}
        			//向后取点
        			for(int i=this.m300Index;i<pointsXY.length/2-1;i++){
        				int q = (int) pointsXY[i*2];
        				int w = (int) pointsXY[i*2+1];
        				points.add(new Point(q,w));
//        				if(w-y<400){
//        					break;
//        				}
        			}
        			
        			canv.drawColor(Color.BLACK);
        			
        			float offsetX_ = 400 / 2 - x;
    		        float offsetY_ = 400 / 2 - y;//-100
    		        for (int i = 0; i <points.size(); i++) {
    		            points.get(i).x = (int) (points.get(i).x+offsetX_);
    		            points.get(i).y = (int) (points.get(i).y+offsetY_);
    		        }
    		        
    		        Path mypath = new Path();
    		        Path cicPath = new Path();
    		        mypath.moveTo(points.get(0).x, points.get(0).y);
    		        cicPath.addCircle(points.get(0).x, points.get(0).y, 10, Direction.CCW);
    		        for(int i=1;i<points.size();i++){
    		        	mypath.lineTo(points.get(i).x, points.get(i).y);
    		        	cicPath.addCircle(points.get(i).x, points.get(i).y, 10, Direction.CCW);
    		        }
    		        paint.setColor(Color.WHITE);
    		        canv.drawPath(mypath, paint);
    		        paint.setStyle(Style.FILL);
    		        paint.setColor(Color.RED);
    		        canv.drawPath(cicPath, paint);
    		        paint.setColor(Color.BLUE);
    		        canv.drawCircle(x+offsetX_, y+offsetY_, 11, paint);
        			
        			
        			
//		        	paint.setColor(Color.GREEN);
//		        	canvas.drawCircle(x, y, 6, paint);
//		        	Log.e("test", "=================x:"+x+" , y:"+y);
//		        	Bitmap fakerCrossImage = Bitmap.createBitmap(400, 400, hudwayBitmap.getConfig());
//		        	Canvas temp_canvas = new Canvas(fakerCrossImage);
//		        	temp_canvas.drawColor(Color.BLACK);
//		        	temp_canvas.drawBitmap(
//		        			hudwayBitmap, 
//		        			new Rect((int)(x-200), (int)(y-200), (int)(x+200), (int)(y+200)),//src
//		        			new Rect(0,0,400,400),  //dst
//		        			null);
		        	if(mCount++  == 20){
		        		BaseActivity.mFlushFakerCross = false;
		        		mCount = 0;
		        		Message msg = Message.obtain();
		        		msg.obj = temp;
		        		BaseActivity.mHandler.sendMessage(msg );
		        	}
        		}
        	}catch(Exception e){
        		Log.e("test",e.toString());
        	}
        }

        //draw red line
        //		mPaint.setColor(Color.RED);
        //		canvas.drawPath(redPointPath, mPaint);

        // draw inside line
        //      mPaint.setStrokeWidth(INSIDE_LINE_WIDTH);
        //      mPaint.setColor(Color.BLACK);
        //      canvas.drawPath(basePath, mPaint);

        //TODO draw rectPath to see real road rect
        /*if (!mRectPath.isEmpty()) {
            mPaint.setColor(Color.RED);
            mPaint.setStrokeWidth(10);
            canvas.drawPath(mRectPath, mPaint);
        }*/

        /*// draw can move point
        mPaint.setColor(Color.RED);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setStrokeWidth(CIRCLE_LINE_WIDTH);
		canvas.drawPath(circlePath,mPaint);*/


        Matrix initMatrix = new Matrix();
        can.setMatrix(initMatrix);
        //finally draw arway to surface view.
        can.drawBitmap(hudwayBitmap, 0, 0, null);

    }

    /**
     * set matrix to canvas with rotate and translate.
     *
     * @param translateX
     * @param translateY
     * @param offsetX
     * @param rotateXDegrees
     * @param canvas
     */
    private void setRotateMatrix4Canvas(float translateX, float translateY, float offsetX, float rotateXDegrees, Canvas canvas) {
        final Camera camera = new Camera();
        @SuppressWarnings("deprecation")
        final Matrix matrix = canvas.getMatrix();
        // save the camera status for restore
        camera.save();
        // around X rotate N degrees
        //		camera.rotateX(50);
        //		camera.translate(0.0f, -100f, 0.0f);
        camera.rotateX(rotateXDegrees);
        camera.translate(0.0f, offsetX, 0.0f);
        //x = -500 则为摄像头向右移动
        //y = 200 则为摄像头向下移动
        //z = 500 则为摄像头向高处移动
        // get the matrix from camera
        camera.getMatrix(matrix);
        // restore camera from the next time
        camera.restore();
        matrix.preTranslate(-translateX, -translateY);
        matrix.postTranslate(translateX, translateY);
        canvas.setMatrix(matrix);
    }

    /**
     * 根据当前的location获取到用于绘制的屏幕点的集合
     *
     * @return 用于绘制Hudway的点的集合
     */
    public List<Point> getCurrentPoints(AMapNaviLocation location) {
        List<Point> points = new ArrayList<Point>();
        fullPoints(location.getCoord(), points);
        return points;
    }

    /**
     * calculate to full points to draw path bitmap
     *
     * @param currentLatLng our car current location latlng
     * @param points        points list from draw the path bitmap
     */
    private void fullPoints(NaviLatLng currentLatLng, List<Point> points) {
        if (currentLatLng == null || mPathLatLngs == null || mPathLatLngs.size() <= 0) {
            return;
        }
        float totalLength = 0;
        points.clear();
        Point currentScreenPoint = mProjection
                .toScreenLocation(naviLatLng2LatLng(currentLatLng));
        points.add(currentScreenPoint);
        for (int i = mCurrentIndex; i < mPathLatLngs.size(); i++) {
            Point pathPoint = mProjection
                    .toScreenLocation(naviLatLng2LatLng(mPathLatLngs.get(i)));
            float distance = 0;
            if (i == mCurrentIndex) {
                if (currentScreenPoint.equals(pathPoint)) {
                    continue;
                }
                this.mCurPoint2NextPointDist = AMapUtils
                        .calculateLineDistance(
                                naviLatLng2LatLng(currentLatLng),
                                naviLatLng2LatLng(mPathLatLngs.get(i)));
                distance = mCurPoint2NextPointDist;
                totalLength += mCurPoint2NextPointDist;
            } else {
                if (pathPoint.equals(mProjection.toScreenLocation(
                        naviLatLng2LatLng(mPathLatLngs.get(i - 1))))) {
                    continue;
                }
                distance = AMapUtils.calculateLineDistance(
                        naviLatLng2LatLng(mPathLatLngs.get(i - 1)),
                        naviLatLng2LatLng(mPathLatLngs.get(i)));
                totalLength += distance;
            }
           
            //be sure the total distance is HUDWAY_LENGTH_IN_SCREEN
            if (totalLength == HUDWAY_LENGTH_IN_SCREEN) {
                points.add(pathPoint);
                return;
            } else if (totalLength > HUDWAY_LENGTH_IN_SCREEN) {
                float div = totalLength - HUDWAY_LENGTH_IN_SCREEN;
                Point prePoint = null;
                if (i == mCurrentIndex) {
                    prePoint = mProjection
                            .toScreenLocation(naviLatLng2LatLng(currentLatLng));
                } else {
                    prePoint = mProjection
                            .toScreenLocation(naviLatLng2LatLng(mPathLatLngs.get(i - 1)));
                }
                Point makePoint = new Point(
                        (int) (prePoint.x + (pathPoint.x - prePoint.x) * ((distance - div) / distance)),
                        (int) (prePoint.y + (pathPoint.y - prePoint.y) * ((distance - div) / distance)));
                points.add(makePoint);
                return;
            } else {
            	if(mFirst){
	            	if(totalLength == 300){
	            		this.m300Index = points.size();
	            		mFirst = false;
	            		this.m300InPaths = true;
	            	}else if(totalLength > 300){
	            		float div = totalLength - 300;
	                    Point prePoint = null;
	                    if (i == mCurrentIndex) {
	                        prePoint = mProjection
	                                .toScreenLocation(naviLatLng2LatLng(currentLatLng));
	                    } else {
	                        prePoint = mProjection
	                                .toScreenLocation(naviLatLng2LatLng(mPathLatLngs.get(i - 1)));
	                    }
	                    Point makePoint = new Point(
	                            (int) (prePoint.x + (pathPoint.x - prePoint.x) * ((distance - div) / distance)),
	                            (int) (prePoint.y + (pathPoint.y - prePoint.y) * ((distance - div) / distance)));
	                    points.add(makePoint);
	                    this.m300Index = points.size()-1;
	                    mFirst  = false;
	                    this.m300InPaths = false;
	            	}	
            	}
            		points.add(pathPoint);
            	
            }
        }
        return;
    }

    private LatLng naviLatLng2LatLng(NaviLatLng naviLatLng) {
        return naviLatLng == null ? null : new LatLng(naviLatLng.getLatitude(), naviLatLng.getLongitude());
    }

    /**
     * set index for current location `s next point
     *
     * @param curPoint current point index
     * @param curStep  current step index
     */
    public void setCurrentIndex(int curPoint, int curStep) {
        mCurrentIndex = 0;
        for (int i = 0; i < curStep; i++) {
            mCurrentIndex += mCroodsInSteps.get(i);
        }
        mCurrentIndex += curPoint + 1;
        if (mCurrentIndex >= mPathLatLngs.size()) {
            mCurrentIndex = mPathLatLngs.size() - 1;
        }
    }

    /**
     * draw test line
     *
     * @param width    bitmap width
     * @param height   bitmap height
     * @param location current location
     * @param degrees  bitmap`s degrees
     * @return
     */
    public Bitmap createLineTest(int width, int height, AMapNaviLocation location, float degrees) {
        if (mProjection == null || mCurrentIndex <= 0 || mCurrentIndex >= mPathLatLngs.size() - 1) {
            return null;
        }
        Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bm);
        Point locPoint = mProjection.toScreenLocation(naviLatLng2LatLng(location.getCoord()));
        Point prePoint = mProjection.toScreenLocation(naviLatLng2LatLng(mPathLatLngs.get(mCurrentIndex - 1)));
        Point curPoint = mProjection.toScreenLocation(naviLatLng2LatLng(mPathLatLngs.get(mCurrentIndex)));
        Point nextPoint = mProjection.toScreenLocation(naviLatLng2LatLng(mPathLatLngs.get(mCurrentIndex + 1)));

        Paint paint = new Paint();
        paint.setStrokeWidth(20);
        paint.setColor(Color.YELLOW);
        canvas.drawPoint(locPoint.x, locPoint.y, paint);
        paint.setStrokeWidth(10);
        paint.setColor(Color.RED);
        canvas.drawLine(prePoint.x, prePoint.y, curPoint.x, curPoint.y, paint);
        paint.setColor(Color.GREEN);
        canvas.drawLine(curPoint.x, curPoint.y, nextPoint.x, nextPoint.y, paint);
        paint.setTextSize(30);
        paint.setColor(Color.WHITE);
        canvas.drawText("curIndex : " + mCurrentIndex, 300, 500, paint);
        canvas.rotate(degrees);

        return bm;
    }

    /**
     * 根据
     *
     * @param crossImage a cross bitmap to cut
     * @param pointsXY   cut with point list
     * @return
     */
    private Bitmap cutBitmap(Bitmap crossImage, float[] pointsXY) {
        Path rectPath = points2path(pointsXY);

        int width = BITMAP_WIDTH;
        int height = BITMAP_HEIGHT;
        int LINE_WIDTH = (OUTSIDE_LINE_WIDTH - MIDDLE_LINE_WIDTH) / 2;

        Bitmap target = Bitmap.createBitmap(width, height, crossImage.getConfig());
        Canvas temp_canvas = new Canvas(target);

        Path bitmap_path = new Path();
        bitmap_path.moveTo(pointsXY[0] - crossImage.getWidth() / 2 - LINE_WIDTH / 2,
                           pointsXY[1] - crossImage.getHeight());
        bitmap_path.lineTo(pointsXY[0] + crossImage.getWidth() / 2 + LINE_WIDTH / 2,
                           pointsXY[1] - crossImage.getHeight());
        bitmap_path.lineTo(pointsXY[0] + crossImage.getWidth() / 2 + LINE_WIDTH / 2,
                           pointsXY[1]);
        bitmap_path.lineTo(pointsXY[0] - crossImage.getWidth() / 2 - LINE_WIDTH / 2,
                           pointsXY[1]);
        bitmap_path.close();
        if (bitmap_path.op(rectPath, Path.Op.INTERSECT)) {
            Paint paint = new Paint();
            paint.setStrokeWidth(1);
            paint.setStyle(Paint.Style.FILL);
            temp_canvas.drawPath(bitmap_path, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            temp_canvas.drawBitmap(crossImage, pointsXY[0] - crossImage.getWidth() / 2,
                                   pointsXY[1] - crossImage.getHeight(), paint);
        } else {
            temp_canvas.drawBitmap(crossImage, pointsXY[0] - crossImage.getWidth() / 2,
                                   pointsXY[1] - crossImage.getHeight(), null);
        }

        return target;
    }

    /**
     * 根据现有的points生成该点左右两侧的点的集合组成的RectPath
     *
     * @param pointsXY
     */
    private Path points2path(float[] pointsXY) {
        List<Point> points = new ArrayList<Point>();
        for (int i = 0; i < pointsXY.length; i += 2) {
            points.add(new Point((int) pointsXY[i], (int) pointsXY[i + 1]));
        }

        int path_width = MIDDLE_LINE_WIDTH;

        //获取一侧点的集合
        mRightSidePoints.clear();
        for (int i = 0; i < points.size(); i++) {
            Point currentPoint = points.get(i);
            Point secondPoint;
            int m = currentPoint.x;
            int n = currentPoint.y;
            if (i == points.size() - 1) {
                secondPoint = points.get(i - 1);
            } else {
                secondPoint = points.get(i + 1);
            }
            int a;
            int b;
            a = secondPoint.x;
            b = secondPoint.y;

            int x;
            int y;

            //m,n为B，a,b为A
            int x1 = m, y1 = n;
            int x2 = a, y2 = b;
            if (y2 == y1) {
                x = (int) (m - (b - n) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                y = (int) (n + (path_width / 2) * (a - m) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
            } else if (x2 == x1) {
                x = x1 + (path_width / 2);
                y = y1;
            } else if ((x2 < x1 && y2 > y1) || (x2 < x1 && y2 < y1)) {
                x = (int) (x1 + (path_width / 2) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                y = (int) (y1 - (path_width / 2) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
            } else {
                x = (int) (x1 - (path_width / 2) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                y = (int) (y1 + (path_width / 2) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
            }
            Point point = new Point(x, y);

            //如果不是第一个或者最后一个，那么需要取该点和该点的前一个点继续运算得到x,y，然后取中间值
            if (i != 0 && i != points.size() - 1) {
                secondPoint = points.get(i - 1);
                a = secondPoint.x;
                b = secondPoint.y;
                x1 = m;
                y1 = n;
                x2 = a;
                y2 = b;
                if (y2 == y1) {
                    x = (int) (m + (b - n) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                    y = (int) (n - (path_width / 2) * (a - m) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                } else if (x2 == x1) {
                    x = x1 + (path_width / 2);
                    y = y1;
                } else if ((x2 < x1 && y2 > y1) || (x2 < x1 && y2 < y1)) {
                    x = (int) (x1 - (path_width / 2) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                    y = (int) (y1 + (path_width / 2) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
                } else {
                    x = (int) (x1 + (path_width / 2) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                    y = (int) (y1 - (path_width / 2) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
                }
                point.x = (point.x + x) / 2;
                point.y = (point.y + y) / 2;
            }

            mRightSidePoints.add(point);
        }

        //获取另一侧点的集合
        mLeftSidePoints.clear();
        for (int i = 0; i < points.size(); i++) {
            Point currentPoint = points.get(i);
            Point secondPoint;
            int m = currentPoint.x;
            int n = currentPoint.y;
            if (i == points.size() - 1) {
                secondPoint = points.get(i - 1);
            } else {
                secondPoint = points.get(i + 1);
            }
            int a;
            int b;
            a = secondPoint.x;
            b = secondPoint.y;

            int x;
            int y;


            int x1 = m, y1 = n;
            int x2 = a, y2 = b;
            if (y2 == y1) {
                x = (int) (m + (b - n) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                y = (int) (n - (path_width / 2) * (a - m) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
            } else if (x2 == x1) {
                x = x1 - (path_width / 2);
                y = y1;
            } else if ((x2 < x1 && y2 > y1) || (x2 < x1 && y2 < y1)) {
                x = (int) (x1 - (path_width / 2) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                y = (int) (y1 + (path_width / 2) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
            } else {
                x = (int) (x1 + (path_width / 2) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                y = (int) (y1 - (path_width / 2) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
            }
            Point point = new Point(x, y);
            //如果不是第一个或者最后一个，那么需要取该点和该点的前一个点继续运算得到x,y，然后取中间值
            if (i != 0 && i != points.size() - 1) {
                secondPoint = points.get(i - 1);
                a = secondPoint.x;
                b = secondPoint.y;
                x1 = m;
                y1 = n;
                x2 = a;
                y2 = b;
                if (y2 == y1) {
                    x = (int) (m - (b - n) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                    y = (int) (n + (path_width / 2) * (a - m) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                } else if (x2 == x1) {
                    x = x1 - (path_width / 2);
                    y = y1;
                } else if ((x2 < x1 && y2 > y1) || (x2 < x1 && y2 < y1)) {
                    x = (int) (x1 + (path_width / 2) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                    y = (int) (y1 - (path_width / 2) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
                } else {
                    x = (int) (x1 - (path_width / 2) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                    y = (int) (y1 + (path_width / 2) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
                }
                point.x = (point.x + x) / 2;
                point.y = (point.y + y) / 2;
            }

            mLeftSidePoints.add(point);
        }

        //由于最后一个点的坐标是反向计算出来的，因此它的left和right是反的，在此做交换处理
        Point temp = mRightSidePoints.remove(mRightSidePoints.size() - 1);
        mRightSidePoints.add(mLeftSidePoints.remove(mLeftSidePoints.size() - 1));
        mLeftSidePoints.add(temp);

        //将点集合转成成矩形Path
        mRectPath.reset();
        Point point = mLeftSidePoints.get(0);
        mRectPath.moveTo(point.x, point.y);
        for (int i = 1; i < mRightSidePoints.size() + mLeftSidePoints.size(); i++) {
            if (i < mLeftSidePoints.size()) {
                point = mLeftSidePoints.get(i);
            } else {
                point = mRightSidePoints.get(mRightSidePoints.size() - (i - mLeftSidePoints.size() + 1));
            }
            mRectPath.lineTo(point.x, point.y);
        }

        /*
        //将点集合转换成Path集合，Path集合个数为原始点的个数减一(此处可表示为left或者right集合长度减一)
		mPaths.clear();
		for(int i=0;i<mRightSidePoints.size()-1;i++){
			Path path = new Path();
			Point leftCurrentPoint = mRightSidePoints.get(i);
			Point leftNextPoint = mRightSidePoints.get(i+1);
			Point rightCurrentPoint = mLeftSidePoints.get(i);
			Point rightNextPoint = mLeftSidePoints.get(i+1);
			path.moveTo(leftCurrentPoint.x,leftCurrentPoint.y);
			path.lineTo(leftNextPoint.x,leftNextPoint.y);
			path.lineTo(rightNextPoint.x,rightNextPoint.y);
			path.lineTo(rightCurrentPoint.x,rightCurrentPoint.y);
			path.close();
			mPaths.add(path);
		}*/
        return mRectPath;
    }
}
