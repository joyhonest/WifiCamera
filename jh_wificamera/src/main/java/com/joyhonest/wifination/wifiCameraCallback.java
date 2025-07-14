package com.joyhonest.wifination;

import android.graphics.Bitmap;
import com.joyhonest.wifination.GP4225_Device.GetFiles;
import com.joyhonest.wifination.GP4225_Device.MyFile;
import com.joyhonest.wifination.GP4225_Device.OnLinePlayStatus;

public interface wifiCameraCallback {


    //这个接口暂时还没有使用！！！！！

    void onGetFrame(Bitmap bmp);


    //#define  bit0_OnLine            1
    //#define  bit1_LocalRecording    2
    //#define  SD_Ready               4
    //#define  SD_Recroding           8
    //#define  SD_Photo               0x10
    default void onCameraStatusChanged(int n){};

    default void onGetBattery(int nBatter){};   //获取电池电量
    default  void onGetBatteryInfo(byte[] batteryInfo){};  //获取电池信息，比onGetBattery更详细，不然是否在充电，充电是否完成。。。 不是所有固件支持
    default  void onRevFiles(GetFiles files){};  //获取SD卡文件列表huidiao


    default void onDownloadFile(jh_dowload_callback jh_dowload_callback){};  //下载SD卡文件时的回调
    default void onGetThumb(MyThumb thumb){}; //获取SD开文件的缩略图
    default  void onGetSDFleThumbnail_fail(MyFile file){};   //获取SD开文件的缩略图失败回调
    default void OnStartOnLinePlayStatus(byte nStatus){};  //0 OK  1 文件错误 2 当前模式不支持  4 忙，正在播放

    /*                PLAY_STATUS
                1 byte =  0:空闲，1: 正在播放， 2:播放暂停，3:文件 异常无法播放，4:文件不存在
                播放速度   1byte U8 速度 x10, 例如 1.5 倍对应 15
                文件时长   4Byte U32 单位秒
                已播放时长 4Byte U32 单位秒
*/
    default void OnGetOnLinePlayStatus(OnLinePlayStatus onLinePlayStatus){};




    default  void onDeleteFile(MyFile file){};   //删除SD文件回调 这里，文件长度为0 表示删除失败？为1 表示删除OK
    default  void onDeleteAllFile(int nStatus){};
    default  void onGetDeviceDateTime(byte []buffer){}; //获取设备时间


    //bit0-3: 0 off 1 on, bit4-7:  0 Y/M/D  1: D/M/Y 2 M/D/Y
    default void onGetDeviceWatermark(int nStatus){} ; //获取设备的水印设定

    default void onGetDeviceReversalStatus(int n){};  //获取设备图上是否翻转设定
    default void onGetDeviceRecordTime(int nSec){};   //获取设备正在录像时的录像时长
    default void onGetWiFiSSID(String ssid){};
    default void  onGetWifiChannel(int nChannel){};
    default void  onFormatSD_Status(int nStatus){}; //格式SD时返回的状态
    default  void onGetFirmwareVersion(String sVer){}; //获取固件版本
    default  void  onGetResolution(int n){}; //获取设备的摄像头分辨率
    default  void  onGetKey(byte[] keydata){}; //获取按键
    default  void onGetKey(int nKey){};  // 获取按键
    default  void onSensorTemperatureAlarm(byte[] data){};  //温度报警
    default  void onGetCameraPara(byte[] para){}; //获取摄像头参数，比如EV，对比度。。。 具体参考协议



    //PCM  实时传输 信息，是否支持等
//                    AUDIO_EN 传输控制    1开始传输 0停止传输
//                    AUDIO_STATUS 传输状态   0:声音未开启， 1:声音已开启 2:不支持声音传输功能
//                    AUDIO_ENCODE         Bit0~3= 0:16bit PCM， 1:8bit alaw Bit4~7= 0: 8K, 1:16K, 2:44.1K,3:32K
    default  void  onGetPcmInfo(byte[] data){};
    default  void onGetTftOrientation(int nStatus){};  //获取估计TFT屏幕是否横屏竖屏显示设置。


    default  void  onPlayDuration(long nSec){};  //内置播放器播放视频的总时长
    default  void  onPlayStatus(int nStatus){};  //内置播放器播放视频d状态
    default  void  onPlayTime(long nSec){};  //内置播放器播放视频已经播放的时长

    default void onSnapOrRecord(String Sn){};   //拍照或者录像完成。可以把它加入到系统图库中去


    default  void onConvertEvent(int n){};   //转换视频格式时的回调
    default   void onChangeBWMode(boolean b){};
}
