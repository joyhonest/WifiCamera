package com.joyhonest.wifination;

import android.graphics.Bitmap;

public interface PlayerInterface {
    void Playduration(int i);
    void PlayStatus(int nStatyus);
    void PlayTime(int da);

   // void PlayDispImage(byte[] data,int w,int h);
   void PlayDispImage(Bitmap bitmap);



}
