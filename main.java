package org.usfirst.frc.team1089.main;

import java.util.ArrayList;

import org.opencv.core.Mat;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.HttpCamera;
import edu.wpi.cscore.MjpegServer;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoMode;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.tables.ITable;

public class Main {
	
    public static void main(String[] args) {
        // Loads our OpenCV library. This MUST be included
        System.loadLibrary("opencv_java310"); 
        
        // Connect NetworkTables, and get access to the publishing table
        NetworkTable.setClientMode();
        // Set your team number here
        NetworkTable.setTeam(1089);

        NetworkTable.initialize();
        
        // This is the network port you want to stream the raw received image to
        // By rules, this has to be between 1180 and 1190, so 1185 is a good choice
        final int STREAM_PORT = 1186;
        final int SECOND_STREAM_PORT = 1187;
        final Runtime RUNTIME = Runtime.getRuntime();
        
        //Root (string) in order to access the network tables i.e. the key
        
        final String KEY = "Vision";

        // This stores our reference to our mjpeg server for streaming the input image
        
        //Our Network Table
        
        NetworkTable cubeVision = NetworkTable.getTable(KEY + "/CubeVision");
        
        //Our MjpegServers
        
        MjpegServer lifecamRawStream = new MjpegServer("RAW_LIFECAM", STREAM_PORT);
        MjpegServer lifecamOutputStream = new MjpegServer("OUTPUT_LIFECAM", SECOND_STREAM_PORT);
        
        //Our Camera
        
        UsbCamera lifecam = new UsbCamera("Lifecam 3000", 0);
        
        //Our CV Source
        
        CvSource lifecamSource = new CvSource("CvSource_LifeCam", VideoMode.PixelFormat.kMJPEG, 640, 480, 20);
        
        //Our CV Sink
        
        CvSink lifecamSink = new CvSink("Lifecam_Sink");
        
        //Our GripPipeline
        
        GripPipeline grip = new GripPipeline();
        
        //Creates a thread 
        
        //setting the lifecam to have source lifecam
        
        lifecamSink.setSource(lifecam);
        lifecamRawStream.setSource(lifecam);
        
        //declares the settings for our lifecam
        
        lifecam.setResolution(640, 480);
        lifecam.setFPS(20);
        lifecam.setBrightness(30);
        lifecam.setExposureManual(0);
        
        //adds listeners for values for camera settings and hsl settings
        
        NetworkTable.getTable(KEY + "/CubeVision").addTableListener(
        		(ITable table, String key, Object value, boolean isNew) -> {
        				lifecam.setBrightness((int)value);});
        
        RUNTIME.addShutdownHook(new Thread(() -> {
		    System.out.println("Shutting Down...");
        	lifecamOutputStream.free();
        	lifecamSink.free();
        	lifecamSource.free();
        	lifecamRawStream.free();   
	   }));
	   while (true) {     
	     // All Mats and Lists should be stored outside the loop to avoid allocations
	        // as they are expensive to create
	        Mat img = new Mat();
	
	        // Infinitely process image
	        while (!Thread.interrupted()) {
	            // Grab a frame. If it has a frame time of 0, there was an error.
	            // Just skip and continue
	            if (lifecamSink.grabFrame(img) == 0) {
	                System.out.println(Thread.currentThread().getName() + ": " + lifecamSink.getError());
	                continue;
	            }
	            grip.process(img);
	            img.release();
	        }
	   }
    }

        // Selecting a Camera
        // Uncomment one of the 2 following camera options
        // The top one receives a stream from another device, and performs operations based on that
        // On windows, this one must be used since USB is not supported
        // The bottom one opens a USB camera, and performs operations on that, along with streaming
        // the input image so other devices can see it.

        // HTTP Camera
	/*
	// This is our camera name from the robot. this can be set in your robot code with the following command
	// CameraServer.getInstance().startAutomaticCapture("YourCameraNameHere");
	// "USB Camera 0" is the default if no string is specified
	String cameraName = "USB Camera 0";
	HttpCamera camera = setHttpCamera(cameraName, inputStream);
	// It is possible for the camera to be null. If it is, that means no camera could
	// be found using NetworkTables to connect to. Create an HttpCamera by giving a specified stream
	// Note if this happens, no restream will be created
	if (camera == null) {
	  camera = new HttpCamera("CoprocessorCamera", "YourURLHere");
	  inputStream.setSource(camera);
	}
	*/

        // All Mats and Lists should be stored outside the loop to avoid allocations
        // as they are expensive to create

        // Infinitely process image

    private static HttpCamera setHttpCamera(String cameraName, MjpegServer server) {
        // Start by grabbing the camera from NetworkTables
        NetworkTable publishingTable = NetworkTable.getTable("CameraPublisher");
        // Wait for robot to connect. Allow this to be attempted indefinitely
        while (true) {
            try {
                if (publishingTable.getSubTables().size() > 0) {
                    break;
                }
                Thread.sleep(500);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


        HttpCamera camera = null;
        if (!publishingTable.containsSubTable(cameraName)) {
            return null;
        }
        ITable cameraTable = publishingTable.getSubTable(cameraName);
        String[] urls = cameraTable.getStringArray("streams", null);
        if (urls == null) {
            return null;
        }
        ArrayList<String> fixedUrls = new ArrayList<String>();
        for (String url : urls) {
            if (url.startsWith("mjpg")) {
                fixedUrls.add(url.split(":", 2)[1]);
            }
        }
        camera = new HttpCamera("CoprocessorCamera", fixedUrls.toArray(new String[0]));
        server.setSource(camera);
        return camera;
    }

    private static UsbCamera setUsbCamera(int cameraId, MjpegServer server) {
        // This gets the image from a USB camera
        // Usually this will be on device 0, but there are other overloads
        // that can be used
        UsbCamera camera = new UsbCamera("CoprocessorCamera", cameraId);
        server.setSource(camera);
        return camera;
    }

}
