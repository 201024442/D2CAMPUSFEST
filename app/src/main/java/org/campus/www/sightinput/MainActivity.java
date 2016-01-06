package org.campus.www.sightinput;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


//전체적인 내용은 OpenCV 3.1.0의 Sample인 tutorial-1-camerapreview를 기준으로.
//http://sourceforge.net/projects/opencvlibrary/files/opencv-android/
public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    //입력을 저장할 Mat
    Mat inputMat;
    //결과를 저장할 Mat
    Mat outputMat;
    //그릴 원의 중심
    Point circleCenter;
    //그릴 원의 색
    Scalar circleColor;
    //RGB식으로 사진을 저장하기 위한 Mat형 변수.
    Mat mRgba;
    //HSV식으로 사진을 저장하기 위한 Mat형 변수.
    Mat mHSV;
    Mat mThresholded;
    //캡슐화를 위한 클래스
    App app;
    //표시하는 원의 중심점
    Point center;
    //텍스트 뷰
    TextView textView;
    //onFrame(매 프레임마다 작동)에서 setText에서 예외가 발생하므로 해결하기 위한 핸들러.
    Handler handler;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.cvcameraView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        handler = new Handler();
        //캡슐화를 위한 클래스
        app = new App();
        //(Team & OpenSource)변수들 초기화
        inputMat = new Mat();
        outputMat = new Mat();
        circleCenter = new Point(100,100);

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mHSV = new Mat(height, width, CvType.CV_8UC4);
        mThresholded=new Mat(height,width,CvType.CV_8UC1);
        textView = (TextView)findViewById(R.id.textInput);
        textView.setText("");
    }

    public void onCameraViewStopped() {
        Log.d(TAG, "DEBUG--Camera View END");
    }

    //매 카메라 프레임마다 작동되는 메소드
    public Mat onCameraFrame(CvCameraViewFrame inputFrame){
        //오픈 소스를 사용해 물체를 따라가는 알고리즘을 구현
        //http://cell0907.blogspot.kr/2013/08/tracking-ball-in-android-with-opencv.html

        //카메라로 받은 입력을 Mat 변수에 RGB형태로 저장.
        inputMat = inputFrame.rgba();
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV,4);

        //색, 채도, 명도.
        Scalar hsv_min = new Scalar(175, 150, 150, 0);
        Scalar hsv_max = new Scalar(179, 255, 255, 0);
        //mRgba 변수를 HSV식으로 변환해서 mHSV 변수에 저장.
        Imgproc.cvtColor(mRgba, mHSV, Imgproc.COLOR_RGB2HSV,4);
        //색, 채도, 명도(HSV)가 hsv_min과 hsv_max에 표기된 값 사이에 맞는다면 mThresholded에 하얗게 표시.
        Core.inRange(mHSV, hsv_min, hsv_max, mThresholded);

        //허프 변환으로 붉은색의 중앙 검출
        Mat circles = new Mat();
        Imgproc.HoughCircles(mThresholded, circles, Imgproc.CV_HOUGH_GRADIENT, 2, mThresholded.height()/4, 500, 50, 0, 0);

        //붉은색의 중앙 좌표를 저장할 배열
        float[] data2 = new float[circles.rows() * (int)circles.elemSize()/4];
        //circles.rows()가 0이 아니면, 즉 지정한 색이 검출되면.
        if(circles.rows() != 0) {
            //Circles는 Mat형 변수이고.
            circles.get(0, 0, data2);


            center = new Point(data2[0],data2[1]);
            Imgproc.circle(mRgba,center,25,new Scalar(250,250,250),10);
        }//end of if
        //입력을 기다리고 있다가
        if(app.waitingNow()) {
            //대기상태에서 입력이 들어오면 트래킹을 시작
            if(circles.rows()!=0){
                app.start_tracking();
            }//end of if
        }//end of if
        //트래킹 도중이다만 입력 도중 취소해도 여기로 돌아옴
        else if(app.trackingNow()&&circles.rows()!=0){
            app.clock_reset();
        }//end of else if
        //도중에 사라지면 입력중
        else if(app.trackingNow()&&circles.rows()==0){
            app.clock_plus();
            Log.d(TAG, "DEBUG--inputing "+app.getClock());
            if(app.clockIsOver()){
                Log.d(TAG, "DEBUG--inputed : ("+center.x+", "+center.y+")");
                app.start_waiting();
                app.clock_reset();
                //아래는 버튼 위치 분별 코드. 팀에서 작성.
                if(center.x>app.button_1_start.x&&center.x<app.button_1_end.x
                        &&center.y>app.button_1_start.y&&center.y<app.button_1_end.y)input_1();
                else if(center.x>app.button_2_start.x&&center.x<app.button_2_end.x
                        &&center.y>app.button_2_start.y&&center.y<app.button_2_end.y)input_2();
                else if(center.x>app.button_3_start.x&&center.x<app.button_3_end.x
                        &&center.y>app.button_3_start.y&&center.y<app.button_3_end.y)input_3();
                else if(center.x>app.button_4_start.x&&center.x<app.button_4_end.x
                        &&center.y>app.button_4_start.y&&center.y<app.button_4_end.y)input_4();
                else if(center.x>app.button_5_start.x&&center.x<app.button_5_end.x
                        &&center.y>app.button_5_start.y&&center.y<app.button_5_end.y)input_5();
                else if(center.x>app.button_6_start.x&&center.x<app.button_6_end.x
                        &&center.y>app.button_6_start.y&&center.y<app.button_6_end.y)input_6();
                else if(center.x>app.button_7_start.x&&center.x<app.button_7_end.x
                        &&center.y>app.button_7_start.y&&center.y<app.button_7_end.y)input_7();
                else if(center.x>app.button_8_start.x&&center.x<app.button_8_end.x
                        &&center.y>app.button_8_start.y&&center.y<app.button_8_end.y)input_8();
                else if(center.x>app.button_9_start.x&&center.x<app.button_9_end.x
                        &&center.y>app.button_9_start.y&&center.y<app.button_9_end.y)input_9();
                else if(center.x>app.button_0_start.x&&center.x<app.button_0_end.x
                        &&center.y>app.button_0_start.y&&center.y<app.button_0_end.y)input_0();
                else if(center.x>app.button_Back_start.x&&center.x<app.button_Back_end.x
                        &&center.y>app.button_Back_start.y&&center.y<app.button_Back_end.y)backSpace();
                else{
                    startActivity(new Intent("android.intent.action.CALL", Uri.parse("tel:" + textView.getText())));
                }//end of else
            }//end of if
        }//end of else if
        //입력 완료

        //매 프레임마다 버튼을 그려줘야 함.
        app.drawButton(mRgba);

        //return값(Mat형)이 기기의 화면으로 출력된다.
        return mRgba;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    //조건이 갖춰졌을 때에 내용을 입력하는 부분. onCameraFrame에서는 권한 때문에 .setText메소드를 사용할 수 없지만,
    //Handler를 사용하면 되는 걸 오픈 소스로 배움.
    //http://stackoverflow.com/questions/3280141/calledfromwrongthreadexception-only-the-original-thread-that-created-a-view-hie
    private void input_1(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"1");
            }
        });
    }
    private void input_2(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"2");
            }
        });
    }
    private void input_3(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"3");
            }
        });
    }
    private void input_4(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"4");
            }
        });
    }
    private void input_5(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"5");
            }
        });
    }
    private void input_6(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"6");
            }
        });
    }
    private void input_7(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"7");
            }
        });
    }
    private void input_8(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"8");
            }
        });
    }
    private void input_9(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"9");
            }
        });
    }
    private void input_0(){
        handler.post(new Runnable(){
            public void run(){
                textView.setText(textView.getText()+"0");
            }
        });
    }
    private void backSpace(){
        handler.post(new Runnable(){
            public void run(){
                CharSequence temp = textView.getText();
                temp = temp.subSequence(0,temp.length()-1);
                textView.setText(temp);
            }
        });
    }
}


class App{
    private int clock_limit;
    private int clock_now;
    private boolean waiting_input;
    private boolean tracking_input;

    private int blank_left = 100;
    private int blank_upper = 0;
    private int button_width = 300;
    private int button_height = 170;

    public Point button_1_start = new Point(blank_left,blank_upper);
    public Point button_1_end = new Point(blank_left+button_width,blank_upper+button_height);
    public Point button_2_start = new Point(blank_left+button_width,blank_upper);
    public Point button_2_end = new Point(blank_left+button_width*2,blank_upper+button_height);
    public Point button_3_start = new Point(blank_left+button_width*2,blank_upper);
    public Point button_3_end = new Point(blank_left+button_width*3,blank_upper+button_height);

    public Point button_4_start = new Point(blank_left,blank_upper+button_height);
    public Point button_4_end = new Point(blank_left+button_width,blank_upper+button_height*2);
    public Point button_5_start = new Point(blank_left+button_width,blank_upper+button_height);
    public Point button_5_end = new Point(blank_left+button_width*2,blank_upper+button_height*2);
    public Point button_6_start = new Point(blank_left+button_width*2,blank_upper+button_height);
    public Point button_6_end = new Point(blank_left+button_width*3,blank_upper+button_height*2);

    public Point button_7_start = new Point(blank_left,blank_upper+button_height*2);
    public Point button_7_end = new Point(blank_left+button_width,blank_upper+button_height*3);
    public Point button_8_start = new Point(blank_left+button_width,blank_upper+button_height*2);
    public Point button_8_end = new Point(blank_left+button_width*2,blank_upper+button_height*3);
    public Point button_9_start = new Point(blank_left+button_width*2,blank_upper+button_height*2);
    public Point button_9_end = new Point(blank_left+button_width*3,blank_upper+button_height*3);

    public Point button_0_start = new Point(blank_left,blank_upper+button_height*3);
    public Point button_0_end = new Point(blank_left+button_width*3,blank_upper+button_height*4);

    public Point button_Back_start = new Point(blank_left+button_width*3,blank_upper);
    public Point button_Back_end = new Point(blank_left+button_width*3+button_width/2,blank_upper+button_height*4);


    //캡슐화를 위한 클래스
    App(){
        clock_limit = 7;
        clock_now = 0;
        //입력을 대기하는 중. 이게 True인 경우 아직 자판에 손이 올라가지 않은 상태.
        waiting_input = true;
        //입력을 고르는 중. 이게 True인 경우 이제 물체를 clock_limit프레임동안 가리고 있으면 입력됨.
        tracking_input = false;
    }

    Mat drawButton(Mat mRgba){
        Imgproc.rectangle(mRgba,button_1_start,button_1_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_2_start,button_2_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_3_start,button_3_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_4_start,button_4_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_5_start,button_5_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_6_start,button_6_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_7_start,button_7_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_8_start,button_8_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_9_start,button_9_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_0_start,button_0_end,new Scalar(0,0,0),5);
        Imgproc.rectangle(mRgba,button_Back_start,button_Back_end,new Scalar(0,0,0),5);
        return mRgba;
    }//end of drawButton

    void clock_plus(){
        clock_now++;
    }

    boolean clockIsOver(){
        if(clock_now >= clock_limit){
            return true;
        }//end of if
        return false;
    }//end of check_clock

    void clock_reset(){
        clock_now = 0;
    }

    void start_tracking(){
        waiting_input = false;
        tracking_input = true;
    }

    void start_waiting(){
        waiting_input = true;
        tracking_input = false;
    }

    boolean trackingNow(){
        return tracking_input;
    }//end of checkTrackingNow

    boolean waitingNow(){
        return waiting_input;
    }//end of checkWaitingNow

    int getClock(){
        return clock_now;
    }

}