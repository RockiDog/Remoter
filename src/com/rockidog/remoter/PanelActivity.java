package com.rockidog.remoter;

import com.rockidog.remoter.PanelView;
import com.rockidog.remoter.PanelView.ActionListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class PanelActivity extends Activity implements Runnable {
  private static final int TIME = 30;
  private DatagramSocket mDatagramSocket = null;
  private InetAddress mBroadcastAddress = null;
  private int mPort = 7001;
  private byte[] mBuffer = null;

  private Thread mSocketThread = null;
  private boolean isPaused = false;
  private float[] mDirection = {0, 0};
  private float[] mSpeed = {0, 0};
  private int mButton = ActionListener.INVALID;
  private int mPower = 0;
  private int mDribbleLevel = 0;
  private boolean isButtonClicked = false;

  public static String mIP;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PanelView mPanelView = new PanelView(this);
    mPanelView.setActionListener(new ActionListener() {
      @Override
      public void onJoystickPositionChanged(int joystick, float radian, float speed) {
        mDirection[joystick] = radian;
        mSpeed[joystick] = speed;
      }
      
      @Override
      public void onButtonClicked(int button) {
        //isButtonClicked = true;
        mButton = button;
        mPower = mDribbleLevel;
        //isButtonClicked = false;
      }
      
      @Override
      public void onButtonClicked(int button, int power) {
        isButtonClicked = true;
        mButton = button;
        mPower = power;
        isButtonClicked = false;
      }
      
      @Override
      public boolean onVolunmKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_VOLUME_UP == keyCode) {
          if (2 == mDribbleLevel)
            mDribbleLevel = 0;
          else
            ++mDribbleLevel;
        } else if (KeyEvent.KEYCODE_VOLUME_DOWN == keyCode) {
          if (0 == mDribbleLevel)
            mDribbleLevel = 2;
          else
            --mDribbleLevel;
        } else
          return false;
        Context context = getApplicationContext();
        String message = new String("吸球力度是：") + Integer.toString(mDribbleLevel);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        return true;
      }
    });
    setContentView(mPanelView);
    
    mSocketThread = new Thread(this);
    mSocketThread.start();
  }

  @Override
  protected void onResume() {
    super.onResume();
    isPaused = false;
  }

  @Override
  protected void onPause() {
    super.onPause();
    isPaused = true;
  }

  @Override
  public void run() {
    String buffer = null;
    DatagramPacket data = null;
    try {
      mBroadcastAddress = InetAddress.getByName(mIP);
      mDatagramSocket = new DatagramSocket();
    } catch (SocketException s) {
      s.printStackTrace();
      mDatagramSocket.close();
    } catch (UnknownHostException u) {
      u.printStackTrace();
      mDatagramSocket.close();
    }
    
    while (false == isPaused) {
      long startTime = SystemClock.uptimeMillis();
      
      try {
        // L: Left joystick
        // R: Right joystick
        // D: Direction in angle (by 0.01 degree, [0, 36000])
        // S: Speed of vehicle in percent ([0, 100])
        // B: Button (0: shooting button, 1: dribbling button, 2: invalid button)
        // P: Power in percent ([0, 100])
        buffer = "L" + "D" + Integer.toString((int) mDirection[0]) + "S" + Integer.toString((int) mSpeed[0]);
        buffer += "R" + "D" + Integer.toString((int) mDirection[1]) + "S" + Integer.toString((int) mSpeed[1]);
        buffer += "B" + Integer.toString(mButton) + "P" + Integer.toString(mPower) + "#";
        mBuffer = buffer.getBytes();
        data = new DatagramPacket(mBuffer, mBuffer.length, mBroadcastAddress, mPort);
        mDatagramSocket.send(data);
        if (false == isButtonClicked && ActionListener.SHOOTING == mButton)
          mButton = ActionListener.INVALID;
      } catch (IOException e) {
        e.printStackTrace();
        mDatagramSocket.close();
      }
      
      long endTime = SystemClock.uptimeMillis();
      long diffTime = endTime - startTime;
      try {
        if (TIME > diffTime)
          Thread.sleep(TIME - diffTime);
      } catch(InterruptedException e) {
        e.printStackTrace();
        mDatagramSocket.close();
      }
    }
  }
}
