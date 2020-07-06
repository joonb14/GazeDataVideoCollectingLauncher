# Android GazeDataVideoCollectingLauncher
### This app is based on following examples
Camera2Api: https://github.com/android/camera-samples/tree/master/Camera2Basic <br>
For Launcher example: https://parallelcodes.com/create-android-launcher-program/ <br>
The Result app looks like this <br><br>
<img width="300" src="https://user-images.githubusercontent.com/30307587/84723891-b34df100-afc1-11ea-9c7b-ed1fbafed6ef.png">
### Application Details
This app collects Front Facing Camera Frame, Gyroscope, Accelerometer, touch (X,Y) position whenever you touch the items in launcher. In order to process and save Front Facing Camera Frame, I delayed starting new Activity by 1sec(1000ms). Otherwise you will face errors such as Camera Handler trying to send message to dead thread.
### Check the Permissions before using the APP
I only ask for Camera permission on the runtime. So you need to check permissions in Application info, and grant External storage R/W permissions before you try collecting data
### Needs Modification for Others to Use
This app is based on devices which has Smallest width 410dp. I worked on Pixel 3 XL specifically.(Current Smartphones' default setting. Confirmed on Nexus 5X, 6P, Pixel XL, 2XL, 3XL, S8, S9+, S10 5G, Note 9, Note 10+. But S20+ was 384dp). <br>
So if you are using device whose Smallest width is not 410dp, you have to change layout_width values in grd_items.xml<br>
Also, if you want to change the grid layout's # of columns, consider changing values in fragment_camera2_basic.xml and grd_items.xml<br>
If installed app captures Rear camera image, then you have to change the value at <br>Camera2BasicFragement.java Line 908~ if (mCameraId.equals("1")) ...... check what number equals to your device's front camera.
### Frame Resolution
My app's default Capturing frame resolution is 640x480.<br>
You can change this setting by modifying <br>Camera2BasicFragement.java Line 569: mImageReader = ImageReader.newInstance(480, 640, ImageFormat.JPEG, /*maxImages*/2);<br>
### Parse Saved Data
Image will be saved to /sdcard/CaptureApp/ directory <br>
If you run into problems in saving images, consider creating directory CaptureApp in /sdcard/ using adb shell then restart the App <br>
I saved all Front Facing Camera Frame, Gyroscope, Accelerometer, touch (X,Y) position in 1 image file. By storing Gyroscope, Accelerometer, touch (X,Y) position in file name. The file name is in following format<br> "pic_front_Datetime_picturecount_positionX_positionY_gyroscopeX_gyroscopeY_gyroscopeZ_accelerometerX_accelerometerY_accelerometerZ.jpg"<br>
I used Sensor.TYPE_GYROSCOPE, Sensor.TYPE_ACCELEROMETER which are calibrated data. If you want to get Raw data, check informations in https://developer.android.com/guide/topics/sensors/sensors_motion
