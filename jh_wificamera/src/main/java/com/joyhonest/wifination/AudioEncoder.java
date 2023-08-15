package com.joyhonest.wifination;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.WeakHashMap;


public class AudioEncoder implements AudioCodec {
    //private Client mClient;


    public int nRecType=0;
    int KEY_CHANNEL_COUNT = 1;
    int KEY_SAMPLE_RATE = 8000;

    int CHANNEL_MODE = AudioFormat.CHANNEL_IN_MONO;// (KEY_CHANNEL_COUNT ==1? AudioFormat.CHANNEL_IN_MONO:AudioFormat.CHANNEL_IN_STEREO);
    int BUFFFER_SIZE = 1024*2;

    private Worker mWorker;
    private final String TAG = "AudioEncoder";
    private byte[] mFrameByte;

    public MediaFormat mediaFormat;

    public AudioEncoder() {
        //  mClient=client;
        nRecType = 0;
    }

    public void SetDataExt(boolean b)
    {
        if(b) {
            nRecType = 1;
            KEY_CHANNEL_COUNT = 2;
            CHANNEL_MODE = AudioFormat.CHANNEL_IN_STEREO;
            KEY_SAMPLE_RATE = 8000;

        }
        else {
            nRecType = 0;
//            KEY_CHANNEL_COUNT = 1;
//            CHANNEL_MODE = AudioFormat.CHANNEL_IN_MONO;
//            KEY_SAMPLE_RATE = 44100;
            KEY_CHANNEL_COUNT = 2;
            CHANNEL_MODE = AudioFormat.CHANNEL_IN_STEREO;
            KEY_SAMPLE_RATE = 8000;

        }

    }

    public void WriteExtData(byte[] data)
    {
        if(mWorker!=null)
        {
            mWorker.WriteExtData(data);
        }
    }

    public boolean  isCanRecordAudio()
    {
        Worker p = new Worker();
        p.nRecType = nRecType;
        return p.isCanRecordAudio();
    }


    public boolean  start() {
        if(mWorker !=null)
        {
            mWorker.setRunning(false);
            try {
                mWorker.join(100);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            mWorker = null;
        }
        mWorker = new Worker();
        mWorker.nRecType = nRecType;
        boolean re = mWorker.prepare();
        if(re)
        {
            mWorker.setRunning(true);
            mWorker.start();
        }
        return re;
    }

    public void stop() {
        if (mWorker != null) {
            mWorker.setRunning(false);
            mWorker = null;
        }
    }


    private class Worker extends Thread {
        private int mFrameSize = 2048*2;
        private byte[] mBuffer;
        private boolean isRunning = false;
        public  int nRecType = 0; //0 用mic录音， ！=0 从外部灌数据进来
        private long pts_unit=0;
        private MediaCodec mEncoder;
        private AudioRecord mRecord;
        MediaCodec.BufferInfo mBufferInfo;

        long pts;

        boolean bStart = false;
        @Override
        public void run() {
            int re = 0;
            bStart = false;
            pts=0;
            while (isRunning) {
                if(nRecType==0) {
                    re = mRecord.read(mBuffer, 0, mFrameSize);
                    encode(mBuffer);
                }
                else
                {
                    byte[] rea = wifination.audioCodecExt.ReadData(mFrameSize);
                    encode(rea);
                }
            }
            release();
        }

        public void WriteExtData(byte[] data)
        {
            if(data != null)
            {
                encode(data);
            }
        }

        public void setRunning(boolean run) {
            isRunning = run;
        }

        /**
         * 释放资源
         */
        private void release() {
            if (mEncoder != null) {
                mEncoder.stop();
                mEncoder.release();
            }
            if (mRecord != null) {
                mRecord.stop();
                mRecord.release();
                mRecord = null;
            }
        }


        public    boolean isCanRecordAudio()
        {
            if(nRecType!=0)
                return true;

            boolean re = false;
            try {
                int minBufferSize = AudioRecord.getMinBufferSize(KEY_SAMPLE_RATE, CHANNEL_MODE,AUDIO_FORMAT);
                mRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT, minBufferSize);
                mRecord.startRecording();
                mRecord.stop();
                mRecord.release();
                mRecord = null;
                re = true;
            }
            catch (Exception ignored)
            {
                ;
            }

            return re;
        }


        /**
         * 连接服务端，编码器配置
         *
         * @return true配置成功，false配置失败
         */
        public boolean prepare() {
            boolean re = false;
            int a1 = 8;
            try {
                MyMediaMuxer.nFramesAudio=0;


                mBufferInfo = new MediaCodec.BufferInfo();
                mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
                mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE, KEY_SAMPLE_RATE, KEY_CHANNEL_COUNT);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE);
                mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, KEY_AAC_PROFILE);
                mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mEncoder.start();
                re = true;
            } catch (Exception  e) {
                e.printStackTrace();
                mEncoder = null;
                re =false;
            }
            if(nRecType!=0)     //如果是从外部灌数据，就无需mRecord;
            {
                mFrameSize = 2048;
                MyMediaMuxer.nCt = (mFrameSize * 1000000) / (KEY_SAMPLE_RATE * 2);
                pts_unit = (long) (mFrameSize * 1000000L) / (KEY_SAMPLE_RATE * 2L);
                return re;
            }
            if(!re)
            {
                return re;
            }
            re = false;
            try {

                int minBufferSize = AudioRecord.getMinBufferSize(KEY_SAMPLE_RATE, CHANNEL_MODE,AUDIO_FORMAT);
                mRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT, minBufferSize);
                //int buffSize = Math.min(BUFFFER_SIZE, minBufferSize);
                mFrameSize = 2048;
                MyMediaMuxer.nCt = (mFrameSize * 1000000) / (KEY_SAMPLE_RATE * 2);
                pts_unit = (long) (mFrameSize * 1000000L) / (KEY_SAMPLE_RATE * 2L);
                mBuffer = new byte[mFrameSize];
                mRecord.startRecording();
                re = true;
            }
            catch (Exception e)
            {
                mEncoder.stop();
                mEncoder.release();
                mRecord = null;
                mEncoder = null;
                re = false;
            }
            return re;
        }




        private void encode(byte[] data) {
            if(data==null)
                return;
            long ppp = 0;

            int inputBufferId = mEncoder.dequeueInputBuffer(1000 * 50);
            if (inputBufferId >= 0) {

                ByteBuffer bb = mEncoder.getInputBuffer(inputBufferId);// inputBuffers[inputBufferId];
                bb.put(data, 0, data.length);
                ppp = pts*pts_unit;
                pts++;
                mEncoder.queueInputBuffer(inputBufferId, 0, data.length, ppp, 0);
            }

            MediaCodec.BufferInfo aBufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mEncoder.dequeueOutputBuffer(aBufferInfo, 1000 * 10);

            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mEncoder.getOutputFormat();
                MyMediaMuxer.AddAudioTrack(newFormat);
                Log.e(TAG,"add audio track");
            }

            if (outputBufferIndex >= 0) {  //编码器有可能一次性突出多条数据 所以使用while
                // outputBuffers[outputBufferId] is ready to be processed or rendered.
                ByteBuffer bb =mEncoder.getOutputBuffer(outputBufferIndex);//  outputBuffers[outputBufferIndex];
                bb.rewind();
                byte[] dataA = new byte[aBufferInfo.size];
                bb.get(dataA, 0, dataA.length);
                MyMediaMuxer.WritSample(dataA,false,aBufferInfo);
                //Log.e("abcdefg","write a audio");
                mEncoder.releaseOutputBuffer(outputBufferIndex, false);
            }
        }

        /**
         * 给编码出的aac裸流添加adts头字段
         *
         * @param packet    要空出前7个字节，否则会搞乱数据
         * @param packetLen
         */

        private void addADTStoPacket(byte[] packet, int packetLen) {
            int profile = 2;  //AAC LC
            int freqIdx = 4;  //44.1KHz
            int chanCfg = 2;  //CPE
            packet[0] = (byte) 0xFF;
            packet[1] = (byte) 0xF9;
            packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
            packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
            packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
            packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
            packet[6] = (byte) 0xFC;
        }
    }


    private static native boolean naSentVoiceData(byte[] data, int nLen);

}
