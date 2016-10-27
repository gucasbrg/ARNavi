package com.haloai.hud.hudendpoint.arwaylib.render.object3d;

import com.haloai.hud.hudendpoint.arwaylib.render.vertices.GeometryData;
import com.haloai.hud.hudendpoint.arwaylib.utils.ARWayConst;
import com.haloai.hud.utils.HaloLogger;

public class TileFloor extends BaseObject3D {
    private float mSize;
    private int mNumLines;

    public TileFloor(float width,float height,float spacing) {
        super();
        initGridFloor(width, height, spacing);
    }

    private void initGridFloor(float width,float height,float spacing) {
        GeometryData geometryData = getGeometryData2(width, height, spacing);
        addVerties(geometryData);
        applyVerties();
    }
    public static GeometryData getGeometryData2(float width,float height,float spacing){
        int widthNum = (int) (width/spacing)+1;
        int heightNum = (int) (height/spacing)+1;
        print(String.format("widthNum = %s,heightNum=%s \n",widthNum,heightNum));
        if(widthNum <= 0 || heightNum <= 0){
            return null;
        }
        int vertexsCnt = (widthNum)*(heightNum)*4;

        float[] vertices = new float[(vertexsCnt)*3];
        float[] coords = new float[(vertexsCnt)*2];
        int[] indices = new int[(widthNum*heightNum)*2*3];
        int index = 0;
        float x = -width/2;
        float y = height/2+spacing;
        float z = 0;

        for (int i = 0; i < heightNum; i++) {
            for (int j = 0; j < widthNum; j++) {

                vertices[index++] = x+spacing;
                vertices[index++] = y;
                vertices[index++] = z;

                vertices[index++] = x;
                vertices[index++] = y;
                vertices[index++] = z;

                vertices[index++] = x;
                vertices[index++] = y-spacing;
                vertices[index++] = z;

                vertices[index++] = x+spacing;
                vertices[index++] = y-spacing;
                vertices[index++] = z;

                /*print(String.format("vertices x = %s,y=%s \n",x,y));
                print(String.format("vertices x = %s,y=%s \n",x+spacing,y));
                print(String.format("vertices x = %s,y=%s \n",x+spacing,y-spacing));*/

                x += spacing;
            }
            x = -width/2;
            y -= spacing;
        }
        print(String.format("vertices index = %s,length=%s \n",index,vertices.length));

        index = 0;
        for (int i = 0; i < heightNum; i++) {
            for (int j = 0; j < widthNum; j++) {
                coords[index++] = 1;coords[index++] = 1;
                coords[index++] = 0;coords[index++] = 1;
                coords[index++] = 0;coords[index++] = 0;
                coords[index++] = 1;coords[index++] = 0;
            }
        }
        print(String.format("coords index = %s,length=%s\n",index,coords.length));
        index = 0;
        for (int i = 0; i < vertexsCnt; i += 4) {
            indices[index++] = i+0;
            indices[index++] = i+1;
            indices[index++] = i+2;

            indices[index++] = i+2;
            indices[index++] = i+3;
            indices[index++] = i+0;
        }
        print(String.format("indices index = %s,length=%s\n",index,indices.length));

        GeometryData element = new GeometryData();
        element.setUseTextureCoords(true);
        element.setUseColors(false);
        element.setUseNormals(false);
        element.vertices = vertices;
        element.textureCoords = coords;
        element.indices = indices;

        return element;
    }

    public static GeometryData getGeometryData(float width,float height,float spacing){
        int widthNum = (int) (width/spacing)+1;
        int heightNum = (int) (height/spacing)+1;
        print(String.format("widthNum = %s,heightNum=%s \n",widthNum,heightNum));
        if(widthNum <= 0 || heightNum <= 0){
            return null;
        }
        int vertexsCnt = (widthNum)*(heightNum)*3;

        float[] vertices = new float[(vertexsCnt)*3];
        float[] coords = new float[(vertexsCnt)*2];
        int[] indices = new int[(widthNum*heightNum)*3];
        int index = 0;
        float x = -width/2;
        float y = height/2+spacing;
        float z = 0;

        for (int i = 0; i < heightNum; i++) {
            for (int j = 0; j < widthNum; j++) {

                vertices[index++] = x+spacing;
                vertices[index++] = y;
                vertices[index++] = z;

                vertices[index++] = x;
                vertices[index++] = y;
                vertices[index++] = z;

                vertices[index++] = x;
                vertices[index++] = y-spacing;
                vertices[index++] = z;

                /*print(String.format("vertices x = %s,y=%s \n",x,y));
                print(String.format("vertices x = %s,y=%s \n",x+spacing,y));
                print(String.format("vertices x = %s,y=%s \n",x+spacing,y-spacing));*/

                x += spacing;
            }
            x = -width/2;
            y -= spacing;
        }
        print(String.format("vertices index = %s,length=%s \n",index,vertices.length));

        index = 0;
        for (int i = 0; i < heightNum; i++) {
            for (int j = 0; j < widthNum; j++) {
                coords[index++] = 1;coords[index++] = 1;
                coords[index++] = 0;coords[index++] = 1;
                coords[index++] = 0;coords[index++] = 0;
            }
        }
        print(String.format("coords index = %s,length=%s\n",index,coords.length));

        for (index = 0; index < indices.length; index++) {
            indices[index] = index;
        }
        print(String.format("indices index = %s,length=%s\n",index,indices.length));

        GeometryData element = new GeometryData();
        element.setUseTextureCoords(true);
        element.setUseColors(false);
        element.setUseNormals(false);
        element.vertices = vertices;
        element.textureCoords = coords;
        element.indices = indices;

        return element;
    }

    private static void print(String msg){
        HaloLogger.logE(ARWayConst.SPECIAL_LOG_TAG,msg);
//        System.out.print(ARWayConst.SPECIAL_LOG_TAG+msg);
    }
    private void createGridFloor() {
        /*final float sizeHalf = mSize * 0.5f;
        final float spacing = mSize / mNumLines;

        mPoints = new Stack<>();

        for(float y = -sizeHalf; y <= sizeHalf; y += spacing) {
            mPoints.add(new Vector3(-sizeHalf, y,0));
            mPoints.add(new Vector3(sizeHalf, y,0));
        }

        for(float x = -sizeHalf; x <= sizeHalf; x += spacing) {
            mPoints.add(new Vector3(x, -sizeHalf,0));
            mPoints.add(new Vector3(x, sizeHalf, 0));
        }
        init(true);
        setDrawingMode(GLES20.GL_LINES);*/
    }
}
