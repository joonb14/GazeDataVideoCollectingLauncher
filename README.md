# Android GazeDataVideoCollectingLauncher
### This app is based on following examples
Camera2Api: https://github.com/android/camera-samples/tree/master/Camera2Basic <br>
For Launcher example: https://parallelcodes.com/create-android-launcher-program/ <br>
The Result app looks like this <br><br>
<img width="300" src="https://user-images.githubusercontent.com/30307587/84723891-b34df100-afc1-11ea-9c7b-ed1fbafed6ef.png">
### Application Details
This app collects Front Facing Camera <b>Video</b>, Gyroscope, Accelerometer, touch (X,Y) position whenever you touch the items in launcher. In order to process and save Front Facing Camera <b>Video</b>, I delayed starting new Activity by 1sec(1000ms). Otherwise you will face errors such as Camera Handler trying to send message to dead thread.
### Needs Modification for Others to Use
This app is based on devices which has Smallest width 410dp. I worked on Pixel 3 XL specifically.(Current Smartphones' default setting. Confirmed on Nexus 5X, 6P, Pixel XL, 2XL, 3XL, S8, S9+, S10 5G, Note 9, Note 10+. But S20+ was 384dp). <br>
So if you are using device whose Smallest width is not 410dp, you have to change layout_width values in grd_items.xml<br>
Also, if you want to change the grid layout's # of columns, consider changing values in fragment_camera2_basic.xml and grd_items.xml<br>
If installed app captures Rear camera <b>Video</b>, then you have to change the value at <br>Camera2VideoFragement.java Line 406 String cameraId = "1"; ...... check what number equals to your device's front camera.
### Frame Resolution
My app's default <b>Video<//b> resolution is 640x480.<br>
You can change this setting by modifying <br>Camera2VideoFragement.java Line 419: mVideoSize = new Size(640,480);<br>
### Parse Saved Data
<b>Video</b> will be saved to /sdcard/CaptureApp/ directory <br>
Video files are stored in following format: Video_VideoNum_System.currentTimeMillis().mp4<br>
I saved all Logs to /sdcard/LogFile/log.txt<br>
The file name is in following format<br> 
VideoNum, System.currentTimeMillis(), gazeX, gazeY, gyroscopeX, gyroscopeY, gyroscopeZ, AccelerometerX, AccelerometerY, AccelerometerZ<br> 
I used Sensor.TYPE_GYROSCOPE, Sensor.TYPE_ACCELEROMETER which are calibrated data. If you want to get Raw data, check informations in https://developer.android.com/guide/topics/sensors/sensors_motion
