package com.joyhonest.wifination;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
//import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

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


    public static long startTimeNanos = 0; // 录制开始时的基准时间
    public static Context context = null;
    public static ParcelFileDescriptor pfd=null;
    static ContentResolver resolver =null;
    static Uri uri=null;

    public static void F_CloseFD()
    {
        if(pfd!=null)
        {
            try {
                pfd.close();
            }
            catch (IOException ignored)
            {

            }
            finally {
                pfd = null;
            }
            if(uri!=null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues update = new ContentValues();
                    update.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(uri, update, null, null);
                }
                uri = null;
            }

        }

    }

    public static String getFileNameWithoutExtension(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return "";
        }

        // 1. 提取最后一段（兼容 / 和 \）
        int lastSeparatorIndex = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
        String fileName = (lastSeparatorIndex == -1) ? fullPath : fullPath.substring(lastSeparatorIndex + 1);

        // 2. 去掉扩展名（最后一个点之后的部分）
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) { // 避免隐藏文件（如 .bashrc）被误删
            fileName = fileName.substring(0, lastDotIndex);
        }

        return fileName;
    }
    public static int F_getFd(String sFilename,String sAlam)
    {
        if(context==null)
            return  -1;
        String stype = sFilename.substring(sFilename.lastIndexOf(".") + 1);
        resolver = context.getContentResolver();
        Path pa = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            pa = Paths.get(sFilename);
//        }
//        String strNme = pa.getFileName().toString();
         String strNme = getFileNameWithoutExtension(sFilename);


        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, strNme);
        if (stype.equalsIgnoreCase("png")) {
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        } else {
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        }


        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (sAlam == null) {
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM);
                } else {
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/" + sAlam);
                }

                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }
        }
        catch (Exception ignored)
        {
            uri = null;
        }



        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if(uri!=null)
                pfd = resolver.openFileDescriptor(uri, "w");

            if (pfd != null) {
                return pfd.getFd();
            }
        }
        catch (Exception ignored)
        {
                ;
        }
        return   -1;

    }

    private static Queue<MuxerData> videoCache = new LinkedList<>();
    private static Queue<MuxerData> audioCache = new LinkedList<>();

    public  static int  init(String strNmeA,String sAlam)
    {
        int nResult = 0;
        videoCache.clear();
        audioCache.clear();

        File file = new File(strNmeA);
        if(file.exists() && file.isFile())
        {
            file.delete();
        }

        String strNme = Paths.get(strNmeA).getFileName().toString();
        if(sAlam==null)
            strNme = strNmeA;

        bRecording = false;
        nResult = 1;
        try {
            bStartWrite = false;
            formatV = null;
            formatA = null;
            startTimeNanos = System.nanoTime();
            if(sAlam == null) {
                uri = null;
                resolver = null;
                pfd = null;
                mediaMuxer = new MediaMuxer(strNme, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            }
            else {

                /// //////

                //    mediaMuxer = new MediaMuxer(strNme, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                resolver = context.getContentResolver();

                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, strNme);
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/" + sAlam);

                    values.put(MediaStore.Video.Media.IS_PENDING, 1);
                }

                uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                pfd = resolver.openFileDescriptor(uri, "w");

                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    mediaMuxer = new MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                }
            }



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


    private static class MuxerData {
        byte[] data;      // 深拷贝的数据
        MediaCodec.BufferInfo info;

        MuxerData(byte[] src, MediaCodec.BufferInfo info) {
            this.data = new byte[src.length];
            System.arraycopy(src,0,this.data,0,src.length);
            this.info = new MediaCodec.BufferInfo();
            this.info.set(0, info.size, info.presentationTimeUs, info.flags);
        }
    }

    static int nCountV = 0;
    static int nCountH = 0;
    static int WritSample(byte[] data, boolean bVideo,  MediaCodec.BufferInfo info)  //long ppp,
    {
        int re = -5;
        if(bVideo && videoInx<0)
            return  -1;
        if(!bVideo && audioInx<0)
            return  -2;

        if(!bRecording)
        {
            MuxerData dataA = new MuxerData(data,info);
            if(bVideo)
            {
                if(videoInx>=0) {
                    videoCache.add(dataA);

                    nCountV++;
                }
            }
            else
            {
                if(audioInx>=0)
                    audioCache.add(dataA);
            }
        }

        if(videoInx>= 0)
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

        if(bRecording) {
            if (videoInx >= 0 && bVideo) {
                for (MuxerData da : videoCache) {
                    if ((da.info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        try {

                            mediaMuxer.writeSampleData(videoInx, ByteBuffer.wrap(da.data), da.info);
                            nCountFrame++;

                            re = 0;
                        } catch (Exception ignored) {

                        }
                    } else {
                        re = 0;
                    }
                }
                videoCache.clear();

                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0)
                {
                    try {

                        mediaMuxer.writeSampleData(videoInx, ByteBuffer.wrap(data), info);
                        nCountFrame++;
                        re = 0;
                    } catch (Exception ignored) {

                    }
                } else {
                    re = 0;
                }

            }

            if (audioInx >= 0 && !bVideo) {
                for (MuxerData da : audioCache) {
                    try {
                       // Log.e(" "," audioA =  "+da.info.presentationTimeUs);
                        mediaMuxer.writeSampleData(audioInx, ByteBuffer.wrap(da.data), da.info);
                        nFramesAudio++;
                        re = 0;
                    } catch (Exception ignored) {

                    }
                }
                audioCache.clear();

                try {
                    //Log.e(" "," audio =  "+info.presentationTimeUs);
                    mediaMuxer.writeSampleData(audioInx, ByteBuffer.wrap(data), info);
                    nFramesAudio++;
                    re = 0;
                } catch (Exception ignored) {

                }
            }
        }
/*
        if(!bStartWrite && bVideo && (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME)!=0)
        {
            bStartWrite = true;
        }
        if(!bStartWrite && bVideo) {
            return -4;
        }

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

 */
        return re;
    }




    // 缓存队列：存放未开始 muxer 前的编码数据

    static void startMuxer()
    {
        if(!bRecording)
        {
            bRecording = true;
            bStartWrite = false;
            MyMediaMuxer.nFramesAudio = 0;
            MyMediaMuxer.nCountFrame = 0;
            mediaMuxer.start();
        }
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
//            if(videoInx>=0)
//            {
//                if(wifination.bG_Audio)
//                {
//                    if(audioInx>=0)
//                    {
//                        startMuxer();
//                    }
//                }
//                else
//                {
//                    startMuxer();
//                }
//            }
        }
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
//            if(audioInx>=0)
//            {
//                if(videoInx>=0)
//                {
//                    startMuxer();
//                }
//            }
        }
    }



    public static void stop()
    {
         if(bRecording)
         {
             bRecording = false;
             SystemClock.sleep(100);
             if(pfd!=null)
             {
                 try {
                     pfd.close();
                 }
                 catch (IOException ignored)
                 {

                 }
                 finally {
                     pfd = null;
                 }
             }
             try {
                 mediaMuxer.stop();
                 mediaMuxer.release();

             }
             catch (Exception e)
             {
                 ;
             }
             finally {
                 mediaMuxer = null;
             }

             formatV = null;
             formatA = null;

             mediaMuxer=null;

             audioInx = -1;
             videoInx = -1;
             if(uri!=null) {
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                     ContentValues update = new ContentValues();
                     update.put(MediaStore.Video.Media.IS_PENDING, 0);
                     resolver.update(uri, update, null, null);
                 }
             }
         }
    }


}
