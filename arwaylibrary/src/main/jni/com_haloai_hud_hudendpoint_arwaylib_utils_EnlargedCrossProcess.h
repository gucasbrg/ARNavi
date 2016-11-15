/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess */

#ifndef _Included_com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess
#define _Included_com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess
#ifdef __cplusplus
extern "C" {
#endif
#undef com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess_CROSS_ROAD_LEN
#define com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess_CROSS_ROAD_LEN 2000L
/*
 * Class:     com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess
 * Method:    nativeGetBranchRoads
 * Signature: (JI[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess_nativeGetBranchRoads
  (JNIEnv *, jobject, jlong, jint, jobjectArray);

/*
 * Class:     com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess
 * Method:    nativeGetHopPointInCrossImage
 * Signature: (JLcom/haloai/hud/hudendpoint/arwaylib/utils/EnlargedCrossProcess/PointA;)I
 */
JNIEXPORT jint JNICALL Java_com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess_nativeGetHopPointInCrossImage
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess
 * Method:    nativeGetCrossLinks
 * Signature: (Ljava/util/List;Ljava/util/List;Lcom/haloai/hud/hudendpoint/arwaylib/utils/jni_data/LatLngOutSide;Lcom/haloai/hud/hudendpoint/arwaylib/utils/jni_data/Size2iOutside;Ljava/lang/String;ILjava/util/List;Ljava/util/List;Ljava/util/List;)I
 */
JNIEXPORT jint JNICALL Java_com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess_nativeGetCrossLinks
  (JNIEnv *, jobject, jobject, jobject, jobject, jobject, jstring, jint, jobject, jobject, jobject);

/*
 * Class:     com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess
 * Method:    nativeClearRoadNetStatus
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_haloai_hud_hudendpoint_arwaylib_utils_EnlargedCrossProcess_nativeClearRoadNetStatus
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
