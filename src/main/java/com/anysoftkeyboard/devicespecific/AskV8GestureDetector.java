/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.devicespecific;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.anysoftkeyboard.AnySoftKeyboard;
import com.anysoftkeyboard.IndirectlyInstantiated;
import com.anysoftkeyboard.utils.Log;

import java.io.File;
import java.io.FileOutputStream;


@TargetApi(8)
@IndirectlyInstantiated
public class AskV8GestureDetector extends GestureDetector {
    private static final int NOT_A_POINTER_ID = -1;
    private File dir;
    private final String accountName = "Ridwan";
    private static String fileName, TAG = "Async",line,line1;
    private Runnable runnable;
    private Handler handler;
    private File f;
    private Context t;
    private  FileOutputStream fos;
    private MotionEvent v;
    protected final ScaleGestureDetector mScaleGestureDetector;
    private final AskOnGestureListener mListener;
    public static float pValue = 0;
    public static float Value0 ;
    public static float Value1 ;
    public static float time;
    private BigComputationTask T;


    private int mSingleFingerEventPointerId = NOT_A_POINTER_ID;

    public AskV8GestureDetector(Context context, AskOnGestureListener listener) {
        super(context, listener, null, true/*ignore multi-touch*/);
        T = new BigComputationTask();
        mListener = listener;

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                final float factor = detector.getScaleFactor();
                if (factor > 1.1)
                    return mListener.onSeparate(factor);
                else if (factor < 0.9)
                    return mListener.onPinch(factor);

                return false;
            }
        });
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {

        int singleFingerEventPointerId = mSingleFingerEventPointerId;

        //I want to keep track on the first finger (https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/300)
        switch (MotionEventCompat.getActionMasked(ev)){
            case MotionEvent.ACTION_DOWN:

                /**
                 * isu_research
                 */
                //logTouchEvent(ev.getX(), ev.getY(), ev.getEventTime(), ev.getPressure());
                //logKeyEvent(AnySoftKeyboard.keyCode);
                //T.execute();
                pValue = ev.getPressure();
                Value0 = ev.getDownTime();

                if (ev.getPointerCount() == 1) {
                    mSingleFingerEventPointerId = ev.getPointerId(0);
                    singleFingerEventPointerId = mSingleFingerEventPointerId;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Value1 = ev.getEventTime();
                time = Value1 - Value0;
                if (ev.getPointerCount() == 1)
                    mSingleFingerEventPointerId = NOT_A_POINTER_ID;

        }
        try {
            //https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/26
            mScaleGestureDetector.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            //I have nothing I can do here.
        } catch (ArrayIndexOutOfBoundsException e) {
            //I have nothing I can do here.
        }
        //I'm going to pass the event to the super, only if it is a single touch, and the event is for the first finger
        //https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/300
        if (ev.getPointerCount() == 1 && ev.getPointerId(0) == singleFingerEventPointerId)
            return super.onTouchEvent(ev);
        else
            return false;
    }



    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void logTouchEvent(final float x, final float y, final long eventTime, final float pressure) {

         line = Long.toString(eventTime) + ',' + Float.toString(x) + ',' + Float.toString(y) + ',' + Float.toString(pressure) + '\n';
        Log.d("Keyboard Touch", line);
      //  Log.d("val","values %s",line);

//    try {
//        FileWriter writer = new FileWriter(f, true);
//        writer.append(line);
//        writer.flush();
//        writer.close();
//    } catch(IOException e) {
//        e.printStackTrace();
//    }
}

    // Android PUF Security Research
    private void logKeyEvent(final int keyCode) {
//        dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "PUF");
//        if(!dir.exists()) {
//            dir.mkdirs();
//        }
//        fileName = accountName + "_" + Build.SERIAL + "keyCodes.csv";
//        f = new File(dir, fileName);
//        if(!f.exists()) {
//            try {
//                f.createNewFile();
//            } catch(IOException e) {
//                e.printStackTrace();
//            }
//        }
        line1 = Integer.toString(keyCode) + '\n';
        Log.d("Keyboard Code", line1);
     //   Log.d("val","value %s",line1);
//        try {
//            FileWriter writer = new FileWriter(f, true);
//            writer.append(line);
//            writer.flush();
//            writer.close();
//        } catch(IOException e) {
//            e.printStackTrace();
//        }
    }

    private class BigComputationTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            // Runs on UI thread
            Log.d(TAG, "About to start...");
        }

        @Override
        protected Void doInBackground(Void... params) {

            AnySoftKeyboard.compute();
            return null;
        }


        @Override
        protected void onPostExecute(Void res) {
            // Runs on the UI thread
            Log.d(TAG, "Big computation finished");

        }

    }
}

