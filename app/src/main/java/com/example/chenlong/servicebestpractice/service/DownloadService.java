package com.example.chenlong.servicebestpractice.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.example.chenlong.servicebestpractice.DownloadTask;
import com.example.chenlong.servicebestpractice.MainActivity;
import com.example.chenlong.servicebestpractice.R;
import com.example.chenlong.servicebestpractice.intef.CustomDownloadListener;

import java.io.File;

public class DownloadService extends Service
{

    private DownloadTask downloadTask;
    private String downloadUrl;
    private CustomDownloadListener mListener = new CustomDownloadListener()
    {
        @Override
        public void onProgress(int progress)
        {
            //刷新进度
            getNotificationManager().notify(1, getNotification("正在下载中", progress));
        }

        @Override
        public void onSuccess()
        {
            downloadTask = null;  //下载成功后,清除下载任务
            stopForeground(true);
            //刷新通知
            getNotificationManager().notify(1, getNotification("下载成功", -1));
            Toast.makeText(DownloadService.this, "下载完成!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed()
        {
            downloadTask=null;
            stopForeground(true);
            //刷新进度
            getNotificationManager().notify(1, getNotification("下载失败!", -1));
            Toast.makeText(DownloadService.this, "下载失败!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused()
        {
            downloadTask=null;
            Toast.makeText(DownloadService.this, "已暂停下载", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCanceled()
        {
            downloadTask=null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "已取消下载", Toast.LENGTH_SHORT).show();
        }
    };

    public DownloadService()
    {
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return new DownloadBinder();
    }

    /**
     * 自定义返回的Binder方法
     * 有开启下载 暂停下载 跟取消下载方法
     */
    public class DownloadBinder extends Binder
    {
        /**
         * 开启下载
         *
         * @param url
         */
        public void startDownload(String url)
        {
            if (downloadTask == null)
            {
                downloadUrl = url;  //拿到路径下载
                downloadTask = new DownloadTask(mListener); //创建对象
                downloadTask.execute(downloadUrl);  //执行下载

                startForeground(1, getNotification("正在下载文件中", 0));
                Toast.makeText(DownloadService.this, "已开启下载", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 暂停下载
         */
        public void pauseDownload()
        {
            if (downloadTask != null)
            {
                downloadTask.pauseDownload();
            }
        }

        /**
         * 取消下载
         */
        public void cancelDownload()
        {
            if (downloadTask != null)
            {
                downloadTask.cancelDownload();
            } else
            {
                if (downloadUrl != null)
                {
                    //取消下载 就是删除源文件 然后取消通知栏
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

                    File file = new File(directory + fileName); //找到源文件
                    if (file.exists())
                    {
                        file.delete();
                    }

                    getNotificationManager().cancel(1); //取消通知栏
                    stopForeground(true);
                    Toast.makeText(DownloadService.this, "下载任务已取消", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 返回一个通知管理者
     *
     * @return
     */
    private NotificationManager getNotificationManager()
    {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * 创建一个通知
     *
     * @param title
     * @param progress
     * @return
     */
    private Notification getNotification(String title, int progress)
    {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setContentTitle(title);

        //有进度大于0才显示
        if (progress > 0)
        {
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }
}
