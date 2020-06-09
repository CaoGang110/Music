package com.sunday.imoocmusicdemo.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.sunday.imoocmusicdemo.R;
import com.sunday.imoocmusicdemo.activitys.WelcomeActivity;
import com.sunday.imoocmusicdemo.helps.MediaPlayerHelp;
import com.sunday.imoocmusicdemo.models.MusicModel;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * 1、通过Service 连接 PlayMusicView 和 MediaPlayHelper
 * 2、PlayMusicView -- Service：
 *      1、播放音乐、暂停音乐
 *      2、启动Service、绑定Service、解除绑定Service
 * 3、MediaPlayHelper -- Service：
 *      1、播放音乐、暂停音乐
 *      2、监听音乐播放完成，停止 Service
 */
public class MusicService extends Service {
    public static final String TAG = "MusicService";
//    不可为 0
    public static final int NOTIFICATION_ID = 1;
    private  Disposable mDisposable;
    private MediaPlayerHelp mMediaPlayerHelp;
    private MusicModel mMusicModel;
    private boolean isFirstPlay = false;
    public MusicService() {
    }

    public class MusicBind extends Binder {
        /**
         * 设置音乐（MusicModel）
         */
        public void setMusic (MusicModel musicModel) {
            mMusicModel = musicModel;
            startForeground(); //设置服务在前台可见
        }

        /**
         * 播放音乐
         */
        public void playMusic () {
            /**
             * 1、判断当前音乐是否是已经在播放的音乐
             * 2、如果当前的音乐是已经在播放的音乐的话，那么就直接执行start方法
             * 3、如果当前播放的音乐不是需要播放的音乐的话，那么就调用setPath的方法
             *
             * 4.播放音乐之前得判断音频焦点
             * 5.当在播放音乐时，接受到电话，此时播放的音乐是否还在播放(是否还在进行)
             */
            if (mMediaPlayerHelp.getPath() != null
                    && mMediaPlayerHelp.getPath().equals(mMusicModel.getPath())) {
                mMediaPlayerHelp.start();
            } else {
                mMediaPlayerHelp.setPath(mMusicModel.getPath());
                mMediaPlayerHelp.setOnMeidaPlayerHelperListener(new MediaPlayerHelp.OnMeidaPlayerHelperListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mMediaPlayerHelp.start();
                    }

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopSelf();
                    }
                });
            }

            if (!isFirstPlay) {
                isFirstPlay = true;
                Observable.interval(0, 1000, TimeUnit.MILLISECONDS)
//                        .observeOn(AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.from(Objects.requireNonNull(Looper.myLooper())))
                        .subscribe(observer);
            }
        }

        Observer<Long>  observer = new Observer<Long>(){

            @Override
            public void onSubscribe(Disposable disposable) {
                mDisposable = disposable;
            }

            @Override
            public void onNext(Long aLong) {
                Log.d(TAG, " isMainThread: " +  isMainThread());
                Log.d(TAG, "progress: " + mMediaPlayerHelp.progress());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {
                cancel();
            }
        };

        public boolean isMainThread() {
            return Looper.getMainLooper() == Looper.myLooper();
        }

        /**
         * 取消订阅
         */
        public  void cancel() {
            if (mDisposable != null && !mDisposable.isDisposed()) {
                mDisposable.dispose();
                Log.d(TAG, "====Rx定时器取消======");
            }
        }


        /**
         * 暂停播放
         */
        public void stopMusic () {
            mMediaPlayerHelp.pause();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return new MusicBind();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mMediaPlayerHelp = MediaPlayerHelp.getInstance(this);
    }

    /**
     * 系统默认不允许不可见的后台服务播放音乐，
     * Notification ，
     */
    /**
     * 设置服务在前台可见
     */
    private void startForeground () {

        /**
         * 通知栏点击跳转的intent
         */
        PendingIntent pendingIntent = PendingIntent
                .getActivity(this, 0, new Intent(this, WelcomeActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);


        /**
         * 创建Notification
         */
        Notification notification = null;
        /**
         * android API 26 以上 NotificationChannel 特性适配
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = createNotificationChannel();
            notification = new Notification.Builder(this, channel.getId())
                    .setContentTitle(mMusicModel.getName())
                    .setContentText(mMusicModel.getAuthor())
                    .setSmallIcon(R.mipmap.logo)
                    .setContentIntent(pendingIntent)
                    .build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle(mMusicModel.getName())
                    .setContentText(mMusicModel.getAuthor())
                    .setSmallIcon(R.mipmap.logo)
                    .setContentIntent(pendingIntent)
                    .build();
        }



        /**
         * 设置 notification 在前台展示
         */
        startForeground(NOTIFICATION_ID, notification);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel createNotificationChannel () {
        String channelId = "imooc";
        String channelName = "ImoocMusicService";
        String Description = "ImoocMusic";
        NotificationChannel channel = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(Description);
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        channel.setShowBadge(false);

        return channel;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
