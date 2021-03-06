package com.haloai.hud.hudendpoint.arwaylib.render.shader;

import android.opengl.GLES20;

import com.haloai.hud.utils.HaloLogger;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.plugins.IMaterialPlugin;
import org.rajawali3d.materials.shaders.AShader;
import org.rajawali3d.materials.shaders.IShaderFragment;
import org.rajawali3d.math.vector.Vector3;

/**
 * Created by wangshengxing on 16/10/15.
 */
public class RoadFogMaterialPlugin implements IMaterialPlugin {
    private RoadFogVertexShaderFragment mVertexShader;
    private RoadFogFragmentShaderFragment mFragmentShader;

    public RoadFogMaterialPlugin() {
        mVertexShader = new RoadFogVertexShaderFragment();
        mFragmentShader = new RoadFogFragmentShaderFragment();
    }

    @Override
    public Material.PluginInsertLocation getInsertLocation() {
        return Material.PluginInsertLocation.PRE_LIGHTING;
    }

    @Override
    public IShaderFragment getVertexShaderFragment() {
        return mVertexShader;
    }

    @Override
    public IShaderFragment getFragmentShaderFragment() {
        return mFragmentShader;
    }

    @Override
    public void bindTextures(int nextIndex) {
        /*mVertexShader.applyParams();
        mFragmentShader.applyParams();*/
    }
    @Override
    public void unbindTextures() {}

    public void setFogEndPosition(Vector3 position) {
        mFragmentShader.setFogEndPosition(position);
    }

    public void setFogStartPosition(Vector3 position) {
        mFragmentShader.setFogStartPosition(position);
    }
    public void setIsFog(boolean isFog){
        mFragmentShader.setIsFog(isFog);
    }

    private class RoadFogVertexShaderFragment extends AShader implements IShaderFragment {
        public final static String SHADER_ID = "ROAD_FOG_VERTEX";

        private final String V_FOG_POSITION = "vFogPosition";
        private final String V_FOG = "vFog";

        private RVec3 mvFogPosition;
        private RFloat mvFog;

        private RVec3 mgPosition;

        public RoadFogVertexShaderFragment() {
            super(ShaderType.VERTEX_SHADER_FRAGMENT);
            initialize();

        }

        @Override
        public void initialize()
        {
            super.initialize();
            mvFogPosition = (RVec3) addVarying(V_FOG_POSITION, DataType.VEC3);
            mvFog = (RFloat) addVarying(V_FOG, DataType.FLOAT);

        }
        @Override
        public void main() {
            mvFog.assign(new RVec3("aNormal").x());
            mgPosition = (RVec4) addGlobal(DefaultShaderVar.G_POSITION);
            mvFogPosition.assign(mgPosition.xyz());
        }

        @Override
        public void setLocations(int programHandle) {

        }

        @Override
        public void applyParams() {
            super.applyParams();

        }

        @Override
        public String getShaderId() {
            return SHADER_ID;
        }

        @Override
        public Material.PluginInsertLocation getInsertLocation() {
            return Material.PluginInsertLocation.IGNORE;
        }

        @Override
        public void bindTextures(int nextIndex) {}

        @Override
        public void unbindTextures() {}
    }
    private class RoadFogFragmentShaderFragment extends AShader implements IShaderFragment {
        public final static String SHADER_ID  = "ROAD_FOG_VERTEX";

        private final String U_START_POS = "uStartPosition";
        private final String U_END_POS   = "uEndPosition";
        private final String U_BACKGROUND   = "uBackground";
        private final String U_IS_FOG   = "uIsFog";

        private final String V_FOG_POSITION = "vFogPosition";

        private RVec3  muStartPosition;
        private RVec3  muEndPosition;
//        private RBool  muIsFog;

        private RVec3 mvFogPosition;


        private int muStartPositionHandle, muEndPositionHandle,muBackGroundHandle,muIsFogHandle;

        private float[] mStartPosition;
        private float[] mEndPosition;
//        private float[] mBackGround;
//        private int mIsFog;

        public RoadFogFragmentShaderFragment()
        {
            super(ShaderType.FRAGMENT_SHADER_FRAGMENT);
            initialize();
            mStartPosition = new float[]{0,0,0,0};
            mEndPosition = new float[]{0,0.01f,0,0};
//            mBackGround = new float[]{0x44/0xff,0x44/0xff,0x44/0xff};
//            mIsFog = GLES20.GL_FALSE;
        }

        @Override
        public String getShaderId() {
            return SHADER_ID;
        }

        @Override
        public void initialize()
        {
            super.initialize();
            muStartPosition = (RVec3) addUniform(U_START_POS, DataType.VEC3);
            muEndPosition = (RVec3) addUniform(U_END_POS, DataType.VEC3);
//            muIsFog = (RBool) addUniform(U_IS_FOG, DataType.BOOL);

            mvFogPosition = (RVec3) addVarying(V_FOG_POSITION, DataType.VEC3);
        }

        @Override
        public void main() {
            RVec4 gColor = (RVec4) getGlobal(DefaultShaderVar.G_COLOR);

            gColor.a().assign(new RVec4("texColor").a());
            gColor.rgb().assign(new RVec4("vColor").rgb());
            gColor.rgb().multiply(new RFloat("uColorInfluence"));

//            RFloat fogFactor = new RFloat("fogFactor");
            RFloat roadFog = new RFloat("roadFog");
//            fogFactor.assign(1);
            roadFog.assign(1);
            /*startif(new Condition(muIsFog,Operator.EQUALS,true));{

            }
            endif();*/
            roadFog.assign(distance(mvFogPosition.xyz(),muEndPosition).divide(distance(muEndPosition,muStartPosition)));
            roadFog.assign(clamp(roadFog,0,1));
            // TODO: 2016/11/2
            roadFog.assign(roadFog.multiply(roadFog).multiply(roadFog));
//            fogFactor.assign(roadFog);

            gColor.rgb().assignMultiply(roadFog);//rgb().
        }

        @Override
        public void setLocations(int programHandle) {
            super.setLocations(programHandle);
            muEndPositionHandle = getUniformLocation(programHandle, U_END_POS);
            muStartPositionHandle = getUniformLocation(programHandle, U_START_POS);
//            muBackGroundHandle = getUniformLocation(programHandle, U_BACKGROUND);
//            muIsFogHandle = getUniformLocation(programHandle, U_IS_FOG);
        }

        @Override
        public void applyParams() {
            super.applyParams();
//            HaloLogger.logE("fogPlugin","applyParams start");
            GLES20.glUniform3fv(muStartPositionHandle, 1, mStartPosition, 0);
            GLES20.glUniform3fv(muEndPositionHandle, 1, mEndPosition, 0);
//            HaloLogger.logE("fogPlugin","applyParams end");
//            GLES20.glUniform3fv(muBackGroundHandle, 1, mBackGround, 0);
//            GLES20.glUniform1i(muIsFogHandle,mIsFog);
        }

        public void setFogStartPosition(Vector3 pos)
        {
            float[] p = mStartPosition;
            p[0] = (float)pos.x;
            p[1] = (float)pos.y;
            p[2] = (float)pos.z;
        }

        public void setFogEndPosition(Vector3 pos)
        {
            float[] p = mEndPosition;
            p[0] = (float)pos.x;
            p[1] = (float)pos.y;
            p[2] = (float)pos.z;
        }

        public void setIsFog(boolean isFog){
//            mIsFog = isFog?GLES20.GL_TRUE:GLES20.GL_FALSE;
        }

        @Override
        public Material.PluginInsertLocation getInsertLocation() {
            return Material.PluginInsertLocation.IGNORE;
        }

        @Override
        public void bindTextures(int nextIndex) {}

        @Override
        public void unbindTextures() {}
    }
}
