package com.joyhonest.wifination;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;

import org.simple.eventbus.EventBus;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Locale;


/**
 * Created by aivenlau on 16/7/13.
 */





public class wifination {



    public interface OnReceiveFrame{
        void onReceiveFrame(Bitmap bmp);
    }



    private static int nIx=0;


    public  static  OnReceiveFrame onReceiveFrame=null;


    public  final   static    int  GP4225_Type_Video = 1;
    public  final   static    int  GP4225_Type_Locked = 2;
    public  final   static    int  GP4225_Type_Photo = 3;


    public final    static   int   OnLine_Cmd_Pause = 1;
    public final    static   int   OnLine_Cmd_Resume = 2;
    public final    static   int   OnLine_Cmd_Pre = 3;
    public final    static   int   OnLine_Cmd_Next = 4;
    public final    static   int   OnLine_Cmd_Stop = 5;


    public final    static   int   OnLine_Status_Idl = 0;
    public final    static   int   OnLine_Status_Playing = 1;
    public final    static   int   OnLine_Status_Pause = 2;


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
    public final static int IC_GPRTPC  =    10;
    public final static int IC_RTLH264  =   11;
    public final static int IC_GPH264_34 =    12;



    public static AudioEncoder AudioEncoder;
    public static AudioCodecExt audioCodecExt;

    public final static int TYPE_ONLY_PHONE = 0;
    public final static int TYPE_ONLY_SD = 1;
    public final static int TYPE_BOTH_PHONE_SD = 3;
    public final static int TYPE_PHOTOS = 0;
    public final static int TYPE_VIDEOS = 1;
    public static ByteBuffer mDirectBuffer;
    //public static ByteBuffer mDirectBufferYUV;
    public static boolean bDisping = false;

    public static boolean bSupportPcmAudio = false;
    private  static VideoMediaCoder  videoMediaCoder;

    //private static  Context  mContext=null;

    private final static String TAG = "wifination";
    private static final wifination m_Instance = new wifination();
    private static final int BMP_Len = (((4096 + 3) / 4) * 4) * 4 * 2160 ;
    private  final  static int CmdLen = 2048;
    public static GP4225_Device gp4225_Device;

    public static Context appContext = null;


    private  static Bitmap Gesture_bmp =null; // = Bitmap.createBitmap(300,300, Bitmap.Config.ARGB_8888);

    static {
        try {
            System.loadLibrary("JoyWifiCamera");    //2024-07-09 //名称改为JoyCamera
            AudioEncoder = new AudioEncoder();
            videoMediaCoder = new VideoMediaCoder();
            audioCodecExt = new AudioCodecExt();

            gp4225_Device = new GP4225_Device();

            mDirectBuffer = ByteBuffer.allocateDirect(BMP_Len + CmdLen);     //获取每帧数据，主要根据实际情况，分配足够的空间。

            naSetDirectBuffer(mDirectBuffer, BMP_Len + CmdLen);
            //G_StartAudio(1);


        } catch (UnsatisfiedLinkError Ule) {
            Log.e(TAG, "Cannot load JoyCamera.so ...");
            Ule.printStackTrace();
        } finally {


        }
    }

//    public static void   naSetMaxResolution(int nWidth,int nHeight)
//    {
//        int len = (((nWidth + 3) / 4) * 4) * 4 * nHeight + 2048;
//        mDirectBuffer = ByteBuffer.allocateDirect(len + CmdLen);     //获取每帧数据，主要根据实际情况，分配足够的空间。
//        naSetDirectBuffer(mDirectBuffer, len + CmdLen);
//    }

    private wifination() {

    }

    //静态工厂方法
    public static wifination getInstance() {
        return m_Instance;
    }
    private static native void naSetDirectBuffer(Object buffer, int nLen);
    private static native void naSetDirectBufferYUV(Object buffer, int nLen);


    ////// ------------- -------------------------

    //初始化，开始接受wifi数据，显示图像
    /*
        IC_GKA：  sPath=@“1”  720P   sPath=@“2” VGA
        IC_GP：       sPath=@"http://192.168.25.1:8080/?action=stream"
        IC_GPRTSP     sPath = @"rtsp://192.168.26.1:8080/?action=stream"
        其他模块：      sPath=@“”;
    */


    public static native  void  naSetLedPWM(byte nPwm);
    public static native  void  naGetLedPWM();
    public static native  void  naGetBattery();
    public static native  void naGetBatteryInfo(); //获取更详细的电池信息，比如是否在充电以及电池百分比，这个需要有些固件可能不支持
    public static native  void  naGetWifiSSID();
    public static native  void  naSetWifiSSID(String sSSid);
    public static native  void  naSetLedMode(int nMode);
    public static native  void  naGetLedMode();





    //写数据到设备
    public static native  void naWriteData2Flash(byte[]data,int nLen);
    //需要读取设备数据，读取的数据从OnGetGP_Status 返回
    public static native  void naReadDataFromFlash();



    public static int naInit_1(String str)
    {
        bHandle = false;
        bEanbelHandle = true;
        return naInit(str);
    }
    public static int naStart(OnReceiveFrame _onReceiveFrame)
    {
        bHandle = false;
        onReceiveFrame = _onReceiveFrame;
        return naInitC("");
    }

    public static native int naInitC(String pFileName);
    public static  int naInit()
    {
        bSupportPcmAudio = false;
        return naInitC("");
    }
    public static  int naInit(String pFileName)
    {
        return naInitC(pFileName);
    }

    //停止播放
    private static native int naStopB();
    public static  native long naGetTime();
    public static  int naStop()
    {
        onReceiveFrame = null;
        bSupportPcmAudio = false;
        return naStopB();
    }
    //向飞控发送命令OnGetGP_Status
    public static native int naSentCmd(byte[] cmd, int nLen);

    //图像是否翻转
    public static native void naSetFilp(boolean b);
    // 是否VR显示
    public static native void naSet3D(boolean b);


    public static native  void naStartRead20000_20001();





    public static native void naSetDarkcornerWidth(int nleftTopDx,int nleftTopDy,int nrightbottomDx,int nrightbottomDy);     //有些产品，因为镜头问题，会有暗角，这个只是来设定消除暗角的值。


    //TYPE_ONLY_PHONE   ==  录像或者拍照到手机
    //TYPE_ONLY_SD     ==  录像或者拍照到模块的SD卡（目前只对应GKA模块有效)
    // TYPE_BOTH_PHONE_SD  ==  录像或者拍照同时到模块的SD卡和手机
    //
    //拍照
    public static native int naSnapPhoto(String pFileName, int PhoneOrSD);
    //录像
    private static native int naStartRecordA(String pFileName, int PhoneOrSD);

    private static  String sVideoName="";
    public static  boolean bG_Audio=false;



    public static  native void naSetScaleHighQuality(int nQ);

    public static  int naStartRecord(String pFileName, final  int PhoneOrSD)
    {

        if(PhoneOrSD != TYPE_ONLY_SD) {
            sVideoName = pFileName;
        }
        String tmpFileName = sVideoName+"_.tmp";
        if(PhoneOrSD == TYPE_BOTH_PHONE_SD || PhoneOrSD == TYPE_ONLY_PHONE)
        {
            if(isPhoneRecording()) {
                return 0;
            }

            if(tmpFileName.length()>10)
            {
                int i = MyMediaMuxer.init(tmpFileName);
                if(i<0)
                {
                    SystemClock.sleep(500);
                    MyMediaMuxer.init(tmpFileName);
                }
            }
            if(bG_Audio && AudioEncoder.nRecType != 1)
            {
                if(!AudioEncoder.isCanRecordAudio())  //判读是否可以录音，因为有时录音权限没有打开就无法录音
                {
                    bG_Audio = false;
                }
            }

            if(bG_Audio)
            {
                boolean re = G_StartAudio(1);
                if(!re) //录音权限被拒绝
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
        return 0;
    }

    // 获取录像时间 ms
    public static  int  naGetRecordTime()
    {
        return (int)videoMediaCoder.getRecordTime();
    }
    //停止录像
    public static native void naStopRecord(int PhoneOrSD);
    //停止所有录像
    public static native int naStopRecord_All();
    //录像到手机时，是否录音
    public static  void naSetRecordAudio(boolean b)
    {
        bG_Audio = b;
    }


    //手机是否在录像

    public static native  boolean naIsJoyCamera();  //判断连接的WIFI是否是我司的产品

    public static native boolean isPhoneRecording();
    //设定手机录像的分辨率，一般无需设定，默认位模块传回视频分辨率
    public static native int naSetRecordWH(int ww, int hh);

    //2022-03-31
    //Bit0 VGA（640x480) 0: 不支持， 1：支持
    //Bit1 720P(1280x720) 0: 不支持， 1：支持
    //Bit2 1080P(1920x1080) 0: 不支持， 1：支持
    //Bit3~31 暂时保留 0: 不支持， 1：支持
    public static native void naSetSDRecordResolution(int nResolution);
    public static  native void naGetSDRecordResolution();

    public static  native void  naSetMicOnOff(boolean bOn);
    public static  native void  naGetMicOnOff();

    public static native  int naGetDispWH();
    //设定是否需要SDK内部来显示，b = true， SDK 把解码到的图像发送到JAVA，由APP自己来显示而不是通过SDK内部来渲染显示
    // SDK解码后图像 由 ReceiveBmp 返回
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
            GP4225 凌通 支持SD卡录像+
    */

    //同步时间
    public static native void naSyncTime();//New ver
    public static native void na4225_SyncTime(byte[] data,int nLen);  //Old ver
    public static native void na4225_ReadTime();
    //是否显示水印
    public static native void na4225_SetOsd(boolean  b);
    public static native void na4225_ReadOsd();
    //设备图像翻转
    public static native void na4225_SetReversal(boolean  b);
    public static native void na4225_ReadReversal();
    //设置录像分段时间 - 0  1min  1  - 3min  2 - 5min
    public static native void na4225_SetRecordTime(int n);
    public static native void na4225_ReadRecordTime();
    //格式化SD卡
    public static native void na4225_FormatSD();
    //读取固件版本信息
    public static native void na4225_ReadFireWareVer();

    //恢复出厂设置
    public static native void na4225_ResetDevice();

    public static   void  naGetFirmwareVersion()
    {
        na4225_ReadFireWareVer();
    }





    /*
        APP读取状态信息
    */
    public static native void na4225_ReadStatus();
    public static void naGetStatus()
    {
        na4225_ReadStatus();
    }

    /*
            APP设定工作模式
            0  实时图像  录像模式
            1  文件操作
     */

    public static native void na4225_SetMode(byte nMode);

    /*
        APP 查询文件列表
        先调用na4225_SetMode，进入文件列表模式
        nType = 1;  视频
        nType = 2;  锁定视频
        nType = 3'  相片
        nType = 4'  锁定相片
 */

    public static native void na4225_GetFileList(int nType, int nStrtinx,int nEndinx);
    public static native void na4225_DeleteFile(String sPath,String sFileName);
    public static native void na4225_DeleteAll(int nType); //  2 videos 3 photos   4 all
    public static native void na4225_GetSDFileThumbnail(String sPath,String sFileName,int nLen,String sSaveName);
    public static native int na4225_CreateThumbnailTcp(boolean b);


    public static native void na4225_SetTcpReadDelay(int nMs);

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
        文件下载
    */

    public static native boolean na4225StartDonwLoad(String sPath,String sFileName,int nLen,String sSaveName);
    public static native boolean  na4225StartPlay(String sPath,String sFileName,int nLen); //旧版本

    //在线播放新版本，只有新版固件支持 bFastTcp = true 支持
    public static native boolean  na4225StartPlay_newVer(String sPath,String sFileName,int nLen);   //新版本播放
    public static native boolean  na4225OnLinePlaySetStatus(int nCmd);
    public static native boolean  na4225OnLinePlayGetStatus();
    public static native boolean naDisConnectedTCP();
    ///////// 4225 end --------------

    private static native void naSetRevBmpA(boolean b);

    //设定是否手势识别， True，每一帧也会由 ReceiveBmp 返回，不同的是 SDK内部还是会显示视频。 如果APP 自己来实现手势识别和显示，
    // 可以用 naSetRevBmp 来替代
    private static native void naSetGestureA(boolean b);


    //RTL

    //onReadRtlData  返回读取结果。

    public static native  int naGetRtl_Mode();
    public static native  int naGetRtl_List(int bImage,int inx);
    public static native  int naDownLoadRtlFile(String sFileName);
    public static native  int naCancelRTL();


    //设定 客户 只针对 GKA， “sima” 表示 客户是司马 ，目前只有这一个设定
    public static native void naSetCustomer(String sCustomer);
    //获取SD卡列表 (针对  IC_GK_A 以下只对 IC_GKA
    public static native int naGetPhotoDir();
    public static native int naGetVideoDir();
    public static native int naGetFiles(int nType);
    public static native int naDownloadFile(String sPath, String dPath);
    public static native int naCancelDownload();
    public static native int naDeleteSDFile(String fileName);





    //获取SD卡中视频的的缩略图(针对  IC_GKA),一般建议如果已经下载到手机的视频文件,利用系统函数来获取缩略图,本函数主要是用于获取没有下载到手机
//的SD卡中的视频文件缩略图,调用次函数后,SDK会回调 GetThumb(byte[] data,String sFilename), data 是缩略图数据,filename是表明是哪个视频文件
//一般,我们在调用naGetVideoDir()时, 在回调函数GetFiles(byte[] filesname)得到文件名,在调用此函数来获取缩略图
    public static native int naGetThumb(String filename);  //针对国科模块




    public static native int naCancelGetThumb();   //针对国科模块

    //获取手机中视频文件的缩略图,添加这个函数是因为有时手机的系统函数兼容性不会，有时无法获取到缩略图。
    private static native int naGetVideoThumbnailB(String filename,Bitmap bmp);


    public static native  void naSetDispStyle(int nType); //0-6

    ///旧接口,不建议用....

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
            return naSetRecordWH(-1, -1); //默认摄像头传来的分辨率
        }
    }

    public static native int naGetSessionId();
    public static native void naSetGKA_SentCmdByUDP(boolean bUdp);
    //GP_RTSP
    //获取模块类型
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

    //跟随
    public static native void naSetFollow(boolean bFollow);
    public static native void naSetContinue();

    //Sunbplus
    public static native int naSetMenuFilelanguage(int nLanguage);


    public static native void naFillFlyCmdByC(int nType);

    public static native int naSave2FrameMp4(byte[] data, int nLen, int tyoe, boolean bKeyframe);

    public static native int naGetFps();

    public static native int naGetwifiFps();

    public static native String naGetControlType(); //获取飞控型号，主要针对 SYMA  国科t

    public static native boolean naSetBackground(byte[] data, int width, int height);

    public static native void naSetAdjFps(boolean b); //对应国科IC，有些早期固件不支持调整FPS，所以需要增加这一条命令


    //雷达
    public static  native void naSetRadarAdj(boolean b);


    //镜头传过来的数据旋转 0 90 180 270
    public static native  void naSetCameraDataRota(int n);



    public static native void naSetVrBackground(boolean b);

    public static native void naRotation(int n);  // n == 0 || n ==90 || n ==-90 || n ==180 || n==270
    public static  native void naSetbRotaHV(boolean b); //b = flase  表示手机是竖屏显示，但因为我们的camera是横屏数据，所以还需调用 naRotation 来转 90度满屏显示
    //b = true,  手机横屏显示，此时如果调用 naRotation， 就只是把 显示画面旋转 ，如果转 90 ，-90 270 ，就会显示有 黑边
    public static native boolean naSetWifiPassword(String sPassword);
    public static native void naSetLedOnOff(boolean bOpenLed);


    public static native void naSetScale(float fScal); //设定放大显示倍数

    public static void naSetCmdResType(int nType)
    {
        JH_Tools.F_SetResType(nType);
    }


    public static native void naSetNoTimeOut(boolean b);    //



    public static native void naSetDebug(boolean b);//串口数据 ，用于与固件调试

    public static native void naWriteport20000(byte[] cmd,int nleng);

    public static native  void naSetMirror(boolean b);


    public static  native void naSetSnapPhoto(int w,int h,boolean b);


    public static native void init();

    public static native void release();

    public static native void changeLayout(int width, int height);

    public static native void drawFrame();


    public static native  void naSetSaveTypeByName(boolean b); //b = true  储存照片时，根据 传入的 文件名 后缀 来确定格式。  false   强制以 jpg格式储存


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

    public static native void naSetGsensor2SDK(int xx,int yy,int zz);  //这是利用Rotate Filter 来旋转

    public static void naSetProcessesGsensorDataByApp(boolean b)    //如果有APP来处理Gsensor 来计算角度。 然后有APP调用naSetAngle 来旋转图片。
    {
        gp4225_Device.bProcessesGsensorDataByApp = b;
    }
    public static  native  void naSetAngle(float  Angle);

    public  static native void naSet3DDenoiserPara(String sPara);
    public static native void naSet3DDenoiser(boolean b); //视频是否3D降噪
    public static native void naSetEnableRotate(boolean b); //视频是否可以旋转任意角度。

    public static native int naSetRotate(int nRotate);  //这是利用Rotate Filter 来旋转 //返回 0 表示  naSetEnableRotate（false)

    public static native void naSetSensor(boolean b);   //GSensor on or off
    public static native void naSetSensorA(boolean b);   //GSensor on or off 如果由外部来旋转图片，调用此方法
    public static native void naSetCircul(boolean b);   //GSensor 打开时，是否圆形显示？
    public static native void naSetsquare(boolean b);   //GSensor 打开时，是否正方形形显示？
    public static native void naSetsquare_fit(boolean b);   //GSensor 打开时，是否长方形切并且随gsensor旋转满屏显示？
    //bEnableCircul

    public static native void naSetAcdetection(boolean b);  //电流检测 on or off
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



    ////////////   操作摄像头  2020-04-26 添加 /////////
    public  static  native  void naSetUVCA_Brightness(int nBrightness);   //亮度 -64~+64
    public  static  native  void naSetUVCA_Contrast(int nContrast);     //对比度  0-100
    public  static  native  void naSetUVCA_Saturation(int naturation);     //饱和度 0-100
    public  static  native  void naSetUVCA_Zoom(int nZoom);     //缩放 0-255
    public  static  native  void naSetUVCA_Panorama(int nPanorama);     //全景 0-255
    public  static  native  void naSetUVCA_Inclination(int nInclination);  //倾斜 0-255
    public  static  native  void naSetUVCA_Roll(int nRoll);     //滚动


    public  static  native  void naSentPhotoKey();


    //读取 ，
    public  static  native  void naGetUVCA_Brightness();   //亮度 -64~+64
    public  static  native  void naGetUVCA_Contrast();     //对比度  0-100
    public  static  native  void naGetUVCA_Saturation();     //饱和度 0-100
    public  static  native  void naGetUVCA_Zoom();     //缩放 0-255
    public  static  native  void naGetUVCA_Panorama();     //全景 0-255
    public  static  native  void naGetUVCA_Inclination();  //倾斜 0-255
    public  static  native  void naGetUVCA_Roll();     //滚动
    //////////
/*
    public static  native  void CancelNoiseInit(int frame_size, int sample_rate);
    public static  native  int CancelNoisePreprocess(byte[] cmd, int nLen);
    public static  native  void CancelNoiseDestroy();
*/

    public  static boolean  bGesture = false;
    public  static boolean  bRevBmp = true;
    private static ObjectDetector sig=null;
    public  static native int naSetTransferSize(int nWidth,int nHeight);   //宽度必须是8的倍数
    public static void naSetGesture_vol(float aa)
    {
        ObjectDetector.MINIMUM_CONFIDENCE_TF_OD_API = aa;
    }

    public static native boolean naSentUdpData(String sIP, int nPort,byte[] data, int nLen);
    public static native boolean naStartReadUdp(int nPort); // 收到数据后，会通过 onUdpRevData 返回
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

    private static  void onGetLedPwm(int nLed_)    //GP 30 模块返回LED PWM设定值
    {
        Integer  nLed = nLed_;
        EventBus.getDefault().post(nLed,"onGetLed");
        EventBus.getDefault().post(nLed,"onGetLedPWM");
    }



    private static  void onUdpRevData(byte[] data,int nPort,int nIP)      // naStartReadUdp，后，读取到的数据从这里返回
    {
        UpdData  udp_data = new UpdData(data,nPort);
        if(nPort == 20001) {
            if (bProgressGP4225UDP) {
                gp4225_Device.nCameraIP = nIP;
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

    public static Bitmap naGetVideoThumbnail(String filename,int w,int h)
    {
        Bitmap bitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
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


    private static void OnSave2ToGallery(String sName, int nPhoto)     //拍照或者录像完成。可以把它加入到系统图库中去
    {
        String Sn = String.format(Locale.ENGLISH,"%02d%s", nPhoto, sName);
        EventBus.getDefault().post(sName, "onSnapPhotoFinish");
        EventBus.getDefault().post(Sn, "SavePhotoOK");
    }


    private static void OnGetWifiData(byte[] data)        //GKA  返回 模块透传数据
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



    //20000端口SDK内部没有处理的在此处返回
    public  static int nSecuritySeed = -1;
    public  static int nSecurityStatus = 0;

    private static void OnGetGP_Status(int nStatus) {
        int nType =  ((nStatus>>16) & 0xFFFF);
        switch(nType)
        {
            case 0x1020:           //夜视灯PWM
            {
                Integer ix = (nStatus & 0xFF);
                EventBus.getDefault().post(ix, "OnGetPwmData");
            }
            break;
            case   0x3005:    //读取flash数据
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
            case   0x3006:      //写数据结果
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
            case 0xFFFF:                    //所有通过串口传过来的数据 ，用于与固件调试时使用
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
            case 0x5443:            //wifi透传数据
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
            case  0x2000://              回传 模块本身信息数据
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
            case 0xFFFE:            //电量
            {
                Integer nB = nStatus &0x0F;
                EventBus.getDefault().post(nB, "OnGetBatteryLevel");
            }
            break;

            case  0x0006:       //返回显示Style
            {
                Integer nB = nStatus &0x0F;
                EventBus.getDefault().post(nB, "OnGetSetStyle");
            }
            break;

            case 0xFFFC:            //按键  -- 这是为了兼容之前的SDK，新版的SDK通过  OnKeyPress  返回
                Integer ix = nStatus &0xFF;                //返回 模块按键
                EventBus.getDefault().post(ix, "OnGetGP_Status");
                break;

            case 0xFBFB:         //所有其他通过UDP返回的数据
                int nLen = (nStatus & 0xFFFF);
                byte[] cmd1 = new byte[nLen];
                ByteBuffer buf = wifination.mDirectBuffer;
                for (int i = 0; i < nLen; i++)
                {
                    cmd1[i] = buf.get(i + BMP_Len);
                }
                EventBus.getDefault().post(cmd1, "GetDataFromWifi");
                break;

            case 0xFFF0:     // UVCA 状态返回
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

    //// 测试信息。。。。。
    private static void RevTestInfo(byte[] info) {


    }

    // IC_GKA  获取SD卡文件列表回调函数
    private static void GetFiles(byte[] filesname) {
        String s1 = null;
        s1 = new String(filesname);
        EventBus.getDefault().post(s1, "GetFiles");      //调用第三方库来发消息。
    }


    //  当模块状态改变时回调函数
    private static void OnStatusChamnge(int nStatus) {
        Integer n = nStatus;
        EventBus.getDefault().post(n, "SDStatus_Changed");      //调用第三方库来发送消图片显示消息。
        EventBus.getDefault().post(n, "onCameraStatusChanged");      //调用第三方库来发送消图片显示消息。

        //#define  bit0_OnLine            1
        //#define  bit1_LocalRecording    2
        //#define  SD_Ready               4
        //#define  SD_Recroding           8
        //#define  SD_Photo               0x10
    }


    //下载文件回调 nError =1 表示有错误。
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

    //获取缩略图 回调函数 data，160*90 的图像数据
    private static void GetThumb(byte[] data, String sFilename) {
        if (data != null) {
            MyThumb thumb = new MyThumb(data, sFilename);
            EventBus.getDefault().post(thumb, "GetThumb");      //调用第三方库来发送消息。  //针对国科模块
        }
    }

    /////// 以下 SYMA 不使用 --------
    private static void OnKeyPress(int nStatus) {
        Integer n = nStatus;
        Log.v("GKey","Key = "+nStatus);
        EventBus.getDefault().post(n, "key_Press");
        EventBus.getDefault().post(n, "Key_Pressed");
        EventBus.getDefault().post(n, "onGetKey");
    }

    //返回手势识别的300*300 图像，用 C来处理缩放用来提供效率
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


    public static void  naResetHandle()
    {
        bHandle = false;
    }

    private  static Bitmap bmpG = null;
    // 获取一帧图像
    private  static  boolean bHandle = false;
    private  static   boolean bEanbelHandle= false;




    private static void ReceiveBmp(int i) {
        //其中，i:bit00-bit15   为图像宽度
        //      i:bit16-bit31  为图像高度
        // 此函数需要把数据尽快处理和保存。
        // 图像数据保存在mDirectBuffer中，格式为ARGB_8888

        //Log.e(TAG,"get framne");

        if(bHandle && bEanbelHandle)
            return;
        bHandle = true;
        if(bRevBmp) {
            int w = i & 0xFFFF;
            int h = ((i >> 16) & 0xFFFF);
            mDirectBuffer.rewind();
            if(onReceiveFrame!=null)
            {
                if(bmpG!=null)
                {
                    if(bmpG.getWidth() !=w || bmpG.getHeight()!=h)
                    {
                        bmpG = null;
                    }
                }
                if(bmpG==null)
                {
                    bmpG = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                }
                bmpG.copyPixelsFromBuffer(mDirectBuffer);
                onReceiveFrame.onReceiveFrame(bmpG);
                bHandle = false;
            }
            else {
                //if(gp4225_Device.nMode == 0)
                {
                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    bmp.copyPixelsFromBuffer(mDirectBuffer);
                    EventBus.getDefault().post(bmp, "ReceiveBMP");
                    EventBus.getDefault().post(bmp, "onGetFrame");
//                if(bmpG!=null)
//                {
//                    if(bmpG.getWidth() !=w || bmpG.getHeight()!=h)
//                    {
//                        bmpG = null;
//                    }
//                }
//                if(bmpG==null)
//                {
//                    bmpG = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
//                }
//                bmpG.copyPixelsFromBuffer(mDirectBuffer);
//                EventBus.getDefault().post(bmpG, "ReceiveBMP");
                }
            }
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
        if(videoMediaCoder!=null) {
            try {
                videoMediaCoder.offerEncoder(data, nLen);
            }
            catch ( Exception ignored)
            {

            }
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
            catch (Exception ignored)
            {

            }
        }
        if (oldFile.exists() && oldFile.isFile()) {
            oldFile.renameTo(newFile);
            String Sn = String.format(Locale.ENGLISH,"%02d%s", 1, sVideoName);
            EventBus.getDefault().post(sVideoName, "onRecordFinish");
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

    /////// 自动调焦
    public  static native  void naSetnAdjDelay(int nms);
    public  static native void naStartAdjFocus(int x,int y);
    public  static native int  naGetVcm();
    public  static native void  naSetVcm(int nvcm);
    private  static  void  onAdjFocus(int n)
    {
        Integer  data = n;
        EventBus.getDefault().post(data,"onAdjFocus");
    }

    private static void onGetSensorRotationAngle(float nAngle)
    {

    }

    public  static  native int  naGetUvcCameraCount();

    public  static  native int  naOpenUsbCamera(String sUsbCameraName);




    //2020-09-25
    public static native  void na4225_ReadDeviceInfo(); //读取  GP4225_GetDeviceInfo  返回
    public static native  int  naSetIR(int n); //红外
    public static native  int  naReadIR();   // GP4225_GetIR_Status  返回
    public static native  int  naSetPIR(boolean bEnable);  //PIR
    public static native  int  naReadPIR();// GP4225_GetPIR_Status  返回
    public static native   void naSetStatusLedDisp(boolean b); //true  led 状态灯显示  false 不打开状态灯
    public static native   void naReadStatusLedDisp(); //读取  GP4225_GetLed_Status  返回

    public  static native  int naTransferData(byte []data);


    public static native  int  RTL_Post(byte []data,int dataLen);


//2021-01-18


    // 0 - VGA 1 720p  2 = 1080P
    //设定图传分辨率
    public static native void naSetWifiResolution(int n);

    //通过 GP4225_GetResolution 消息返回
    //图传分辨率
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
                // 数组放到buffer中
                picBuffer.rewind();
                picBuffer.put(data);
                //重置 limit 和postion 值 否则 buffer 读取数据不对
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


    public static native void naSetKeepLive(boolean b);

    private  static  void onGetVideoData() //读取到了视频数据，不一定是完整的一帧数据，只是接收到了视频数据，就会回调
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
    //    int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecList mediaCodecList = new MediaCodecList(0);
        MediaCodecInfo[] arraya = mediaCodecList.getCodecInfos();
        if(arraya!=null)
        {
            for(MediaCodecInfo codecInfo :arraya)
            {
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
        }
        return  false;
        /*
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

         */
    }


    //读取， data_ = null 或者 nLen = 0
    public static  native void naSetBK_Para(byte []data_,int nLen);
    public static  native  void naSetBK_MacAddress(byte []data_,int nLen);

    //根据自定义协议来。
    public static  native void naSetCustomData(byte []data_,int nLen);


    private  static  void onGetOtaStatus(int nType,int Data) //OTA 状态返回
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


    //读取摄像头参数设定

    public static native void naGetCameraPara();
    public static native void naSetDeviceEV(int nEv);   //曝光
    public static native void naSetDeviceAutoEV(boolean nEv);   //自动曝光
    public static native void naSetLightFreq(boolean b50Hz);  //平率
    public static native void naSetDeviceBrightness(int nBrightness);// 亮度
    public static native void naSetDeviceContrast(int nContrast);// 对比度

    public static native void naSetDeviceSaturation(int nSaturation);// 饱和度
    public static native void naSetDeviceImageQuality(int nQuality);// 图像质量

    public static native void naSetDeviceSharpness(int nSharpness);// 锐度
    public static native void naSetDeviceWhiteBalance(int bAuto);// 白平衡


    public static native void naSetTimeOsd(int x,int y,int nDateType); //x设置足够大就会不显示
    public static native  void naSetDeviceWatermark(int nDateType); // bit0-3: 0 off 1 on, bit4-7:  0 Y/M/D  1: D/M/Y 2 M/DY
    public static native  void naGetDeviceWatermark();  //结果通过 onGetDeviceWatermark 返回

    public static  native void naSaveSetparaSave2Flash();

    // audioFormat  AudioFormat.ENCODING_PCM_16BIT or  AudioFormat..ENCODING_PCM_8BIT
    //  nFreq = 8000,.....
    private static native  boolean StartPlayAudioNative();
    private static native  boolean StartPlayAudioNative_online();
    private static native  void StopPlayAudioNative();
    //audioFormat  AudioFormat.ENCODING_PCM_16BIT or  AudioFormat..ENCODING_PCM_8BIT
    //nFreq = 8000,.....


    private static void WriteAudioData(byte[] data) //SDK 内部调用
    {
        bSupportPcmAudio = true;
        if((nIx % 0x1F) ==0)
        {
            EventBus.getDefault().post(data,"onGetPcmData");
        }
        nIx++;
        GP4225_Device.WriteAudioData(data);     //播放
    }

    private static void ConvertWriteAudiData(byte[] data)
    {
        audioCodecExt.WriteData(data);
    }

    public static native void naGetPcmInfo();

    public static void naStartPlayAudio(int nFreq,int audioFormat)
    {
        GP4225_Device.F_StartPlayAudio(nFreq,audioFormat);
        StartPlayAudioNative();
    }
    public static void naStartPlayAudio_for_online(int nFreq,int audioFormat)
    {
        GP4225_Device.F_StartPlayAudio(nFreq,audioFormat);
        StartPlayAudioNative_online();
    }
    public static void naStopPlayAudio()
    {
        StopPlayAudioNative();
        GP4225_Device.F_StopPlayAudio();
    }

    public static void naSetRecordAutioExt(boolean b) //录制的声音是从wifi端传来的
    {
        GP4225_Device.bWifiPcm = true;
        AudioEncoder.SetDataExt(b);
    }


    public static void naSetPCM2Speaker(boolean b)
    {
        GP4225_Device.F_Set2Spaker(b);
    }


    public static native  void naGetDeviceCategory(); //获取设备分类。  2023-03-24 添加


    public static  native void naSetCheckDissconnectedTime(int nSecs);

    public static native  void  naSetTest(boolean bTest);

    public static native void naSetOnLinePlayHeighResolution(boolean b);

    private static  native  int naConvertA(String sPath,String sOutPath);
    public static   native void naCancelConvert();

    public static native int naIsSupportAudioAndMJ(String sPath);
    private static  void onConverntEvent(int nStatus)
    {
        Integer ia = nStatus;
        EventBus.getDefault().post(ia,"onConverntEvent");
    }

    public static native boolean naIsNeedConvert(String sPath);

    public static int naCovert(String sPath,String sOutPath) {
        MyMediaMuxer.init(sOutPath);
        int re =naIsSupportAudioAndMJ(sPath);
        if(re >=0 && (re & 0x01 ) !=0)  {  //有声音
            GP4225_Device.bWifiPcm = false;
            wifination.naSetRecordAudio(true);
            wifination.naSetRecordAutioExt(true);
            G_StartAudio(1);
        }
        return naConvertA(sPath,sOutPath);
    };


    public static native void  naSetSystemControlData(byte []data);
    //设定 比如 自动关机时间参数。。。。。
    public static native void  naGetSystemControlData();

    private static void onChangeBWMode(int nBw)    // 图传是否进入黑白图传
    {
        Integer n = nBw;
        EventBus.getDefault().post(n,"onChangeBWMode");
    }
    public static native void naSetBrightness(float fBrightness); //
    public static native void naSetContrast(float fContrast);
    public static native void naSetSaturation(float fSaturation);
    public static native void naSetEnableEQ(boolean b);


    //2024-0508 增加 局域网内摄像头

    public static native int naSetCameraIPandType(String sIP ,int nType);

    public static native int naSetCameraPara(int flag,int nSet1,int nSet2,int nSet3,int nSet4,int nSet5,int nSet6,int nSet7,int nSet8);

//  2024-07-09 添加 打印功能

    private static  void onSentPercent(int nPercent)
    {

    }
    public static native  int naSetPrinter(Bitmap bmp);
    public static native  int naInitPrinter();
    public static native  int naReleasePrinter();
    public static native  int naCreateTCP();
    public static native  void naReleaseTCP();
    public static native  int naSetPrintDataFormat(int nSet1,int nSet2);
    //nSet1 点阵，灰度  nSet2 打印浓度
    public static  native  int naStartSendData();
    public static native  int naCancelPrint();
    //1 naInitPrinter
    //2 naCreateTCP
    //3 naSetPrinter
    //4 naSetPrintDataFormat
    //5 等待返回状态
    //6  naStartSendData;
    //7 等待返回状态
    //8  naReleaseTCP
    //9 naReleasePrinter

    public static native void naSetRota90Disp(boolean b);


    public static native int naSetPicWaterMark(String sPath,boolean b,float para1,float para2); //设定水印贴图
    // para1 //水印图片占 图片的宽度比分比
    //para2 //s水印图片的高宽比

    public static native void naSetOsdFontFilePath(String fontFilePath);
    public static native void naSetTimeOsd_new(int nPos,int nDateType);
    /*nPos
        <0 no disp
        0 up-left
        1 up-right
        2 bottom-left
        3 bottom-right
      */

    //nDateType 0  Y-M-D  1 M-D-Y  2 = D-M-Y

    public static  native  void naSetOsdTextSize(int nSize);
    public static native   void naSetOsdTextColor(int nColor);

    public static native void naSetSensorSensitivity(int n,int n2);
    //低4bit 0 = 失效 n =1 low; n = 2 med;n = 3 hight
    // 2024-08-14 新版本的设定函数，小邱项目需要用旧版的，在另外一个SDK
    //高4bit 具体的灵敏度设定。 此时，低4bit失效。
    //n2 >=0 设定测试用灵敏度
    public static native int naGetSensorSensitivity();

    //2024-08-20
    public static native  void naSetSuportSTA(boolean b);
    // AP 模式下，发送wifi连接 设置 ssid 和 password
    public static final int WiFIPass_noPass = 0;
    public static final int WiFIPass_wdp = 1;
    public static final int WiFIPass_wpa2 = 2;
    public static final int WiFIPass_wpa3 = 3;
    //naSetWifiConnectedInfo
    public static native  boolean naSetStaConnectedInfo(String ssid,String sPass,int passType); // 0:no pwd, 1:wep, 2:wpa2, 3:wpa3
    public static native  void naSetCameraIP(String sIP,int nType);
    //JH_Guset  84682002
    public static native  boolean naGetStaConnectedInfo(); // 0:no pwd, 1:wep, 2:wpa2, 3:wpa3
    public static  native boolean naStartScanCamera();


    public static void  naSetVolAdj(boolean b)
    {
        AudioEncoder.setbVolAdj(b);
    }
    public static void naSetVolAdjValue(float f)
    {
        AudioEncoder.setfVolAdj(f);
    }



    public static native void naSetRedChannel(int nRedCh);
    public static native void naSetGreenChannel(int nGreenCh);
    public static native void naSetBlueChannel(int nBlueCh);




    //2025-06-27 和凡昆讨论确定
    public static native  void naSetTftOrientation(int n);
    public static native  void naGetTftOrientation();



    public  static native void naGetAPP_Special_Function();//用于和 APP 特殊功能交互，比如 APP 是否要支持和显示 WIFI 密码设置 UI、热点设置 UI 等、图像 EV 调节等等。

    public static native boolean naSetWifiPasswordNewVer(String sPassword);
    public static native void naGetWifiPasswordNewVer();



}
