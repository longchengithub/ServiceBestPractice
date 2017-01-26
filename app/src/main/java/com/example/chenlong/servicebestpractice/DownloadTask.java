package com.example.chenlong.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import com.example.chenlong.servicebestpractice.intef.CustomDownloadListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by ChenLong on 2017/1/26.
 */

/**
 * 3个参数 这里参数1表示接收的类型 因为是联网操作 所以定为String的字符串
 * 参数2表示进度条的回调 一般使用integer就行
 * 参数3表示反馈结果  这里定义了4个状态 成功 失败 暂停 取消 所以也用integer
 */
public class DownloadTask extends AsyncTask<String, Integer, Integer>
{

    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private boolean isCanceled; //是否取消下载
    private boolean isPaused;   //是否暂停
    private int lastProgress;   //上次下载的进度

    private CustomDownloadListener mListener;     //接口回调

    public DownloadTask(CustomDownloadListener mListener)
    {
        this.mListener = mListener;
    }

    /**
     * 后台线程的具体操作
     *
     * @param params
     * @return
     */
    @Override
    protected Integer doInBackground(String... params)
    {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try
        {
            long downloadedLength = 0;        //初始化下载进度开始为0
            String downloadUrl = params[0];
            //截取路径最后一个/后的名字 作为文件名
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

            //拼凑出下载文件的最终路径
            file = new File(directory + fileName);
            if (file.exists())
            {
                downloadedLength = file.length();     //重新指定已下载进度 如果是第一次肯定是0 进不来判断.
            }

            long contentLength = getContentLength(downloadUrl);   //先发一次请求获取文件的总大小

            //为0表示 第一次请求失败了
            if (contentLength == 0)
            {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength)
            {   //下载的大小与文件大小相同 说明下载成功
                return TYPE_SUCCESS;
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    //指定断点续传下载
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null)
            {
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");  //随机流包装
                savedFile.seek(downloadedLength);           //断点续传到上次下载的断点
                byte[] b = new byte[1024];
                int len;

                int total = 0;
                while ((len = is.read(b)) != -1)
                {
                    if (isCanceled)
                    {
                        return TYPE_CANCELED;
                    } else if (isPaused)
                    {
                        return TYPE_PAUSED;
                    } else
                    {
                        savedFile.write(b, 0, len);

                        total += len;       //计算出百分比进度 然后返回
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        publishProgress(progress);
                    }
                }

                //走到这一步 表示下载完毕 所以关闭请求
                response.body().close();
                return TYPE_SUCCESS;
            }

        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                if (is != null)
                {
                    is.close();
                }
                if (savedFile != null)
                {
                    savedFile.close();
                }
                if (isCanceled && file != null)
                {
                    file.delete();
                }

            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    /**
     * 获取文件的大小
     *
     * @param downloadUrl
     * @return
     */
    private long getContentLength(String downloadUrl) throws IOException
    {
        long contentLength = 0;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful())
        {
            contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }

        return contentLength;
    }

    /**
     * doInBackground 执行后的回调
     *
     * @param integer
     */
    @Override
    protected void onPostExecute(Integer integer)
    {
        switch (integer)
        {
            case TYPE_SUCCESS:
                mListener.onSuccess();
                break;
            case TYPE_FAILED:
                mListener.onFailed();
                break;
            case TYPE_PAUSED:
                mListener.onPaused();
                break;
            case TYPE_CANCELED:
                mListener.onCanceled();
                break;
        }
    }

    /**
     * 进度条回调
     *
     * @param values
     */
    @Override
    protected void onProgressUpdate(Integer... values)
    {
        int progress = values[0];
        if (progress > lastProgress)
        {
            mListener.onProgress(progress); //接口回调返回的进度
            lastProgress = progress;        //刷新最后的进度
        }
    }

    /**
     * 对外提供的暂停下载的方法
     */
    public void pauseDownload()
    {
        isPaused = true;
    }

    /**
     * 对外提供的取消下载的方法
     */
    public void cancelDownload()
    {
        isCanceled = true;
    }
}
