package com.example.test;

import com.example.myfirstapp.MainActivity;

public class InstrumentedActivity extends MainActivity {
    public static String TAG = "IntrumentedActivity";
    private com.example.test.FinishListener mListener;
    public void setFinishListener(com.example.test.FinishListener listener) {
        mListener = listener;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        super.finish();
        if (mListener != null) {
            mListener.onActivityFinished();
        }
    }
}
