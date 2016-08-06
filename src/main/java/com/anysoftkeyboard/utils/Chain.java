package com.anysoftkeyboard.utils;

/**
 * modifying library chain class
 * can be done by:
 *  overriding methods
 *  adding methods
 */
public class Chain extends components.Chain {

    public Chain(int window, int token, int threshold, int model_size) {
        super(window, token, threshold, model_size);
    }

    public Chain(components.Chain c) {
        super(c);
    }

    /**
     * log.txt should be the file used by logcat
     * this means anything written to this file
     * should be displayed when adb logcat is being utilized
     */
    public void outputProb (){
        String file_name = "/sdcard/log.txt";

        //TODO uncomment this
        //this.output_by_window(file_name);
    }
}
