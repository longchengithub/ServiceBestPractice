package com.example.chenlong.servicebestpractice.intef;

/**
 * Created by ChenLong on 2017/1/26.
 */

public interface CustomDownloadListener
{

    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();
}
