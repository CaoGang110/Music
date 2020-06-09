package com.sunday.imoocmusicdemo.helps;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

public class MediaPlayerHelp {

    public static final String TAG = "MediaPlayerHelp";

    private static MediaPlayerHelp instance;
    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private String mPath;
    private OnMeidaPlayerHelperListener onMeidaPlayerHelperListener;
    private AudioManager mAudioManager;

    public void setOnMeidaPlayerHelperListener(OnMeidaPlayerHelperListener onMeidaPlayerHelperListener) {
        this.onMeidaPlayerHelperListener = onMeidaPlayerHelperListener;
    }

    public static MediaPlayerHelp getInstance(Context context) {

        if (instance == null) {
            synchronized (MediaPlayerHelp.class) {
                if (instance == null) {
                    instance = new MediaPlayerHelp(context);
                }
            }
        }

        return instance;

    }

    private MediaPlayerHelp (Context context) {
        mContext = context;
        mMediaPlayer = new MediaPlayer();
    }


    /**
     * 1、setPath：当前需要播放的音乐
     * 2、start：播放音乐
     * 3、pause：暂停播放
     */

    /**
     * 当前需要播放的音乐
     * @param path
     */
    public void setPath (String path) {
        /**
         * 1、音乐正在播放，重置音乐播放状态
         * 2、设置播放音乐路径
         * 3、准备播放
         */
//        1、音乐正在播放，重置音乐播放状态
        if (mMediaPlayer.isPlaying() || !path.equals(mPath)) {
            mMediaPlayer.reset();
        }
        mPath = path;

//        2、设置播放音乐路径
        try {
            mMediaPlayer.setDataSource(mContext, Uri.parse(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

//        3、准备播放
        mMediaPlayer.prepareAsync();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (onMeidaPlayerHelperListener != null) {
                    onMeidaPlayerHelperListener.onPrepared(mp);
                }
            }
        });

//        监听音乐播放完成
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (onMeidaPlayerHelperListener != null) {
                    onMeidaPlayerHelperListener.onCompletion(mp);
                }
            }
        });

    }

    /**
     * 返回正在播放的音乐路径
     * @return
     */
    public String getPath () {
        return mPath;
    }

    /**
     * 播放音乐
     */
    public void start () {
        if (mMediaPlayer.isPlaying()) return;
        AssetFileDescriptor fileDescriptor ;
        //1 初始化AudioManager对象
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        //2 申请焦点
        mAudioManager.requestAudioFocus(mAudioFocusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        //5 设置播放流类型
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
            }
        });
        mMediaPlayer.start();
//        mMediaPlayer.prepareAsync();




    }

    /**
     * 判断音乐播放进度
     * */
    public int progress(){
        return mMediaPlayer.getCurrentPosition();
    }


    /**
     * 暂停播放
     */
    public void pause () {
        mMediaPlayer.pause();
    }

    /**
     * 通过AudioManager对象调用requestAudioFocus方法，有三个参数
     * OnAudioFocusChangeListener l,int streamType,int durationHint
     * 1 焦点变化的监听器
     * */
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChange = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_LOSS:
                    //长时间丢失焦点,当其他应用申请的焦点为AUDIOFOCUS_GAIN时，
                    //会触发此回调事件，例如播放QQ音乐，网易云音乐等
                    //通常需要暂停音乐播放，若没有暂停播放就会出现和其他音乐同时输出声音
                    Log.d(TAG, "AUDIOFOCUS_LOSS");
//                    stop();
                    pause ();
                    //释放焦点，该方法可根据需要来决定是否调用
                    //若焦点释放掉之后，将不会再自动获得
                    mAudioManager.abandonAudioFocus(mAudioFocusChange);
                break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    //短暂性丢失焦点，当其他应用申请AUDIOFOCUS_GAIN_TRANSIENT或AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE时，
                    //会触发此回调事件，例如播放短视频，拨打电话等。
                    //通常需要暂停音乐播放
//                    stop();
                    pause ();
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //短暂性丢失焦点并作降音处理
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //当其他应用申请焦点之后又释放焦点会触发此回调
                    //可重新播放音乐
                    Log.d(TAG, "AUDIOFOCUS_GAIN");
                    start();
                    break;

                default:
                    /**AUDIOFOCUS_GAIN //长时间获得焦点
                     AUDIOFOCUS_GAIN_TRANSIENT //短暂性获得焦点，用完应立即释放
                     AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK //短暂性获得焦点并降音，可混音播放
                     AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE //短暂性获得焦点，录音或者语音识别
                 */
                    break;
            }
        }
    };

    public interface OnMeidaPlayerHelperListener {
        void onPrepared(MediaPlayer mp);
        void onCompletion(MediaPlayer mp);
    }

}
