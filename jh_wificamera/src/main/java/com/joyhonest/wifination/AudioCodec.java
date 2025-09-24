
package com.joyhonest.wifination;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

public interface AudioCodec {
    String MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC;
    int KEY_BIT_RATE = 64000; //KEY_SAMPLE_RATE * KEY_CHANNEL_COUNT * 16;
    int KEY_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

}
