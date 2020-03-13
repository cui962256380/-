package com.rich.audiorecord;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MY_PERMISSIONS_REQUEST = 1001;
    private static final String TAG = "AUDIO_RECORD";
    private AudioRecord mAudioRecord;   //接收录音
    private static final int mAudioSource = MediaRecorder.AudioSource.MIC;
    //指定采样率 （MediaRecoder 的采样率通常是8000Hz AAC的通常是44100Hz。 设置采样率为44100，目前为常用的采样率，官方文档表示这个值可以兼容所有的设置）
    private static final int mSampleRateInHz = 44100;
    //指定捕获音频的声道数目。在AudioFormat类中指定用于此的常量
    private static final int mChannelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO; //单声道
    //指定音频量化位数 ,在AudioFormaat类中指定了以下各种可能的常量。通常我们选择ENCODING_PCM_16BIT和ENCODING_PCM_8BIT PCM代表的是脉冲编码调制，它实际上是原始音频样本。
    //因此可以设置每个样本的分辨率为16位或者8位，16位将占用更多的空间和处理能力,表示的音频也更加接近真实。
    private static final int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //指定缓冲区大小。调用AudioRecord类的getMinBufferSize方法可以获得。
    private int mBufferSizeInBytes = AudioRecord.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat);//计算最小缓冲区
    //创建AudioRecord。AudioRecord类实际上不会保存捕获的音频，因此需要手动创建文件并保存下载。


    private AudioTrack mAudioTrack;


    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    ArrayList mPermissionList = new ArrayList();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();


        initView();
    }

    private Button mStart,mStop,mPlay;
    private ProgressBar mPb;

    private void initView() {
        mStart=findViewById(R.id.start);
        mStop=findViewById(R.id.stop);
        mPlay=findViewById(R.id.play);
        mPb=findViewById(R.id.pb);
        mPb.setVisibility(View.GONE);
        mStart.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mPlay.setOnClickListener(this);
    }



    private void checkPermissions() {
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                    PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        if (!mPermissionList.isEmpty()) {
            String[] permissions = (String[]) mPermissionList.toArray(new String[mPermissionList.size()]);
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("AudioRecord", permissions[i] + " 权限被用户禁止！");
                }
            }
            // 运行时权限的申请不是本demo的重点，所以不再做更多的处理，请同意权限申请。
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                Toast.makeText(this,"Start",Toast.LENGTH_SHORT).show();
                mStart.setEnabled(false);
                mPb.setVisibility(View.VISIBLE);
                start();

                break;
            case  R.id.stop:
                mStart.setEnabled(true);
                mPb.setVisibility(View.GONE);
                stop();
                break;

            case R.id.play:
                play();
                break;

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void play() {
        mAudioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(mSampleRateInHz)
                        .setChannelMask(mChannelConfig)
                        .build(),
                mBufferSizeInBytes, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);

       new Thread(new Runnable() {
           @Override
           public void run() {
               if(mAudioTrack!=null){
                   mAudioTrack.play();
                   File pcmFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
                   if(pcmFile!=null){
                       byte[] tempBuffer = new byte[mBufferSizeInBytes];
                       try {
                           FileInputStream fis=new FileInputStream(pcmFile);
                             if (fis.available() > 0){  //获取test.pcm的大小
                                 int read=0;
                                 while ((read = fis.read(tempBuffer))!= -1 ){
                                     if(read == AudioTrack.ERROR_INVALID_OPERATION ||
                                             read == AudioTrack.ERROR_BAD_VALUE ||
                                             read ==AudioTrack.ERROR
                                     ){
                                         continue;
                                     }else{
                                         mAudioTrack.write(tempBuffer,0,read);
                                     }
                                 }

                             }

                       } catch (FileNotFoundException e) {
                           e.printStackTrace();
                       }catch (IOException e) {
                           e.printStackTrace();
                       }
                   }
               }
           }
       }).start();
    }

    private void stop() {
        if(mAudioRecord!=null){
            isRecording =false;
            mAudioRecord.stop();
            mAudioRecord.release();
            pcmtoWav();  //Android不能直接进行pcm格式的播放。
        }


    }

    @Override
    protected void onDestroy() {
        if (mAudioRecord != null) {
            mAudioRecord = null;
        }
        if (mAudioTrack != null) {
            mAudioTrack = null;
        }
        super.onDestroy();
    }

    boolean isRecording;
    FileOutputStream fos = null;
    byte data[] = new byte[mBufferSizeInBytes];

    private void start() {
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRateInHz, mChannelConfig, mAudioFormat, mBufferSizeInBytes);
        mAudioRecord.startRecording();
        isRecording = true;
        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
        if (!file.exists()) {
            file.delete();
        }
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "fos err!");
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (fos != null) {
                    while (isRecording) {
                        int read = mAudioRecord.read(data, 0, mBufferSizeInBytes);  //由AudioRecord读出来数据
                        if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                            try {
                                fos.write(data);   //通过字符流写入到SDCAED
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
    }

    public void  pcmtoWav(){
        /*将PCM装WVA*/
        new Thread(new Runnable() {
            @Override
            public void run() {
                PcmToWavUtil  ptw=new PcmToWavUtil(mSampleRateInHz,mChannelConfig,mAudioFormat);
                File pcmFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
                File wavFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.wav");
                if(wavFile.exists()){
                    wavFile.delete();
                }
                ptw.pcmToWav(pcmFile.getAbsolutePath(),wavFile.getAbsolutePath());
            }
        }).start();
    }

}
