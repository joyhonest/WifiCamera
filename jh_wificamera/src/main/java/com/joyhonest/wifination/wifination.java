package com.joyhonest.wifination;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.AudioRecord;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaRecorder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;

import org.simple.eventbus.EventBus;

import java.io.File;
import java.nio.ByteBuffer;




/**
 * Created by aivenlau on 16/7/13.
 */




public class wifination {


    public  final   static    int  GP4225_Type_Video = 1;
    public  final   static    int  GP4225_Type_Locked = 2;
    public  final   static    int  GP4225_Type_Photo = 3;

    public final static int IC_NO = -1;
    public final static int IC_GK = 0;
    public final static int IC_GP = 1;
    public final static int IC_SN = 2;
    public final static int IC_GKA = 3;
    public final static int IC_GPRTSP = 4;
    public final static int IC_GPH264 = 5;
    public final static int IC_GPRTP = 6;
    public final static int IC_GPH264A = 7;
    public final static int IC_GPRTPB = 8;
    public final static int IC_GK_UDP = 9;



    public static AudioEncoder AudioEncoder;

    public final static int TYPE_ONLY_PHONE = 0;
    public final static int TYPE_ONLY_SD = 1;
    public final static int TYPE_BOTH_PHONE_SD = 3;
    public final static int TYPE_PHOTOS = 0;
    public final static int TYPE_VIDEOS = 1;
    public static ByteBuffer mDirectBuffer;
    //public static ByteBuffer mDirectBufferYUV;
    public static boolean bDisping = false;


    private  static VideoMediaCoder  videoMediaCoder;

    //private static  Context  mContext=null;

    private final static String TAG = "wifination";
    private static final wifination m_Instance = new wifination();
    private static final int BMP_Len = (((2560 + 3) / 4) * 4) * 4 * 1920 + 2048;
    //private static final int BMP_Len = (((1920 + 3) / 4) * 4) * 4 * 1080 +1024;
    private  final  static int CmdLen = 2048;


    public static GP4225_Device gp4225_Device;


    public static Context appContext = null;


    private  static Bitmap Gesture_bmp =null; // = Bitmap.createBitmap(300,300, Bitmap.Config.ARGB_8888);

    static {
        try {
            System.loadLibrary("jh_wifi");
            AudioEncoder = new AudioEncoder();
            videoMediaCoder = new VideoMediaCoder();

            gp4225_Device = new GP4225_Device();

            mDirectBuffer = ByteBuffer.allocateDirect(BMP_Len + CmdLen);     //????????????????????????????????????????????????????????????????????????

            naSetDirectBuffer(mDirectBuffer, BMP_Len + CmdLen);
            //G_StartAudio(1);


        } catch (UnsatisfiedLinkError Ule) {
            Log.e(TAG, "Cannot load jh_wifi.so ...");
            Ule.printStackTrace();
        } finally {


        }
    }

//    public static void   naSetMaxResolution(int nWidth,int nHeight)
//    {
//        int len = (((nWidth + 3) / 4) * 4) * 4 * nHeight + 2048;
//        mDirectBuffer = ByteBuffer.allocateDirect(len + CmdLen);     //????????????????????????????????????????????????????????????????????????
//        naSetDirectBuffer(mDirectBuffer, len + CmdLen);
//    }

    private wifination() {

    }

    //??????????????????
    public static wifination getInstance() {
        return m_Instance;
    }
    private static native void naSetDirectBuffer(Object buffer, int nLen);
    private static native void naSetDirectBufferYUV(Object buffer, int nLen);


    ////// ------------- -------------------------

    //????????????????????????wifi?????????????????????
    /*
        IC_GKA???  sPath=@???1???  720P   sPath=@???2??? VGA
        IC_GP???       sPath=@"http://192.168.25.1:8080/?action=stream"
        IC_GPRTSP     sPath = @"rtsp://192.168.26.1:8080/?action=stream"
        ???????????????      sPath=@??????;
    */


    public static native  void  naSetLedPWM(byte nPwm);
    public static native  void  naGetLedPWM();
    public static native  void  naGetBattery();
    public static native  void  naGetWifiSSID();
    public static native  void  naSetWifiSSID(String sSSid);
    public static native  void  naSetLedMode(int nMode);
    public static native  void  naGetLedMode();

    public static   void  naGetFirmwareVersion()
    {
        na4225_ReadFireWareVer();
    }



    //??????????????????
    public static native  void naWriteData2Flash(byte[]data,int nLen);
    //?????????????????????????????????????????????OnGetGP_Status ??????
    public static native  void naReadDataFromFlash();




    public static native int naInit(String pFileName);

    //????????????
    public static native int naStop();
    //?????????????????????OnGetGP_Status
    public static native int naSentCmd(byte[] cmd, int nLen);

    //??????????????????
    public static native void naSetFlip(boolean b);
    // ??????VR??????
    public static native void naSet3D(boolean b);


    public static native  void naStartRead20000_20001();

    public static native void naSetTimeOsd(int x,int y,int nDateType); //x??????????????????????????????



    public static native void naSetDarkcornerWidth(int nleftTopDx,int nleftTopDy,int nrightbottomDx,int nrightbottomDy);     //?????????????????????????????????????????????????????????????????????????????????????????????


    //TYPE_ONLY_PHONE   ==  ???????????????????????????
    //TYPE_ONLY_SD     ==  ??????????????????????????????SD?????????????????????GKA????????????)
    // TYPE_BOTH_PHONE_SD  ==  ????????????????????????????????????SD????????????
    //
    //??????
    public static native int naSnapPhoto(String pFileName, int PhoneOrSD);
    //??????
    private static native int naStartRecordA(String pFileName, int PhoneOrSD);

    private static  String sVideoName="";
    public static  boolean bG_Audio=false;


    public static  native void naSetScaleHighQuality(int nQ);

    public static  void naStartRecord(String pFileName, final  int PhoneOrSD)
    {
        sVideoName = pFileName;
        String tmpFileName = sVideoName+"_.tmp";
        if(PhoneOrSD == TYPE_BOTH_PHONE_SD || PhoneOrSD == TYPE_ONLY_PHONE)
        {
            if(isPhoneRecording()) {
                return;
            }

            if(tmpFileName.length()>10)
            {
                MyMediaMuxer.init(tmpFileName);
            }

            if(bG_Audio)
            {
                if(!AudioEncoder.isCanRecordAudio())  //??????????????????????????????????????????????????????????????????????????????
                {
                    bG_Audio = false;
                }
            }

            if(bG_Audio)
            {
                boolean re = G_StartAudio(1);
                if(!re) //?????????????????????
                {
                    bG_Audio=false;
                }
                else
                {
                    int nn = 0;
                    while(MyMediaMuxer.audioInx<0)
                    {
                        SystemClock.sleep(10);
                        nn++;
                        if(nn>50)
                            break;
                    }

                }
            }
            if(!bG_Audio)
            {
                G_StartAudio(0);
            }
        }

        naStartRecordA(tmpFileName,PhoneOrSD);
    }

    // ?????????????????? ms
    public static  int  naGetRecordTime()
    {
        return (int)videoMediaCoder.getRecordTime();
    }
    //????????????
    public static native void naStopRecord(int PhoneOrSD);
    //??????????????????
    public static native int naStopRecord_All();
    //?????????????????????????????????
    public static  void naSetRecordAudio(boolean b)
    {
        bG_Audio = b;

    }
    //?????????????????????

    public static native  boolean naIsJoyCamera();  //???????????????WIFI????????????????????????

    public static native boolean isPhoneRecording();
    //??????????????????????????????????????????????????????????????????????????????????????????
    public static native int naSetRecordWH(int ww, int hh);

    //2022-03-31
    public static native void naSetSDRecordResolution(int nResolution);
    public static  native void naGetSDRecordResolution();

    public static  native void  naSetMicOnOff(boolean bOn);
    public static  native void  naGetMicOnOff();

    public static native  int naGetDispWH();
    //??????????????????SDK??????????????????b = true??? SDK ??????????????????????????????JAVA??????APP??????????????????????????????SDK?????????????????????
    // SDK??????????????? ??? ReceiveBmp ??????
    public static void naSetRevBmp(boolean b)
    {
        bRevBmp = b;
        naSetRevBmpA(b);
    }

    public static void naSetGesture(boolean b,Context appContext)
    {
        bGesture = b;
        if(bGesture)
        {
            if(sig==null)
            {
                sig = ObjectDetector.getInstance();
                sig.SetAppCentext(appContext);
            }
        }
        if(sig!=null)
        {
            if(Gesture_bmp==null)
                Gesture_bmp = Bitmap.createBitmap(300,300, Bitmap.Config.ARGB_8888);
            sig.F_Start(bGesture);
        }
        naSetGestureA(b);
    }



    /*
            GP4225 ?????? ??????SD?????????+
    */

    //????????????
    public static native void na4225_SyncTime(byte[] data,int nLen);
    public static native void na4225_ReadTime();
    //??????????????????
    public static native void na4225_SetOsd(boolean  b);
    public static native void na4225_ReadOsd();
    //??????????????????
    public static native void na4225_SetReversal(boolean  b);
    public static native void na4225_ReadReversal();
    //???????????????????????? - 0  1min  1  - 3min  2 - 5min
    public static native void na4225_SetRecordTime(int n);
    public static native void na4225_ReadRecordTime();
    //?????????SD???
    public static native void na4225_FormatSD();
    //????????????????????????
    public static native void na4225_ReadFireWareVer();
    //??????????????????
    public static native void na4225_ResetDevice();







    /*
        APP??????????????????
    */
    public static native void na4225_ReadStatus();

    /*
            APP??????????????????
            0  ????????????
            1  ????????????
     */

    public static native void na4225_SetMode(byte nMode);

    /*
        APP ??????????????????
        ?????????na4225_SetMode???????????????????????????
        nType = 1;  ??????
        nType = 2;  ????????????
        nType = 3'  ??????
        nType = 4'  ????????????
 */

    public static native void na4225_GetFileList(int nType, int nStrtinx,int nEndinx);



    public static native void na4225_DeleteFile(String sPath,String sFileName);
    public static native void na4225_DeleteAll(int nType); //  2 videos 3 photos   4 all


    public static  int naPlayFlie(String sFileName)
    {
        return naPlayFlieB(sFileName, new PlayerInterface() {
            @Override
            public void Playduration(int i) {
                //Log.e("AABB","Len = "+i);
                Integer a = i;
                EventBus.getDefault().post(a,"Playduration");

            }

            @Override
            public void PlayStatus(int nStatyus) {
                //Log.e("AABB","Status = "+nStatyus);
                Integer a = nStatyus;
                EventBus.getDefault().post(a,"PlayStatus");
            }

            @Override
            public void PlayTime(int da) {
                //  Log.e("AABB","time = "+da);
                Integer a = da;
                EventBus.getDefault().post(a,"PlayTime");
            }
        });

    }

    private static native int naPlayFlieB(String sFileName,PlayerInterface backCalled);
    public static native void naStopPlay();
    public static native void naPause();
    public static native void naSeek(float nSec);



    /*
        ????????????
    */

    public static native boolean na4225StartDonwLoad(String sPath,String sFileName,int nLen,String sSaveName);
    public static native boolean  na4225StartPlay(String sPath,String sFileName,int nLen);
    public static native boolean naDisConnectedTCP();
    ///////// 4225 end --------------




    private static native void naSetRevBmpA(boolean b);


    //??????????????????????????? True????????????????????? ReceiveBmp ????????????????????? SDK?????????????????????????????? ??????APP ???????????????????????????????????????
    // ????????? naSetRevBmp ?????????
    private static native void naSetGestureA(boolean b);


    //RTL

    //onReadRtlData  ?????????????????????

    public static native  int naGetRtl_Mode();
    public static native  int naGetRtl_List(int bImage,int inx);
    public static native  int naDownLoadRtlFile(String sFileName);
    public static native  int naCancelRTL();


    //?????? ?????? ????????? GKA??? ???sima??? ?????? ??????????????? ??????????????????????????????
    public static native void naSetCustomer(String sCustomer);
    //??????SD????????? (??????  IC_GK_A ???????????? IC_GKA
    public static native int naGetPhotoDir();
    public static native int naGetVideoDir();
    public static native int naGetFiles(int nType);
    public static native int naDownloadFile(String sPath, String dPath);
    public static native int naCancelDownload();
    public static native int naDeleteSDFile(String fileName);





    //??????SD???????????????????????????(??????  IC_GKA),??????????????????????????????????????????????????????,????????????????????????????????????,???????????????????????????????????????????????????
//???SD??????????????????????????????,??????????????????,SDK????????? GetThumb(byte[] data,String sFilename), data ??????????????????,filename??????????????????????????????
//??????,???????????????naGetVideoDir()???, ???????????????GetFiles(byte[] filesname)???????????????,????????????????????????????????????
    public static native int naGetThumb(String filename);




    public static native int naCancelGetThumb();

    //???????????????????????????????????????,?????????????????????????????????????????????????????????????????????????????????????????????????????????
    private static native int naGetVideoThumbnailB(String filename,Bitmap bmp);


    public static native  void naSetDispStyle(int nType); //0-6

    ///?????????,????????????....

    public static  native void naSetDislplayData(byte[]data,int width,int height);

    public static native int naPlay();
    public static native int naStartCheckSDStatus(boolean bStart);

    public static native void naSet3DA(boolean b);
    public static native int naSetGPFps(int nFps);
    public static  int naGkASetRecordResolution(boolean b20P)
    {
        if(b20P) {
            return  naSetRecordWH(1280, 720);
        }
        else
        {
            return naSetRecordWH(-1, -1); //?????????????????????????????????
        }
    }

    public static native int naGetSessionId();
    public static native void naSetGKA_SentCmdByUDP(boolean bUdp);
    //GP_RTSP
    //??????????????????
    public static native int naGetGP_RTSP_Status();

    public static native void naSetdispRect(int w, int h);
    public static native int naRemoteSnapshot();
    public static native int naRemoteSaveVideo();

    //public static native void naSN_WriteFrame(byte[] data, int nLen);
    public static native int naGetSettings();
    public static native boolean naCheckDevice();
    public static native int naSaveSnapshot(String pFileName);
    public static native int naSaveVideo(String pFileName);
    public static native int naStopSaveVideo();
    public static native int naStatus();

    //??????
    public static native void naSetFollow(boolean bFollow);
    public static native void naSetContinue();

    //Sunbplus
    public static native int naSetMenuFilelanguage(int nLanguage);


    public static native void naFillFlyCmdByC(int nType);

    public static native int naSave2FrameMp4(byte[] data, int nLen, int tyoe, boolean bKeyframe);

    public static native int naGetFps();

    public static native int naGetwifiFps();

    public static native String naGetControlType(); //????????????????????????????????? SYMA  ??????t

    public static native boolean naSetBackground(byte[] data, int width, int height);

    public static native void naSetAdjFps(boolean b); //????????????IC????????????????????????????????????FPS????????????????????????????????????


    //??????
    public static  native void naSetRadarAdj(boolean b);


    //?????????????????????????????? 0 90 180 270
    public static native  void naSetCameraDataRota(int n);



    public static native void naSetVrBackground(boolean b);

    public static native void naRotation(int n);  // n == 0 || n ==90 || n ==-90 || n ==180 || n==270
    public static  native void naSetbRotaHV(boolean b); //b = flase  ????????????????????????????????????????????????camera???????????????????????????????????? naRotation ?????? 90???????????????
    //b = true,  ??????????????????????????????????????? naRotation??? ???????????? ?????????????????? ???????????? 90 ???-90 270 ?????????????????? ??????
    public static native boolean naSetWifiPassword(String sPassword);
    public static native void naSetLedOnOff(boolean bOpenLed);


    public static native void naSetScal(float fScal); //????????????????????????

    public static void naSetCmdResType(int nType)
    {
        JH_Tools.F_SetResType(nType);
    }


    public static native void naSetNoTimeOut(boolean b);    //



    public static native void naSetDebug(boolean b);//???????????? ????????????????????????

    public static native void naWriteport20000(byte[] cmd,int nleng);

    public static native  void naSetMirror(boolean b);


    public static  native void naSetSnapPhoto(int w,int h,boolean b);


    public static native void init();

    public static native void release();

    public static native void changeLayout(int width, int height);

    public static native void drawFrame();


    public static native  void naSetSaveTypeByName(boolean b); //b = true  ???????????????????????? ????????? ????????? ?????? ??????????????????  false   ????????? jpg????????????


    public  static  void  naAdjAngleData(int data)
    {
        gp4225_Device.nAdjAngle = data;
    }

    public static native void naSetGsensorPara(int n,float nAngle);


    public static native  void naRelinkPlay();



    public static  native  void naSetGsensorRotaFillWhite(boolean b);


    public static void naSetGsensorType(int n) {
        gp4225_Device.nGsensorType = n;
    }
    public static  void naSetAdjGsensorData(boolean b)
    {
        gp4225_Device.bAdjGsensorData = b;
    }

    public static native void naSetGsensor2SDK(int xx,int yy,int zz);  //????????????Rotate Filter ?????????

    public static void naSetProcessesGsensorDataByApp(boolean b)    //?????????APP?????????Gsensor ?????????????????? ?????????APP??????naSetAngle ??????????????????
    {
        gp4225_Device.bProcessesGsensorDataByApp = b;
    }
    public static  native  void naSetAngle(float  Angle);

    public static native void naSet3DDenoiser(boolean b); //????????????3D??????
    public static native void naSetEnableRotate(boolean b); //???????????????????????????????????????

    public static native int naSetRotate(int nRotate);  //????????????Rotate Filter ????????? //?????? 0 ??????  naSetEnableRotate???false)

    public static native void naSetSensor(boolean b);   //GSensor on or off
    public static native void naSetSensorA(boolean b);   //GSensor on or off ????????????????????????????????????????????????
    public static native void naSetCircul(boolean b);   //GSensor ?????????????????????????????????
    public static native void naSetsquare(boolean b);   //GSensor ?????????????????????????????????
    //bEnableCircul

    public static native void naSetAcdetection(boolean b);  //???????????? on or off
    public static  native  int HiSi_Convert(String sPath,String sOutPath);
    public static  int  F_Convert(String sPath,String sOutPath)
    {
        File file = new File(sOutPath);
        if(file.exists())
        {
            file.delete();
        }
        File file1 = new File(sPath);
        if(file1.exists())
            return HiSi_Convert(sPath,sOutPath);
        else
            return -100;
    }



    ////////////   ???????????????  2020-04-26 ?????? /////////
    public  static  native  void naSetUVCA_Brightness(int nBrightness);   //?????? -64~+64
    public  static  native  void naSetUVCA_Contrast(int nContrast);     //?????????  0-100
    public  static  native  void naSetUVCA_Saturation(int naturation);     //????????? 0-100
    public  static  native  void naSetUVCA_Zoom(int nZoom);     //?????? 0-255
    public  static  native  void naSetUVCA_Panorama(int nPanorama);     //?????? 0-255
    public  static  native  void naSetUVCA_Inclination(int nInclination);  //?????? 0-255
    public  static  native  void naSetUVCA_Roll(int nRoll);     //??????


    public  static  native  void naSentPhotoKey();


    //?????? ???
    public  static  native  void naGetUVCA_Brightness();   //?????? -64~+64
    public  static  native  void naGetUVCA_Contrast();     //?????????  0-100
    public  static  native  void naGetUVCA_Saturation();     //????????? 0-100
    public  static  native  void naGetUVCA_Zoom();     //?????? 0-255
    public  static  native  void naGetUVCA_Panorama();     //?????? 0-255
    public  static  native  void naGetUVCA_Inclination();  //?????? 0-255
    public  static  native  void naGetUVCA_Roll();     //??????
    //////////


/*
    public static  native  void CancelNoiseInit(int frame_size, int sample_rate);

    public static  native  int CancelNoisePreprocess(byte[] cmd, int nLen);

    public static  native  void CancelNoiseDestroy();
*/

    public  static boolean  bGesture = false;
    public  static boolean  bRevBmp = false;


    private static ObjectDetector sig=null;


    public  static native int naSetTransferSize(int nWidth,int nHeight);   //???????????????8?????????


    public static void naSetGesture_vol(float aa)
    {
        ObjectDetector.MINIMUM_CONFIDENCE_TF_OD_API = aa;
    }


    public static native boolean naSentUdpData(String sIP, int nPort,byte[] data, int nLen);
    public static native boolean naStartReadUdp(int nPort); // ??????????????????????????? onUdpRevData ??????
    public static native boolean naStopReadUdp();
    public static  void naSetProgressGP4225Data(boolean b)
    {
        bProgressGP4225UDP = b;
    }
    private  static boolean  bProgressGP4225UDP=true;


    private static void onGetBattery(int nBattery_)
    {
        Integer  nBattery = nBattery_;
        EventBus.getDefault().post(nBattery,"onGetBattery");
    }

    private static  void onGetLedPwm(int nLed_)    //GP 30 ????????????LED PWM?????????
    {
        Integer  nLed = nLed_;
        EventBus.getDefault().post(nLed,"onGetLed");
        EventBus.getDefault().post(nLed,"onGetLedPWM");
    }

    private static  void onUdpRevData(byte[] data,int nPort)      // naStartReadUdp??????????????????????????????????????????
    {
        UpdData  udp_data = new UpdData(data,nPort);
        if(nPort == 20001) {
            if (bProgressGP4225UDP) {
                if (!gp4225_Device.GP4225_PressData(data)) {
                    EventBus.getDefault().post(data, "onUdpRevData");
                    EventBus.getDefault().post(udp_data, "onUdpRevData_NewVer");
                }
            } else {
                EventBus.getDefault().post(data, "onUdpRevData");
                EventBus.getDefault().post(udp_data, "onUdpRevData_NewVer");
            }
        }
        else
        {
            EventBus.getDefault().post(data, "onUdpRevData");
            EventBus.getDefault().post(udp_data, "onUdpRevData_NewVer");
        }
    }

    public static Bitmap naGetVideoThumbnail(String filename)
    {
        Bitmap bitmap = Bitmap.createBitmap(100,100, Bitmap.Config.ARGB_8888);
        int re = naGetVideoThumbnailB(filename,bitmap);
        if(re == 0)
            return bitmap;
        else
            return null;

    }



////////////////

    private static void OnPlayIsStarting_Callback(int n)
    {
        //n !=0  Play is Start  0= Play is over
        Integer i = n;
        EventBus.getDefault().post(i,"OnPlayStatus");

    }





    private static boolean G_StartAudio(int b) {
        if (b != 0) {
            return  AudioEncoder.start();
        } else {
            AudioEncoder.stop();
            return true;
        }
    }


/*
    private static String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    private static int G_getIP()
    {
          if(appContext==null)
            return IC_NO;
        WifiManager wifi_service = (WifiManager) appContext.getSystemService(WIFI_SERVICE);

        WifiInfo info = wifi_service.getConnectionInfo();

        String wifiId;
        wifiId = (info != null ? info.getSSID() : null);
        if (wifiId != null) {
            wifiId = wifiId.replace("\"", "");
            if (wifiId.length() > 4)
                wifiId = wifiId.substring(wifiId.length() - 4);
        } else {
            wifiId = "nowifi";
        }

        int ip = info.getIpAddress();
        return ip;

    }
    */

    public static void F_AdjBackGround(Context context, int bakid) {

        Bitmap bmp = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), bakid, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        if (imageWidth <= 640 && imageHeight <= 480) {
            bmp = BitmapFactory.decodeResource(context.getResources(), bakid);
        } else {
            int scale = imageWidth / 640;
            if (scale <= 0) {
                scale = 2;
            }
            options.inSampleSize = scale;
            options.inJustDecodeBounds = false;
            bmp = BitmapFactory.decodeResource(context.getResources(), bakid, options);
        }

        if (bmp == null)
            return;

        //int ww = bmp.getWidth();
        //int hh = bmp.getHeight();
        //if (ww > 1280 || hh > 720)
        {

            int width = bmp.getWidth();
            int height =bmp.getHeight();
            int newWidth = ((width+7)/8)*8;
            int newHeight = ((height+7)/8)*8;

            Bitmap croppedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(croppedBitmap);
            Matrix frameToCropTransform;
            frameToCropTransform =
                    ImageUtils.getTransformationMatrix(
                            width, height,
                            newWidth, newHeight,
                            0, false);

            Matrix cropToFrameTransform = new Matrix();
            frameToCropTransform.invert(cropToFrameTransform);
            canvas.drawBitmap(bmp, frameToCropTransform, null);

            bmp.recycle();
            bmp = croppedBitmap;
        }

        int ww = bmp.getWidth();
        int hh = bmp.getHeight();

        int bytes = bmp.getByteCount();
        ByteBuffer buf = ByteBuffer.allocate(bytes);
        bmp.copyPixelsToBuffer(buf);
        byte[] byteArray = buf.array();
        naSetBackground(byteArray, ww, hh);
        bmp.recycle();
    }


    private static int getIP()
    {
        if(appContext!=null)
        {

            WifiManager wm=(WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
            if(wm!=null)
            {
                WifiInfo wi=wm.getConnectionInfo();
                if(wi!=null)
                {
                    int ipAdd=wi.getIpAddress();
                    return ipAdd;
                }
            }
            return 0;
        }
        return 0;
    }


    public static void naInitgl(Context context, int backid) {
        init();
    }


    private static void OnSave2ToGallery(String sName, int nPhoto)     //??????????????????????????????????????????????????????????????????
    {
        String Sn = String.format("%02d%s", nPhoto, sName);
        EventBus.getDefault().post(Sn, "SavePhotoOK");
    }


    private static void OnGetWifiData(byte[] data)        //GKA  ?????? ??????????????????
    {
        JH_Tools.AdjData(data);
        JH_Tools.FindCmd();
        JH_Tools.F_ClearData();
    }


    private static  void OnGetAngle(float nAngle)
    {
        Float  a = nAngle;
        EventBus.getDefault().post(a,"OnGetAngle");
    }



    //20000??????SDK????????????????????????????????????
    public  static int nSecuritySeed = -1;
    public  static int nSecurityStatus = 0;

    private static void OnGetGP_Status(int nStatus) {
        int nType =  ((nStatus>>16) & 0xFFFF);
        switch(nType)
        {
            case 0x1020:           //?????????PWM
            {
                Integer ix = (nStatus & 0xFF);
                EventBus.getDefault().post(ix, "OnGetPwmData");
            }
            break;
            case   0x3005:    //??????flash??????
            {
                int nLen = (nStatus & 0xFFFF);
                if (nLen > CmdLen)
                    nLen = CmdLen;
                byte[] cmd = new byte[nLen];
                ByteBuffer buf = wifination.mDirectBuffer;
                //buf.rewind();
                for (int i = 0; i < nLen; i++) {
                    cmd[i] = buf.get(i + BMP_Len);
                }
                EventBus.getDefault().post(cmd, "ReadDataFromFlash");
            }
            break;
            case   0x3006:      //???????????????
            {
                int re  = 1;
                int nLen = (nStatus & 0x00FF);
                if(nLen==1)
                {
                    re = 1;
                }
                else
                {
                    re = 0;
                }
                Integer ii = re;
                EventBus.getDefault().post(ii, "WriteData2FlashResult");
            }
            break;
            case 0xFFFF:                    //???????????????????????????????????? ?????????????????????????????????
            {
                int nLen = (nStatus & 0xFFFF);
                if (nLen > CmdLen)
                    nLen = CmdLen;

                byte[] cmd = new byte[nLen];

                ByteBuffer buf = wifination.mDirectBuffer;
                //buf.rewind();
                for (int i = 0; i < nLen; i++) {
                    cmd[i] = buf.get(i + BMP_Len);
                }

                //SecuritySeed =
                EventBus.getDefault().post(cmd, "GetDataFromRs232");
            }
            break;
            case 0x5443:            //wifi????????????
            {
                int nLen = (nStatus & 0xFF);

                byte[] cmd = new byte[nLen];

                ByteBuffer buf = wifination.mDirectBuffer;
                //buf.rewind();
                for (int i = 0; i < nLen; i++) {
                    cmd[i] = buf.get(i + BMP_Len);
                }
                EventBus.getDefault().post(cmd, "GetWifiSendData");
            }
            break;

            case 0x5444:
            {
                int nLen = (nStatus & 0xFF);
                byte[] cmd = new byte[nLen];

                ByteBuffer buf = wifination.mDirectBuffer;
                //buf.rewind();
                for (int i = 0; i < nLen; i++) {
                    cmd[i] = buf.get(i + BMP_Len);
                }

                nSecurityStatus = cmd[0];
                nSecuritySeed  = (cmd[1]&0xFF) + (cmd[2]&0xFF)*0x100 + (cmd[3]&0xFF)*0x10000 +(cmd[4]&0xFF)*0x1000000;
                Integer  da = nSecurityStatus;
//                if((nSecurityStatus & 0x01) == 1  && (nSecurityStatus & 0x02) ==0 )
//                {
//                    naSetSecurity(nSecuritySeed);
//                    EventBus.getDefault().post(da, "GetSecurityInfo");
//                }
//                else
                {
                    EventBus.getDefault().post(da, "GetSecurityInfo");
                }
            }
            break;
            case  0x2000://              ?????? ????????????????????????
            {
                int nLen = (nStatus & 0xFF);
                byte[] cmd = new byte[nLen];
                ByteBuffer buf = wifination.mDirectBuffer;
                //buf.rewind();
                for (int i = 0; i < nLen; i++) {
                    cmd[i] = buf.get(i + BMP_Len);
                }
                EventBus.getDefault().post(cmd, "GetWifiInfoData");
            }
            break;

            case 0x1021:
            case 0xFFFE:            //??????
            {
                Integer nB = nStatus &0x0F;;
                EventBus.getDefault().post(nB, "OnGetBatteryLevel");
            }
            break;

            case  0x0006:       //????????????Style
            {
                Integer nB = nStatus &0x0F;
                EventBus.getDefault().post(nB, "OnGetSetStyle");
            }
            break;

            case 0xFFFC:            //??????  -- ???????????????????????????SDK????????????SDK??????  OnKeyPress  ??????
                Integer ix = nStatus &0xFF;                //?????? ????????????
                EventBus.getDefault().post(ix, "OnGetGP_Status");
                break;

            case 0xFBFB:         //??????????????????UDP???????????????
                int nLen = (nStatus & 0xFFFF);
                byte[] cmd1 = new byte[nLen];
                ByteBuffer buf = wifination.mDirectBuffer;
                for (int i = 0; i < nLen; i++)
                {
                    cmd1[i] = buf.get(i + BMP_Len);
                }
                EventBus.getDefault().post(cmd1, "GetDataFromWifi");
                break;

            case 0xFFF0:     // UVCA ????????????
            {
                int xx = (nStatus>>8) &0xFF;
                Integer dat = (nStatus) &0xFF;
                switch (xx)
                {
                    case 0x11:
                        EventBus.getDefault().post(dat, "GetUVC_Brightness");
                        break;
                    case 0x12:
                        EventBus.getDefault().post(dat, "GetUVC_Contrast");
                        break;
                    case 0x13:
                        EventBus.getDefault().post(dat, "GetUVC_Saturation");
                        break;
                    case 0x14:
                        EventBus.getDefault().post(dat, "GetUVC_Zoom");
                        break;
                    case 0x15:
                        EventBus.getDefault().post(dat, "GetUVC_Panorama");
                        break;
                    case 0x16:
                        EventBus.getDefault().post(dat, "GetUVC_Inclination");
                        break;
                    case 0x17:
                        EventBus.getDefault().post(dat, "GetUVC_Roll");
                        break;
                    case 0x1A:
                        dat = 0x01;
                        EventBus.getDefault().post(dat, "PhotoKeySent");
                        break;
                    case 0x2A:
                        dat = 0x00;
                        EventBus.getDefault().post(dat, "PhotoKeySent");
                        break;

                    case 0x21:
                        dat|=0x8000;
                        EventBus.getDefault().post(dat, "GetUVC_Brightness");
                        break;
                    case 0x22:
                        dat|=0x8000;
                        EventBus.getDefault().post(dat, "GetUVC_Contrast");
                        break;
                    case 0x23:
                        dat|=0x8000;
                        EventBus.getDefault().post(dat, "GetUVC_Saturation");
                        break;
                    case 0x24:
                        dat|=0x8000;
                        EventBus.getDefault().post(dat, "GetUVC_Zoom");
                        break;
                    case 0x25:
                        dat|=0x8000;
                        EventBus.getDefault().post(dat, "GetUVC_Panorama");
                        break;
                    case 0x26:
                        dat|=0x8000;
                        EventBus.getDefault().post(dat, "GetUVC_Inclination");
                        break;
                    case 0x27:
                        dat|=0x8000;
                        EventBus.getDefault().post(dat, "GetUVC_Roll");
                        break;
                }
            }
            break;


        }
    }

    //// ???????????????????????????
    private static void RevTestInfo(byte[] info) {


    }

    // IC_GKA  ??????SD???????????????????????????
    private static void GetFiles(byte[] filesname) {
        String s1 = null;
        s1 = new String(filesname);
        EventBus.getDefault().post(s1, "GetFiles");      //?????????????????????????????????
    }


    //  ????????????????????????????????????
    private static void OnStatusChamnge(int nStatus) {
        Integer n = nStatus;
        EventBus.getDefault().post(n, "SDStatus_Changed");      //???????????????????????????????????????????????????

        //#define  bit0_OnLine            1
        //#define  bit1_LocalRecording    2
        //#define  SD_Ready               4
        //#define  SD_Recroding           8
        //#define  SD_Photo               0x10
    }


    //?????????????????? nError =1 ??????????????????
    private static void DownloadFile_callback(int nPercentage, String sFileName, int nError) {
        if (nError == 0) {
            ;
        }
        jh_dowload_callback jh_dowload_callback;
        if(nError==0xFF)
        {
            jh_dowload_callback = new jh_dowload_callback(nPercentage, sFileName);
        }
        else {
            jh_dowload_callback = new jh_dowload_callback(nPercentage, sFileName, nError);
        }
        EventBus.getDefault().post(jh_dowload_callback, "DownloadFile");
    }

    //??????????????? ???????????? data???160*90 ???????????????
    private static void GetThumb(byte[] data, String sFilename) {
        if (data != null) {
            MyThumb thumb = new MyThumb(data, sFilename);
            EventBus.getDefault().post(thumb, "GetThumb");      //????????????????????????????????????
        }
    }

    /////// ?????? SYMA ????????? --------
    private static void OnKeyPress(int nStatus) {
        Integer n = nStatus;
        Log.v("GKey","Key = "+nStatus);
        EventBus.getDefault().post(n, "key_Press");
        EventBus.getDefault().post(n, "Key_Pressed");
    }

    //?????????????????????300*300 ???????????? C?????????????????????????????????
    private static void GestureBmp(int i)
    {

        if(bGesture)
        {
            if(sig!=null) {
                if(!sig.isbBusy()) {
                    ByteBuffer buf = wifination.mDirectBuffer;
                    buf.rewind();
                    Gesture_bmp.copyPixelsFromBuffer(buf);
                    sig.GetNumber(Gesture_bmp);
                }
            }
        }
    }

    private  static Bitmap bmp = null;
    // ??????????????????
    private static void ReceiveBmp(int i) {
        //?????????i:bit00-bit15   ???????????????
        //      i:bit16-bit31  ???????????????
        // ????????????????????????????????????????????????
        // ?????????????????????mDirectBuffer???????????????ARGB_8888
        if(bmp==null)
            bmp = Bitmap.createBitmap(i & 0xFFFF, ((i >> 16)& 0xFFFF), Bitmap.Config.ARGB_8888);
        if(bmp.getWidth()!=(i & 0xFFFF) || bmp.getHeight()!=((i >> 16)& 0xFFFF) )
            bmp = Bitmap.createBitmap(i & 0xFFFF, ((i >> 16)& 0xFFFF), Bitmap.Config.ARGB_8888);
        mDirectBuffer.rewind();
        bmp.copyPixelsFromBuffer(mDirectBuffer);    //
        if(bRevBmp) {
            EventBus.getDefault().post(bmp, "ReviceBMP");
            EventBus.getDefault().post(bmp, "ReceiveBMP");
        }
    }

    ///////////video Media
    public  static int F_InitEncoder(int width,int height,int bitrate,int fps)
    {
//        String tmpFileName = sVideoName+"_.tmp";
//        if(tmpFileName!=null && tmpFileName.length()>10)
//        {
//            MyMediaMuxer.init(tmpFileName);
//        }
        return videoMediaCoder.initMediaCodec(width,height,bitrate,fps);
    }

    public   static  void offerEncoder(byte[] data,int nLen)
    {
        if(videoMediaCoder!=null)
        {
            videoMediaCoder.offerEncoder(data,nLen);
        }
    }


    public   static void F_CloseEncoder()
    {
        if(videoMediaCoder!=null)
        {
            videoMediaCoder.F_CloseEncoder();
        }
        G_StartAudio(0);
        MyMediaMuxer.stop();

        if(sVideoName==null)
            return;
        if(sVideoName.length()<10)
            return;

        File oldFile = new File(sVideoName+"_.tmp");
        File newFile = new File(sVideoName);
        if(newFile.exists() && newFile.isFile())
        {
            try {
                newFile.delete();
            }
            catch (Exception e)
            {

            }
        }
        if (oldFile.exists() && oldFile.isFile()) {
            oldFile.renameTo(newFile);
            String Sn = String.format("%02d%s", 1, sVideoName);
            sVideoName="";
            EventBus.getDefault().post(Sn, "SavePhotoOK");
        }
        else
        {
            sVideoName="";
            EventBus.getDefault().post(sVideoName, "SavePhotoOK");
        }



    }

    private static void onReadRtlData(byte[]data)
    {
        //REMODE0;
        if(data.length == 8 )
        {
            String string = new String(data);
            int nMode = -1;
            if(string.equals("REMODE0;"))
            {
                nMode = 0;
            }
            if(string.equals("REMODE1;"))
            {
                nMode = 1;
            }
            if(nMode!=-1)
            {
                Integer nM = nMode;
                EventBus.getDefault().post(nM,"onGetRtlMode");
                return;
            }
        }
        EventBus.getDefault().post(data,"onReadRtlData");
    }

    /////// ????????????
    public  static native  void naSetnAdjDelay(int nms);
    public  static native void naStartAdjFocus(int x,int y);
    public  static native int  naGetVcm();
    public  static native void  naSetVcm(int nvcm);
    private  static  void  onAdjFocus(int n)
    {
        Integer  data = n;
        EventBus.getDefault().post(data,"onAdjFocus");
    }

    public  static  native int  naGetUvcCameraCount();

    public  static  native int  naOpenUsbCamera(String sUsbCameraName);




    //2020-09-25
    public static native  void na4225_ReadDeviceInfo(); //??????  GP4225_GetDeviceInfo  ??????
    public static native  int  naSetIR(int n); //??????
    public static native  int  naReadIR();   // GP4225_GetIR_Status  ??????
    public static native  int  naSetPIR(boolean bEnable);  //PIR
    public static native  int  naReadPIR();// GP4225_GetPIR_Status  ??????
    public static native   void naSetStatusLedDisp(boolean b); //true  led ???????????????  false ??????????????????
    public static native   void naReadStatusLedDisp(); //??????  GP4225_GetLed_Status  ??????

    public  static native  int naTransferData(byte []data);


    public static native  int  RTL_Post(byte []data,int dataLen);


//2021-01-18


    // 0 - VGA 1 720p  2 = 1080P
    //?????????????????????
    public static native void naSetWifiResolution(int n);

    //?????? GP4225_GetResolution ????????????
    //???????????????
    public static native void naGetWifiResolution();


    private static  ByteBuffer picBuffer=ByteBuffer.allocate(60*60*4);
    private static PictureFromVideo_Interface globalPictureFromaeInterface = null;
    private static void onProgressGetPictureFromVideo(byte[]data,int nStatus)
    {
        if(globalPictureFromaeInterface !=null)
        {
            if(nStatus ==1)
            {
                int  nCountFrame = ((data[0] & 0xFF) +
                        (data[1] & 0xFF) * 0x100 +
                        (data[2] & 0xFF) * 0x10000 +
                        (data[3] & 0xFF) * 0x1000000);
                int nTimes = ((data[4] & 0xFF) +
                        (data[5] & 0xFF) * 0x100 +
                        (data[6] & 0xFF) * 0x10000 +
                        (data[7] & 0xFF) * 0x1000000);

                globalPictureFromaeInterface.onStart(nCountFrame,nTimes);
            }
            else if(nStatus == 2)
            {
                // ????????????buffer???
                picBuffer.rewind();
                picBuffer.put(data);
                //?????? limit ???postion ??? ?????? buffer ??????????????????
                picBuffer.rewind();
                Bitmap picBmp =  Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
                picBmp.copyPixelsFromBuffer(picBuffer);
                globalPictureFromaeInterface.onGetaPicture(picBmp);
            }
            else if(nStatus == 3)
            {
                globalPictureFromaeInterface.onEnd();
                globalPictureFromaeInterface = null;
            }
            else if(nStatus<0)
            {
                globalPictureFromaeInterface.onError(nStatus);
                globalPictureFromaeInterface = null;
            }
        }
    }



    private  static  void onGetVideoData() //????????????????????????????????????????????????????????????????????????????????????????????????????????????
    {
        EventBus.getDefault().post("","onGetVideoData");
        //Log.e(TAG,"GetData----");
    }

    private static native  int naGetPictListFromVideo_(String sPath);

    public  static  native  void naCancelGetPicList();

    public static  int naGetPictListFromVideo(String sPath,PictureFromVideo_Interface pictureFromVideo_interface)
    {
        if(globalPictureFromaeInterface!=null)
            return -1;
        globalPictureFromaeInterface = pictureFromVideo_interface;
        return  naGetPictListFromVideo_(sPath);

    }


//    static GPUImage  gpuImage;
//    private  static  void F_GET()
//    {
//        gpuImage.getBitmapWithFilterApplied()
//    }

    // BK security
    public static native int  naSetSecurity(int nSeed);
    public static native  void naCheckSecurityStatus(int nPassword);


    public static boolean CheckResolutionSupport(int width, int height)
    {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }
            String VCODEC="video/avc";
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(VCODEC)) {
                    MediaCodecInfo.CodecCapabilities codecCapabilities = codecInfo.getCapabilitiesForType(VCODEC);
                    if (codecCapabilities != null) {
                        MediaCodecInfo.VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
                        if (videoCapabilities != null) {
                            return videoCapabilities.isSizeSupported(width, height);
                        }
                    }
                }
            }
        }
        return false;
    }


    //????????? data_ = null ?????? nLen = 0
    public static  native void naSetBK_Para(byte []data_,int nLen);
    public static  native  void naSetBK_MacAddress(byte []data_,int nLen);

    //???????????????????????????
    public static  native void naSetCustomData(byte []data_,int nLen);


    private  static  void onGetOtaStatus(int nType,int Data) //OTA ????????????
    {
        Integer a = Data;
        if(nType == 0)
        {
            EventBus.getDefault().post(a,"onUpgradeStatus");
        }
        if(nType == 1)
        {
            EventBus.getDefault().post(a,"onUpgradePercent");
        }
    }

    public static native void naStartOta(byte[] data,long nLen);

}
