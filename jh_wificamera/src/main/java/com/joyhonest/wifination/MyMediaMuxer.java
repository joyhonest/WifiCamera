package com.joyhonest.wifination;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.SystemClock;
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


    public  static int  init(String strNme)
    {


        int nResult = 0;
        if(bRecording)
        {

        }

        bRecording = false;
        nResult = 1;
        try {
            bStartWrite = false;
            formatV = null;
            formatA = null;
            mediaMuxer = new MediaMuxer(strNme, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);


        }catch (Exception e)
        {
            mediaMuxer = null;
            nResult = -1;
            e.printStackTrace();
        }
        if(mediaMuxer!=null)
        {
            nResult = 1;
        }
        return nResult;
    }


    static int WritSample(byte[] data, boolean bVideo,  MediaCodec.BufferInfo info)  //long ppp,
    {
        if(!bRecording)
            return -1;
        if(!bStartWrite && bVideo && (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME)!=0)
        {
            bStartWrite = true;
        }
        if(!bStartWrite)
            return -2;
        int re = -1;
        if(bVideo)
        {
            if (data != null && mediaMuxer != null)
            {

                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0)
                {
                    try {
                        long  us =nCountFrame*1000000/fps ;
                        info.presentationTimeUs = us;
                        mediaMuxer.writeSampleData(videoInx,ByteBuffer.wrap(data), info);
                        nCountFrame++;
                        re = 0;
                    } catch (Exception e) {
                        e.printStackTrace();
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
                if(audioInx>=0)
                {
                    try {
                        long usA = nFramesAudio*nCt;
                        info.presentationTimeUs = usA;
                        mediaMuxer.writeSampleData(audioInx, ByteBuffer.wrap(data), info);
                        nFramesAudio++;
                        re = 0;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }

        }
        return re;
    }


    static void AddVideoTrack(MediaFormat format)
    {
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
                e.printStackTrace();
            }
            if(videoInx>=0)
            {
                if(wifination.bG_Audio)
                {
                    if(audioInx>=0)
                    {
                        bRecording = true;
                        bStartWrite = false;
                        MyMediaMuxer.nFramesAudio=0;
                        MyMediaMuxer.nCountFrame=0;
                        mediaMuxer.start();
                       // Log.e("media","Start 111");
                    }
                }
                else
                {
                    try {
                        bRecording = true;
                        bStartWrite = false;
                        MyMediaMuxer.nFramesAudio=0;
                        MyMediaMuxer.nCountFrame=0;
                        mediaMuxer.start();
                        //Log.e("media","Start 222");
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    static void AddAudioTrack(MediaFormat format)
    {
        if(format!=null && mediaMuxer!=null)
        {
            try {
                if(audioInx<0) {
                    audioInx = mediaMuxer.addTrack(format);
                }
            }catch (Exception e)
            {
                audioInx=-1;
                e.printStackTrace();
            }
            if(audioInx>=0)
            {
                    if(videoInx>=0)
                    {
                        bRecording = true;
                        bStartWrite = false;
                        MyMediaMuxer.nFramesAudio=0;
                        MyMediaMuxer.nCountFrame=0;
                        mediaMuxer.start();
                      //  Log.e("media","Start 333");
                    }
                    else
                    {
                       // Log.e("media","Start 444");
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
                 e.printStackTrace();
             }

             formatV = null;
             formatA = null;

             mediaMuxer=null;

             audioInx = -1;
             videoInx = -1;
         }
    }


}
