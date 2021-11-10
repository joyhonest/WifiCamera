package com.joyhonest.wifination;


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
    private Worker mWorker;
    private final String TAG = "AudioEncoder";
    private byte[] mFrameByte;

    public MediaFormat mediaFormat;

    public AudioEncoder() {
        //  mClient=client;
    }


    public boolean  isCanRecordAudio()
    {
        Worker p = new Worker();
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
        if (mWorker == null)
        {
            mWorker = new Worker();
            boolean re = mWorker.prepare();
            if(re)
            {
                mWorker.setRunning(true);
                mWorker.start();
            }
            return re;
        }
        return false;
    }

    public void stop() {
        if (mWorker != null) {
            mWorker.setRunning(false);
            mWorker = null;
        }
    }


    private class Worker extends Thread {
        private int mFrameSize = 2048;
        private byte[] mBuffer;
        private boolean isRunning = false;
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
                re = mRecord.read(mBuffer, 0, mFrameSize);
                encode(mBuffer);
            }
            release();
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
            catch (Exception e)
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
            try {
                MyMediaMuxer.nFramesAudio=0;



                mBufferInfo = new MediaCodec.BufferInfo();
                mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
                mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE, KEY_SAMPLE_RATE, KEY_CHANNEL_COUNT);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 32000);
                mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, KEY_AAC_PROFILE);
                mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mEncoder.start();
                re = true;
            } catch (Exception  e) {
                e.printStackTrace();
                re =false;
            }
            if(!re)
            {
                return re;
            }
            re = false;
            try {

                int minBufferSize = AudioRecord.getMinBufferSize(KEY_SAMPLE_RATE, CHANNEL_MODE,AUDIO_FORMAT);
                mRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT, minBufferSize);
                int buffSize = Math.min(BUFFFER_SIZE, minBufferSize);
                mFrameSize = buffSize;
                MyMediaMuxer.nCt = (mFrameSize * 1000000) / (KEY_SAMPLE_RATE * 2 * KEY_CHANNEL_COUNT);
                mBuffer = new byte[mFrameSize];
                pts_unit = (long) ((((float)mFrameSize)/(KEY_BIT_RATE/8))*1000000);
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
