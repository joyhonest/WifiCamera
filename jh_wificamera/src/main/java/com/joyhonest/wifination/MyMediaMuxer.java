package com.joyhonest.wifination;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
//import android.util.Log;

import java.nio.ByteBuffer;

public class MyMediaMuxer {

    private  static boolean  bStartWrite = false;
    private  static boolean bRecording=false;
    private  static MediaMuxer mediaMuxer=null;

    public  static  int audioInx=-1;
    public  static  int videoInx=-1;


    public  static MediaFormat formatV;
    public  static MediaFormat formatA;

    public  static   int fps = 25;


    public static  long  nCountFrame=0;

    public  static long   nFramesAudio=0;

    public static  int nCt = (2048 * 1000000) / (16000 * 2);


    public static long startTimeNanos; // 录制开始时的基准时间

    public  static int  init(String strNme)
    {
        int nResult = 0;
        bRecording = false;
        nResult = 1;
        try {
            bStartWrite = false;
            formatV = null;
            formatA = null;
            mediaMuxer = new MediaMuxer(strNme, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            startTimeNanos = System.nanoTime();
            // 记录基准时间

        }catch (Exception e)
        {
            mediaMuxer = null;
            nResult = -1;

        }
        if(mediaMuxer!=null)
        {
            nResult = 1;
        }
        return nResult;
    }

    // 计算当前时间戳（微秒）
    private static long getCurrentTimestampUs() {
        return (System.nanoTime() - startTimeNanos) / 1000;
    }


    static int WritSample(byte[] data, boolean bVideo,  MediaCodec.BufferInfo info)  //long ppp,
    {

        if(bVideo && videoInx<0)
            return  -1;
        if(!bVideo && audioInx<0)
            return  -2;

        if(!bRecording)
            return -3;
        if(!bStartWrite && bVideo && (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME)!=0)
        {
            bStartWrite = true;
        }
        if(!bStartWrite && bVideo) {
            return -4;
        }
        int re = -5;
        if(bVideo)
        {
            if (data != null && mediaMuxer != null)
            {
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0)
                {
                    try
                    {
                        mediaMuxer.writeSampleData(videoInx,ByteBuffer.wrap(data), info);
                      //  Log.e(""," video timeABC = "+info.presentationTimeUs/1000000.0f);
                        nCountFrame++;
                        re = 0;
                    }
                    catch (Exception ignored)
                    {

                    }
                }
                else
                {
                    re = 0;
                }
            }
        }
        else
        {
            if (data != null && mediaMuxer != null)
            {
                //if(audioInx>=0)
                {
                    try {
                        mediaMuxer.writeSampleData(audioInx, ByteBuffer.wrap(data), info);
                      //  Log.e(""," audio timeABC = "+info.presentationTimeUs/1000000.0f);
                        nFramesAudio++;
                        re = 0;
                    }
                    catch (Exception ignored)
                    {

                    }
                }
            }

        }
        return re;
    }


    static void AddVideoTrack(MediaFormat format)
    {
        Log.e("","Track VideoTrack!!!!!!");
        if(format!=null && mediaMuxer!=null)
        {
            try {
                if (videoInx < 0)
                {
                    videoInx = mediaMuxer.addTrack(format);
                }
            }
            catch (Exception e)
            {
                videoInx=-1;
            }
            if(videoInx>=0)
            {
                if(wifination.bG_Audio)
                {
                    if(audioInx>=0)
                    {
                        startMuxer();
                    }
                }
                else
                {
                    startMuxer();
                }
            }
        }
    }

    static void startMuxer()
    {
        bRecording = true;
        bStartWrite = false;
        MyMediaMuxer.nFramesAudio=0;
        MyMediaMuxer.nCountFrame=0;
        mediaMuxer.start();
    }

    static void AddAudioTrack(MediaFormat format)
    {
        Log.e("","Track AddAudioTrack!!!!!!");
        if(format!=null && mediaMuxer!=null)
        {
            try {
                if(audioInx<0) {
                    audioInx = mediaMuxer.addTrack(format);
                }
            }
            catch (Exception e)
            {
                audioInx=-1;
            }
            if(audioInx>=0)
            {
                if(videoInx>=0)
                {
                        startMuxer();
                }
            }
        }
    }



    public static void stop()
    {
         if(bRecording)
         {
             bRecording = false;
             SystemClock.sleep(100);
             try {
                 mediaMuxer.stop();
                 mediaMuxer.release();
             }
             catch (Exception e)
             {
                 ;
             }

             formatV = null;
             formatA = null;

             mediaMuxer=null;

             audioInx = -1;
             videoInx = -1;
         }
    }


}
