package com.haloai.hud.hudendpoint.arwaylib.utils;


import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;

import com.haloai.hud.utils.HaloLogger;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;


/**
 * author       : 龙;
 * date         : 2016/5/6;
 * email        : helong@haloai.com;
 * package_name : com.haloai.hud.hudendpoint.arwaylib.utils;
 * project_name : hudlauncher;
 */
public class MathUtils {
    private static Matrix mMatrix4Rotate = new Matrix();

    public static <VALUE_TYPE extends Number> double getOffsetValue(VALUE_TYPE from, VALUE_TYPE to, long timeOffset, long duration) {
        double offsetValue = ((to.doubleValue() - from.doubleValue()) * (1.0 * timeOffset / duration));
        return offsetValue;
    }

    public static int formatAsEvenNumber(int number) {
        if (IsOddNumber(number)) {
            return number - 1;
        } else {
            return number;
        }
    }

    private static boolean IsOddNumber(int n) {
        return n % 2 != 0;
    }

    /**
     * 通过两点的坐标得到两点的距离
     *
     * @param point1
     * @param point2
     * @return
     */
    public static double calculateDistance(Point point1, Point point2) {
        //(y2-y1)^2+(x2-x1)^2=Z^2
        return calculateDistance(point1.x, point1.y, point2.x, point2.y);
    }

    public static double calculateDistance(PointF point1, PointF point2) {
        //(y2-y1)^2+(x2-x1)^2=Z^2
        return calculateDistance(point1.x, point1.y, point2.x, point2.y);
    }

    public static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow((y2 - y1), 2) + Math.pow((x2 - x1), 2));
    }

    /**
     * 得到两个点连成的线与一个角度之间的差值(正数为向右旋转,负数为向左旋转)
     *
     * @param preX
     * @param preY
     * @param curX
     * @param curY
     * @param cameraRotZ
     * @return
     */
    public static double getRotateDegreesWithLineAndAngle(double preX, double preY, double curX, double curY, double cameraRotZ) {
        double road_direction = MathUtils.rotateDegrees(MathUtils.getDegrees(preX, preY, curX, curY), 180);//顺时针方向，从6点方向旋转到12点方向
        road_direction = road_direction > 180 ? road_direction - 360 : road_direction;
        //                    double camera_direction = euler.z;//顺时针，12点方向为参考
        double camera_direction = Math.toDegrees(cameraRotZ);//顺时针，12点方向为参考
        //        double road_yaw = MathUtils.triPointYaw(pre.x, pre.y, cur.x, cur.y, next.x, next.y);//顺时针为正
        double camera_yaw = -camera_direction + road_direction;
        double cal_camera_yaw = camera_yaw;
        if (Math.abs(camera_yaw) > 180) {
            if (camera_yaw > 0) {
                cal_camera_yaw = camera_yaw - 360;
            } else {
                cal_camera_yaw = camera_yaw + 360;
            }
        }
        return cal_camera_yaw;
    }

    /**
     * 计算三个平面点之间的两个连续矢量方向变化，顺时针方向为正，逆时针方向为负
     * ax,ay,bx,by,cx,cy表示可构成三角形的三个点,对应边了a,b,c,最后求c的角度
     *
     * @param ax
     * @param ay
     * @param bx
     * @param by
     * @param cx
     * @param cy
     * @return
     */
    public static double getRotateDegreesWithTwoLines(double ax, double ay, double bx, double by, double cx, double cy) {
        double a, b, c, mx, my, result = 0, dgress = 0;
        a = calculateDistance(ax, ay, bx, by);
        b = calculateDistance(bx, by, cx, cy);
        c = calculateDistance(ax, ay, cx, cy);
        if (!isTriangle(a, b, c)) {
            return 0;
        } else {
            mx = (ax + cx) / 2;//求中点
            my = (ay + cy) / 2;//求中点
            if ((bx == mx) && (by == my)) {
                return 0;
            }
            dgress = triangleLength2Degress(c, b, a);
            if (!triClockWise(ax, ay, bx, by, cx, cy)) {//逆时针
                result = dgress - 180;
            } else {//顺时针
                result = 180 - dgress;
            }
        }
        return result;
    }

    /***
     * 判断三角形是否为顺时针方向
     *
     * @return
     */
    public static boolean triClockWise(double x1, double y1, double x2, double y2, double x3, double y3) {
        return ((x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1)) < 0;
    }

    /**
     * 以某个参照点逆时针旋转一个点
     *
     * @param angle 以弧度计算
     */

    public static Vector3 vector3Rotate(Vector3 src, Vector3 ref, double angle) {
        Vector3 result = null;
        Vector3 rPoint = new Vector3(src.x - ref.x, src.y - ref.y, src.z);
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        float x = (float) (rPoint.x * c - rPoint.y * s + ref.x);
        float y = (float) (rPoint.x * s + rPoint.y * c + ref.y);
        result = new Vector3(x, y, src.z);
        return result;
    }


    /**
     * 由三角开的三条边计算角度
     *
     * @param a 需要计算角的对边
     * @param b 计算角的邻边
     * @param c 计算角的邻边
     * @return 参数有误时，返回-1，返回角度在0~180
     */
    public static double triangleLength2Degress(double a, double b, double c) {
        if (!isTriangle(a, b, c)) {
            return -1;
        }
        double cosA = (b * b + c * c - a * a) / (2 * b * c);
        double dA = Math.toDegrees(Math.acos(cosA));
        return dA;
    }

    public static Vector3 getEulerDegeress(Quaternion qt) {
        Vector3 r = new Vector3();
        boolean reProject = false;
        double fator = 180 / Math.PI;
        double yaw, roll, pitch;
        yaw = qt.getYaw() * fator;
        roll = qt.getRoll() * fator;
        pitch = qt.getPitch() * fator;
        r.x = pitch;
        r.y = yaw;
        r.z = roll;
        return r;
    }

    /**
     * 由三角开的三条边计算角度
     *
     * @return
     */
    public static boolean isTriangle(double a, double b, double c) {
        if ((a > 0 && a > 0 && c > 0) && ((a + b) > c && (a + c) > b && (b + c) > a)) {
            return true;
        }
        return false;
    }

    /***
     * 顺时针方向重新映射角度
     *
     * @param from
     * @return 0~360
     */
    public static double rotateDegrees(double from, double rotate) {
        double to = from + rotate;
        int n = ((int) to) / 360;
        return to > 360 ? to - 360 * n : to;
    }

    /**
     * 获取两个屏幕点的连线与水平线之间的弧度
     *
     * @param x
     * @param y
     * @param x2
     * @param y2
     * @return
     */
    public static double getRadian(double x, double y, double x2, double y2) {
        return Math.atan2(y2 - y, x2 - x);
    }


    /***
     * 获取两个屏幕点的连线与水平线之间的距离
     *
     * @param x
     * @param y
     * @param x2
     * @param y2
     * @return
     */
    public static double getDegrees(float x, float y, float x2, float y2) {
        return getDegrees((double) x, (double) y, (double) x2, (double) y2);
    }

    public static double getDegrees(double x, double y, double x2, double y2) {
        double degrees = 0f;
        if (x2 == x) {
            if (y2 <= y) {
                degrees = 0;
            } else {
                degrees = 180;
            }
        } else if (y2 == y) {
            if (x2 <= x) {
                degrees = 90;
            } else {
                degrees = 270;
            }
        } else {
            double c = Math.sqrt(Math.pow(Math.abs(x - x2), 2.0) + Math.pow(Math.abs(y - y2), 2.0));
            double b = Math.abs(y - y2);
            double a = Math.abs(x - x2);
            degrees = (Math.acos((c * c + b * b - a * a) / (2 * b * c)) / 2 / Math.PI * 360);
            if (x2 >= x && y2 >= y) {
                degrees += 180;
            } else if (x2 <= x && y2 >= y) {
                degrees = (90 - degrees) + 90;
            } else if (x2 <= x && y2 <= y) {
                degrees += 0;
            } else if (x2 >= x && y2 <= y) {

                degrees = 270 + (90 - degrees);
            }
        }
        return degrees;
    }

    /**
     * 旋转一个bitmap,根据srcPointF,dstPointF以及centerPointF(默认0,0)
     * 旋转角度为(centerPointF,srcPointF)与(centerPointF,dstPointF)的夹角
     * <p/>
     * 根据向量坐标求出向量夹角的余弦值(PS:向量AB的另一个点均为原点)
     * (向量A 点乘 向量B)/(向量A的模 乘以 向量B的模) 等于 cosAB夹角
     * 如果给出
     * a=(X1,Y1),b=(X2,Y2)
     * 向量点积:a.b=X1X2+Y1Y2
     * 向量模相乘:|a|*|b|等于(X1平方+Y1平方)开方 * (X2平方+Y2平方)开方
     *
     * @param srcPointF  源点
     * @param dstPointF  目标点
     * @param crossImage 需要被旋转的bitmap
     * @return 旋转后的bitmap
     */
    public static Bitmap getRotateBitmap(PointF srcPointF, PointF dstPointF, Bitmap crossImage) {
        Matrix matrix = new Matrix();
        double degreesSrc = getDegrees(0, 0, srcPointF.x, srcPointF.y);
        double degreesDst = getDegrees(0, 0, dstPointF.x, dstPointF.y);
        matrix.setRotate((float) (degreesSrc - degreesDst));
        return Bitmap.createBitmap(crossImage, 0, 0, crossImage.getWidth(), crossImage.getHeight(), matrix, true);
    }

//    public static Vector3 longerPoint(Vector3 result , Vector3 start , Vector3 end, float dist){
//        double distance=Vector3.distanceTo(start,end);
//        result.x = start.x + (end.x - start.x) * dist / distance;
//        result.y = start.y + (end.y - start.y) * dist / distance;
//        return result;
//    }

    public static Vector3 longerPoint(Vector3 result , Vector3 start , double orientation, double dist){
        double k = Math.tan(orientation);
        double b = start.y-k*start.x;
        double angle = orientation;
        double deltaX = Math.cos(angle)*dist;

        result.x = start.x+deltaX;
        result.y = result.x*k+b;
        return result;
    }
    /**
     *
     * @param result
     * @param start
     * @param end
     * @param dist
     * @return
     */
    public static Vector3 longerPoint(Vector3 result , Vector3 start , Vector3 end, double dist){

        //y=kx+b
        // TODO: 15/11/2016 处理异常
        double k = (end.y-start.y)/(end.x-start.x);
        double b = start.y-k*start.x;

        double angle = Math.atan2((end.y-start.y),(end.x-start.x));
        double deltaX = Math.cos(angle)*dist;

        result.x = start.x+deltaX;
        result.y = result.x*k+b;

//        double distance=Vector3.distanceTo(start,end);
//        result.x = start.x + (end.x - start.x) * dist / distance;
//        result.y = start.y + (end.y - start.y) * dist / distance;

        return result;
    }

    public static Vector3 crossLlongerPoint(Vector3 result ,Vector3 start ,Vector3 end,float dist){
        longerPoint(result,start,end,dist);
        PointD p = new PointD(result.x,result.y);
        rotateAround(start.x,start.y,p.x,p.y,p,-Math.toRadians(90));
        result.setAll(p.x,p.y,0);
        return result;
    }

    public static void rotateAround(double refX, double refY, double x, double y, Vector3 result, double degrees) {
        double rX = x - refX;
        double rY = y - refY;
        double c = Math.cos(degrees);
        double s = Math.sin(degrees);
        result.x = (rX * c - rY * s) + refX;
        result.y = (rX * s + rY * c) + refY;
    }

    /**
     * @param refX
     * @param refY
     * @param x
     * @param y
     * @param result
     * @param degrees 逆时针旋转一个角度
     */
    public static void rotateAround(double refX, double refY, double x, double y, PointD result, double degrees) {
        double rX = x - refX;
        double rY = y - refY;
        double c = Math.cos(degrees);
        double s = Math.sin(degrees);
        result.x = (rX * c - rY * s) + refX;
        result.y = (rX * s + rY * c) + refY;
    }

    public static void rotatePath(List<Vector3> path, List<Vector3> rotatePath, double refX, double refY, double degrees) {
        if (path == null || rotatePath == null) {
            return;
        }
        rotatePath.clear();
        PointD pointD = new PointD();
        for (Vector3 p : path) {
            rotateAround(refX, refY, p.x, p.y, pointD, degrees);
            rotatePath.add(new Vector3(pointD.x, pointD.y, 0));
        }

    }


    public static void expandPath(double x1, double y1, double x2, double y2, PointD left1, PointD left2, PointD right1, PointD right2, double radius) {
        double degree = Math.PI / 2;
        double distance = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        left1.x = x1 + (x2 - x1) * radius / distance;
        left1.y = y1 + (y2 - y1) * radius / distance;
        right1.set(left1);

        left2.x = x2 + (x1 - x2) * radius / distance;
        left2.y = y2 + (y1 - y2) * radius / distance;
        right2.set(left2);

        rotateAround(x1, y1, left1.x, left1.y, left1, degree);
        rotateAround(x1, y1, right1.x, right1.y, right1, -degree);

        rotateAround(x2, y2, left2.x, left2.y, left2, -degree);
        rotateAround(x2, y2, right2.x, right2.y, right2, +degree);

    }


    public static void points2path(List<Vector3> lineLeft, List<Vector3> lineRight, List<Vector3> prePoints, double pathWidth) {
        points2path(lineLeft, lineRight, prePoints, pathWidth, pathWidth);
    }

    /**
     * 根据一条线获取该先左右两侧点的集合
     */
    public static void points2path(List<Vector3> lineLeft, List<Vector3> lineRight, List<Vector3> prePoints, double leftPathWidth, double rightPathWidth) {
        if (prePoints.size() < 2) {
            return;
        }
        /**
         A(a,b) B(m,n) BC = L

         x1= m - (b-n) /√[(a-m)^2+(b-n)^2]
         x2= m + (b-n) /√[(a-m)^2+(b-n)^2]
         y1 = n + L(a-m) /√[(a-m)^2+(b-n)^2]
         y2 = n - L(a-m) /√[(a-m)^2+(b-n)^2]
         */
        //获取所有计算后的点的集合
        //获取一侧点的集合
        ArrayList<Vector3> leftPoints = new ArrayList<>();
        ArrayList<Vector3> rightPoints = new ArrayList<>();
        leftPoints.clear();
        for (int i = 0; i < prePoints.size(); i++) {
            Vector3 currentVector3 = prePoints.get(i);
            Vector3 secondVector3;
            double m = currentVector3.x;
            double n = currentVector3.y;
            if (i == prePoints.size() - 1) {
                secondVector3 = prePoints.get(i - 1);
            } else {
                secondVector3 = prePoints.get(i + 1);
            }
            double a;
            double b;
            a = secondVector3.x;
            b = secondVector3.y;

            double x;
            double y;

            //			x =  (m - (b-n) / Math.sqrt(Math.pow((a-m),2)+Math.pow((b-n),2)));
            //			y =  (n + 50*(a-m) /Math.sqrt(Math.pow((a-m),2)+Math.pow((b-n),2)));

            /**
             C1(x,y) c2(x3,y3) A(x2,y2) B(x1,y1) BC=a

             x=x1-a*sin{arctan[(y2-y1)/(x2-x1)]}
             y=y1+a*cos{arctan[(y2-y1)/(x2-x1)]}
             x3=x1+a*sin{arctan[(y2-y1)/(x2-x1)]}
             y3=y1- a*cos{arctan[(y2-y1)/(x2-x1)]}
             */

            //x1,y1为当前点，x2,y2为下一个点
            //m,n为B，a,b为A
            double x1 = m, y1 = n;
            double x2 = a, y2 = b;
            if (y2 == y1) {
                x = (m - (b - n) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                y = (n + (leftPathWidth) * (a - m) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
            } else if (x2 == x1) {
                x = x1 - (leftPathWidth);
                y = y1;
            } else if ((x2 < x1 && y2 > y1) || (x2 < x1 && y2 < y1)) {
                x = (x1 + (leftPathWidth) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                y = (y1 - (leftPathWidth) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
            } else {
                x = (x1 - (leftPathWidth) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                y = (y1 + (leftPathWidth) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
            }

            Vector3 vector3 = new Vector3(x, y, 0);
            //如果不是第一个或者最后一个，那么需要取该点和该点的前一个点继续运算得到x,y，然后取中间值
            if (i != 0 && i != prePoints.size() - 1) {
                secondVector3 = prePoints.get(i - 1);
                a = secondVector3.x;
                b = secondVector3.y;
                x1 = m;
                y1 = n;
                x2 = a;
                y2 = b;
                if (y2 == y1) {
                    x = (m + (b - n) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                    y = (n - (leftPathWidth) * (a - m) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                } else if (x2 == x1) {
                    x = x1 - (leftPathWidth);
                    y = y1;
                } else if ((x2 < x1 && y2 > y1) || (x2 < x1 && y2 < y1)) {
                    x = (x1 - (leftPathWidth) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                    y = (y1 + (leftPathWidth) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
                } else {
                    x = (x1 + (leftPathWidth) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                    y = (y1 - (leftPathWidth) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
                }
                vector3.x = (vector3.x + x) / 2;
                vector3.y = (vector3.y + y) / 2;
            }
            double dist = calculateDistance(vector3.x, vector3.y, currentVector3.x, currentVector3.y);
            if (dist < leftPathWidth) {
                vector3.x = currentVector3.x + leftPathWidth / dist * (vector3.x - currentVector3.x);
                vector3.y = currentVector3.y + leftPathWidth / dist * (vector3.y - currentVector3.y);
            }
            leftPoints.add(vector3);

        }

        //获取另一侧点的集合
        rightPoints.clear();
        for (int i = 0; i < prePoints.size(); i++) {
            Vector3 currentVector3 = prePoints.get(i);
            Vector3 secondVector3;
            double m = currentVector3.x;
            double n = currentVector3.y;
            if (i == prePoints.size() - 1) {
                secondVector3 = prePoints.get(i - 1);
            } else {
                secondVector3 = prePoints.get(i + 1);
            }
            double a;
            double b;
            a = secondVector3.x;
            b = secondVector3.y;

            double x;
            double y;

            //			x =  (m + (b-n) / Math.sqrt(Math.pow((a-m),2)+Math.pow((b-n),2)));
            //			y =  (n - 50*(a-m) /Math.sqrt(Math.pow((a-m),2)+Math.pow((b-n),2)));

            double x1 = m, y1 = n;
            double x2 = a, y2 = b;
            if (y2 == y1) {
                x = (m + (b - n) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                y = (n - (rightPathWidth) * (a - m) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
            } else if (x2 == x1) {
                x = x1 + (rightPathWidth);
                y = y1;
            } else if ((x2 < x1 && y2 > y1) || (x2 < x1 && y2 < y1)) {
                x = (x1 - (rightPathWidth) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                y = (y1 + (rightPathWidth) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
            } else {
                x = (x1 + (rightPathWidth) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                y = (y1 - (rightPathWidth) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
            }

            Vector3 vector3 = new Vector3(x, y, 0);

            //如果不是第一个或者最后一个，那么需要取该点和该点的前一个点继续运算得到x,y，然后取中间值
            if (i != 0 && i != prePoints.size() - 1) {
                secondVector3 = prePoints.get(i - 1);
                a = secondVector3.x;
                b = secondVector3.y;
                x1 = m;
                y1 = n;
                x2 = a;
                y2 = b;
                if (y2 == y1) {
                    x = (m - (b - n) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                    y = (n + (rightPathWidth) * (a - m) / Math.sqrt(Math.pow((a - m), 2) + Math.pow((b - n), 2)));
                } else if (x2 == x1) {
                    x = x1 + (rightPathWidth);
                    y = y1;
                } else if ((x2 < x1 && y2 > y1) || (x2 < x1 && y2 < y1)) {
                    x = (x1 + (rightPathWidth) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                    y = (y1 - (rightPathWidth) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
                } else {
                    x = (x1 - (rightPathWidth) * Math.sin(Math.atan((y2 - y1) / (x2 - x1))));
                    y = (y1 + (rightPathWidth) * Math.cos(Math.atan((y2 - y1) / (x2 - x1))));
                }
                vector3.x = (vector3.x + x) / 2;
                vector3.y = (vector3.y + y) / 2;
            }
            double dist = calculateDistance(vector3.x, vector3.y, currentVector3.x, currentVector3.y);
            if (dist < rightPathWidth) {
                vector3.x = currentVector3.x + rightPathWidth / dist * (vector3.x - currentVector3.x);
                vector3.y = currentVector3.y + rightPathWidth / dist * (vector3.y - currentVector3.y);
            }
            rightPoints.add(vector3);
        }

        if (leftPoints.size() <= 0 || rightPoints.size() <= 0) {
            return;
        }

        //TODO 由于最后一个点的坐标是反向计算出来的，因此它的left和right是反的，在此做交换处理
        Vector3 temp = leftPoints.remove(leftPoints.size() - 1);
        leftPoints.add(rightPoints.remove(rightPoints.size() - 1));
        rightPoints.add(temp);

        lineLeft.clear();
        lineLeft.addAll(leftPoints);

        lineRight.clear();
        lineRight.addAll(rightPoints);

        /*//将点集合转成成矩形Path
        mRectPath.reset();
        Vector3 Vector3 = mPointsPath.get(0);
        mRectPath.moveTo(point.x,point.y);
        for(int i=1;i<mPointsPath.size();i++){
            if(i<mLeftPoints.size()){
                Vector3 = mLeftPoints.get(i);
            }else{
                Vector3 = mRightPoints.get(mRightPoints.size()-(i-mLeftPoints.size()+1));
            }
            mRectPath.lineTo(point.x, point.y);
        }


        //将点集合转换成Path集合，Path集合个数为原始点的个数减一(此处可表示为left或者right集合长度减一)
        mPaths.clear();
        for(int i=0;i<mLeftPoints.size()-1;i++){
            Path path = new Path();
            Vector3 leftCurrentVector3 = mLeftPoints.get(i);
            Vector3 leftNextVector3 = mLeftPoints.get(i+1);
            Vector3 rightCurrentVector3 = mRightPoints.get(i);
            Vector3 rightNextVector3 = mRightPoints.get(i+1);
            path.moveTo(leftCurrentPoint.x,leftCurrentPoint.y);
            path.lineTo(leftNextPoint.x,leftNextPoint.y);
            path.lineTo(rightNextPoint.x,rightNextPoint.y);
            path.lineTo(rightCurrentPoint.x,rightCurrentPoint.y);
            path.close();
            mPaths.add(path);
        }*/
    }

    /**
     * 以某个参照点旋转一个点
     *
     * @param angle 以弧度计算
     */
    public static PointF pointRotate(PointF src, PointF ref, double angle) {
        PointF result = null;
        PointF rPoint = new PointF(src.x - ref.x, src.y - ref.y);
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        float x = (float) (rPoint.x * c - rPoint.y * s + ref.x);
        float y = (float) (rPoint.x * s + rPoint.y * c + ref.y);
        result = new PointF(x, y);
        return result;
    }

    /**
     * 得到两点之间的角度 以逆时针方向，0-360
     */
    public static double pointDegree(PointF p1, PointF p2) {
        double angle = 0;
        float diffX, diffY;
        diffX = p2.x - p1.x;
        diffY = p2.y - p1.y;
        if (diffX == 0) {
            if (diffY > 0) {
                return 90;
            } else {
                return -90;
            }
        } else if (diffY == 0) {
            if (diffX >= 0) {
                return 0;
            } else {
                return 180;
            }
        } else {
            double k = (1.0f * diffY) / diffX;   //斜率
            double rad = Math.atan(k); //注意这个角度的范围是 [-pi/2..pi/2], 不是0到90°
            angle = (rad * 180) / Math.PI;
            if (diffY > 0 && diffX < 0) {
                angle += 180;
            } else if (diffY < 0 && diffX < 0) {
                angle += 180;
            } else if (diffY < 0 && diffX > 0) {
                angle += 360;
            }
        }
        return angle;
    }
    public static int translateLine(double x1, double y1, double x2, double y2, Vector3 lineStart, Vector3 lineEnd, double dist ){
        double distance = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        if(distance==0){
            return -1;
        }
        double degree = -Math.PI / 2;
        double radius = Math.abs(dist);
        if(dist>0){
            degree = Math.PI / 2;
        }

        lineStart.x = x1 + (x2 - x1) * radius / distance;
        lineStart.y = y1 + (y2 - y1) * radius / distance;

        lineEnd.x = x2 + (x1 - x2) * radius / distance;
        lineEnd.y = y2 + (y1 - y2) * radius / distance;

        rotateAround(x1, y1, lineStart.x, lineStart.y, lineStart, -degree);
        rotateAround(x2, y2, lineEnd.x, lineEnd.y, lineEnd, +degree);

        return 0;
    }
    /**
     *
     * @param path
     * @param result
     * @param dist 正数为右边扩展、负数左边扩展
     * @return 0：正常返回 -1：异常
     */
    public static int translatePath(List<Vector3> path,List<Vector3> result,double dist){
        if (path == null || path.size()<=1 || result==null) {
            return -1;
        }
        int cnt = path.size();
        Vector3 r0 = new Vector3();
        Vector3 r1 = new Vector3();
        Vector3 r2 = new Vector3();
        Vector3 r3 = new Vector3();
        Vector3 v = new Vector3();
        int inter = 0;
        for (int i = 0; i < cnt; i++) {
            if (i== (cnt-1)){
                Vector3 p0 = path.get(i-1);
                Vector3 p1 = path.get(i);
                translateLine(p0.x,p0.y,p1.x,p1.y,r0,r1,dist);
                v = r1;
                inter = 1;
            } else {
                if(i==0){
                    Vector3 p0 = path.get(i);
                    Vector3 p1 = path.get(i+1);
                    translateLine(p0.x,p0.y,p1.x,p1.y,r0,r1,dist);
                    v = r0;
                    inter = 1;
                }else {
                    Vector3 p0 = path.get(i-1);
                    Vector3 p1 = path.get(i);
                    Vector3 p2 = path.get(i+1);
                    translateLine(p0.x,p0.y,p1.x,p1.y,r0,r1,dist);
                    translateLine(p1.x,p1.y,p2.x,p2.y,r2,r3,dist);
                    inter = getIntersection(r0,r1,r2,r3,v);
                }
            }
            if (inter != 0) {
                result.add(new Vector3(v));
            }else {
                HaloLogger.logE(ARWayConst.ERROR_LOG_TAG,"translateLine no intersection ");
            }
        }
        return 0;
    }
    /**
     * 使用了double型不会造成误差
     * @param line1Start
     * @param line1End
     * @param line2Start
     * @param line2End
     * @param intersectionPoint
     * @return 0:无交点 1:相交,交点在线上 -1相交,交点不再线上
     */
    public static int getIntersection(Vector3 line1Start, Vector3 line1End, Vector3 line2Start, Vector3 line2End, Vector3 intersectionPoint) {
        Vector3 intersection = new Vector3(0, 0, 0);

        if (Math.abs(line1End.y - line1Start.y) + Math.abs(line1End.x - line1Start.x) + Math.abs(line2End.y - line2Start.y)
                + Math.abs(line2End.x - line2Start.x) == 0) {
            if ((line2Start.x - line1Start.x) + (line2Start.y - line1Start.y) == 0) {
                //                Log.e("helong_debug", "ABCD是同一个点！");
            } else {
                //                Log.e("helong_debug","AB是一个点，CD是一个点，且AC不同！");
            }
            return 0;
        }

        if (Math.abs(line1End.y - line1Start.y) + Math.abs(line1End.x - line1Start.x) == 0) {
            if ((line1Start.x - line2End.x) * (line2Start.y - line2End.y) - (line1Start.y - line2End.y) * (line2Start.x - line2End.x) == 0) {
                //                Log.e("helong_debug","A、B是一个点，且在CD线段上！");
            } else {
                //                Log.e("helong_debug","A、B是一个点，且不在CD线段上！");
            }
            return 0;
        }
        if (Math.abs(line2End.y - line2Start.y) + Math.abs(line2End.x - line2Start.x) == 0) {
            if ((line2End.x - line1End.x) * (line1Start.y - line1End.y) - (line2End.y - line1End.y) * (line1Start.x - line1End.x) == 0) {
                //                Log.e("helong_debug","C、D是一个点，且在AB线段上！");
            } else {
                //                Log.e("helong_debug","C、D是一个点，且不在AB线段上！");
            }
            return 0;
        }

        if ((line1End.y - line1Start.y) * (line2Start.x - line2End.x) - (line1End.x - line1Start.x) * (line2Start.y - line2End.y) == 0) {
            //            Log.e("helong_debug","线段平行，无交点！");
            return 0;
        }

        intersection.x = ((line1End.x - line1Start.x) * (line2Start.x - line2End.x) * (line2Start.y - line1Start.y) -
                line2Start.x * (line1End.x - line1Start.x) * (line2Start.y - line2End.y) + line1Start.x * (line1End.y - line1Start.y) * (line2Start.x - line2End.x)) /
                ((line1End.y - line1Start.y) * (line2Start.x - line2End.x) - (line1End.x - line1Start.x) * (line2Start.y - line2End.y));
        intersection.y = ((line1End.y - line1Start.y) * (line2Start.y - line2End.y) * (line2Start.x - line1Start.x) - line2Start.y
                * (line1End.y - line1Start.y) * (line2Start.x - line2End.x) + line1Start.y * (line1End.x - line1Start.x) * (line2Start.y - line2End.y))
                / ((line1End.x - line1Start.x) * (line2Start.y - line2End.y) - (line1End.y - line1Start.y) * (line2Start.x - line2End.x));

        double x = 0.000000000001;
        if ((intersection.x - line1Start.x) * (intersection.x - line1End.x) <= x
                && (intersection.x - line2Start.x) * (intersection.x - line2End.x) <= x
                && (intersection.y - line1Start.y) * (intersection.y - line1End.y) <= x
                && (intersection.y - line2Start.y) * (intersection.y - line2End.y) <= x) {

            //            Log.e("helong_debug","线段相交于点(" + intersection.x + "," + intersection.y + ")！");
            intersectionPoint.x = intersection.x;
            intersectionPoint.y = intersection.y;
            return 1; // '相交'
        } else {
            //            Log.e("helong_debug","线段相交于虚交点(" + intersection.x + "," + intersection.y + ")！");
            intersectionPoint.x = intersection.x;
            intersectionPoint.y = intersection.y;
            return -1; // '相交但不在线段上'
        }
    }

    /**
     * 旋转一个坐标按照某个角度
     *
     * @param basePoint  旋转的原点
     * @param coordinate 需要被旋转的坐标
     * @param degrees    旋转的角度
     */
    public static void rotateCoordinate(PointF basePoint, PointF coordinate, double degrees) {
        Vector3 base = new Vector3(basePoint.x, basePoint.y, 0);
        Vector3 coord = new Vector3(coordinate.x, coordinate.y, 0);
        rotateCoordinate(base, coord, degrees);
        basePoint.x = (float) base.x;
        basePoint.x = (float) base.y;
        coordinate.x = (float) coord.x;
        coordinate.x = (float) coord.y;
    }

    /**
     * 旋转一个坐标按照某个角度
     *
     * @param basePoint  旋转的原点
     * @param coordinate 需要被旋转的坐标
     * @param degrees    旋转的角度
     */
    public static void rotateCoordinate(Vector3 basePoint, Vector3 coordinate, double degrees) {
        if (mMatrix4Rotate == null) {
            mMatrix4Rotate = new Matrix();
        }
        mMatrix4Rotate.setRotate((float) degrees - 180, (float) basePoint.x, (float) basePoint.y);
        float[] xy = new float[2];
        mMatrix4Rotate.mapPoints(xy, new float[]{(float) coordinate.x, (float) coordinate.y});
        coordinate.x = xy[0];
        coordinate.y = xy[1];
    }

    /**
     * 求直线外一点到直线上的投影点
     *
     * @param pLine    线上一点
     * @param k        斜率
     * @param pOut     线外一点
     * @param pProject 投影点
     */
    public static void getProjectivePoint(PointF pLine, double k, PointF pOut, PointF pProject) {
        if (k == 0) {//垂线斜率不存在情况
            pProject.x = pLine.x;
            pProject.y = pOut.y;
        } else {
            pProject.x = (float) ((k * pLine.x + pOut.x / k + pOut.y - pLine.y) / (1 / k + k));
            pProject.y = (float) (-1 / k * (pProject.x - pOut.x) + pOut.y);
        }
    }

    /**
     * 求pOut在pLine以及pLine2所连直线上的投影点
     *
     * @param pLine
     * @param pLine2
     * @param pOut
     * @param pProject
     */
    public static void getProjectivePoint(PointF pLine, PointF pLine2, PointF pOut, PointF pProject) {
        double k = 0;
        try {
            k = getSlope(pLine.x, pLine.y, pLine2.x, pLine2.y);
        } catch (Exception e) {
            k = 0;
        }
        getProjectivePoint(pLine, k, pOut, pProject);
    }

    /**
     * 通过两个点坐标计算斜率
     * 已知A(x1,y1),B(x2,y2)
     * 1、若x1=x2,则斜率不存在；
     * 2、若x1≠x2,则斜率k=[y2－y1]/[x2－x1]
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @throws Exception 如果x1==x2,则抛出该异常
     */
    public static double getSlope(double x1, double y1, double x2, double y2) throws Exception {
        if (x1 == x2) {
            throw new Exception("Slope is not existence,and div by zero!");
        }
        return (y2 - y1) / (x2 - x1);
    }

    /***
     * 按照一定比例缩放整条Path,缩放的基点是Path的第一个点
     *
     * @param path
     * @param zoom
     */
    public static void zoomPath(List<Vector3> path, double zoom) {
        List<Vector3> offsets = new ArrayList<>();
        for (int i = 1; i < path.size(); i++) {
            Vector3 v0 = path.get(i - 1);
            Vector3 v1 = path.get(i);
            Vector3 offset = new Vector3(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z);
            offsets.add(offset);
        }
        for (int i = 1; i < path.size(); i++) {
            Vector3 v0 = path.get(i - 1);
            Vector3 v1 = path.get(i);
            Vector3 offset = offsets.get(i - 1);
            v1.x = v0.x + offset.x * zoom;
            v1.y = v0.y + offset.y * zoom;
            v1.z = v0.z + offset.z * zoom;
        }
    }

    /**
     * @param path
     * @param moveTo
     */
    public static void movePath(List<Vector3> path, Vector3 moveTo) {
        Vector3 v0 = path.get(0);
        Vector3 offset = new Vector3(moveTo.x - v0.x, moveTo.y - v0.y, moveTo.z - v0.z);
        movePath(path, offset.x, offset.y, offset.z);
    }

    /**
     * @param path
     * @param moveByX
     * @param moveByY
     * @param moveByZ
     */
    public static void movePath(List<Vector3> path, double moveByX, double moveByY, double moveByZ) {
        for (int j = 0; j < path.size(); j++) {
            Vector3 v = path.get(j);
            v.x += moveByX;
            v.y += moveByY;
            v.z += moveByZ;
        }
    }

    /**
     * 将一个从高德location得到的bearing(与正北的夹角)转换成
     *
     * @param amapDegrees
     * @return
     */
    public static double convertAMapBearing2OpenglBearing(double amapDegrees) {
        /*if (amapDegrees >= 0 && amapDegrees < 180) {
            return 180 - amapDegrees;
        } else {
            return (360 - amapDegrees + 180) - 360;
        }*/
        return amapDegrees/* > 180 ? amapDegrees - 360 : amapDegrees*/;
    }

    /**
     * rajawali roz 转换成 setRotation 方法
     * @param rotz
     * @return
     */
    public static double rotz2Radians(double rotz){
       return Math.PI/2-rotz;
    }

    public static double radians2Rotz(double radians){
        return Math.PI/2-radians;
    }

}
