package com.joyhonest.wifination;

import android.util.Log;

public class AudioCodecExt {
   private byte[] mData;
   private int    mCount;
   private final int SIZE = 2048*20;
   public AudioCodecExt()
   {
      mData = new byte[SIZE];
      mCount = 0;
   }
   public int WriteData(byte []data)
   {
        int nLen = data.length;
        int nRe = nLen;
          synchronized (mData) {
              if(mCount+nLen<=SIZE) {
                  System.arraycopy(data, 0, mData, mCount, nLen);
                  mCount+=nLen;
              }
              else
              {
                  mCount=0;
                  nRe = 0;
              }
          }
       return nRe;
   }
   public byte[] ReadData(int nLen)
   {
       synchronized (mData)
       {
           byte[] reArray = null;
           if(nLen<=mCount)
           {
               reArray = new byte[nLen];
               System.arraycopy(mData,0,reArray,0,nLen);
               mCount-=nLen;
               System.arraycopy(mData,nLen,mData,0,mCount);
               Log.e("abcde","read extdata");
           }
           return reArray;
       }
   }
}

