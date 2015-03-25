package com.pebble.getpebbledata;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.*;
import android.content.Context;
import android.hardware.*;
import android.location.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.google.common.primitives.UnsignedInteger;

public class MainActivity extends Activity { 
	static String uuid= "7def5a6c-22af-45bd-b332-51ef6031d520"; //old pebble app uuid
	//static String uuid= "2a5f295a-3fab-4b9d-be97-30ab93609f84";
	//static String uuid= "3fc14e3e-6561-4e08-ae4a-6913d1137027"; //testing pebble app uuid
	//static String user=getMacAddress();
	Timer timer;
	TimerTask timerTask;
	static String pass="1314";
	static String op="ADD";
	private static final UUID PEBBLE_DATA_APP_UUID = UUID.fromString(uuid); 
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss"); //Set the format of the .txt file name.       
    private SensorManager sensorManager;    
    private MySensorEventListener sensorEventListener;
    private String phoneAcc="";   
    private PebbleKit.PebbleDataLogReceiver mDataLogReceiver = null;
    //*********************private

    //Define the path of saving the txt file.
    //Save data to SDcard: Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "cache" + File.separator
    //Save data to the root folder: File.separator + "tmp" +  File.separator + "cache"+ File.separator     
    private String folderPath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "cache" + File.separator;
    private String LogFolder = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "cache" + File.separator;
   // private String rootPath =File.separator + "tmp" +  File.separator + "cache"+ File.separator;
    TextView textView = null;
    private boolean registered = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);        
        //DATE_FORMAT.setTimeZone(TimeZone.getDefault());
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0")); 
        //获取感应器管理器 
        sensorEventListener = new MySensorEventListener(); 
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
        //dataupload();        
        startTimer();        
    }   
    
    @Override
    protected void onResume() {    	
        super.onResume();  
        textView = (TextView) findViewById(R.id.log_data_text_view);
        
        PebbleKit.PebbleDataLogReceiver mDataLogReceiver = new PebbleKit.PebbleDataLogReceiver(PEBBLE_DATA_APP_UUID) {    
        	Lock lock= new ReentrantLock(true); //Define a lock to avoid the concurrency problem when writing data to txt file
       	 	int count=0; //Define a count number and show it on the screen to check if the application is working correctly       	 	
       	 	@Override  
			public void receiveData(Context context, UUID logUuid, UnsignedInteger timestamp, UnsignedInteger tag, byte[] accdata) {         	 	   	
       	 		//************receive data***
       	 	    textView.append("Data is coming....\n");       	 		
       	 		//Transform the format of received data from byte array to string. 
           		String dataString = null;
           		String UIString =null;
    			try {
    				dataString = new String(accdata, "UTF-8"); 
    				//********data showing
    				textView.append("Here it is:" + dataString+"\n");
    			} catch (UnsupportedEncodingException e) {
    				textView.setText("Error unencoding data:"+e.getMessage());
    				return;
    			}
    			
    			dataString=dataString.substring(0, dataString.indexOf("}"))+ getLocation()+phoneAcc+",'op':'"+op+"','us':'"+getMacAddress()+"','pb':'"+getPebbleAddress()+"','pass':'"+pass+"'}";	
    			UIString =dataString.substring(0, dataString.indexOf(",'pass'"))+ "}";	//hide psw from the screen
    						
    			textView.append("Integrated data:" + dataString+"\n");
    			
    			//Create txt file and write data into it
    			lock.lock(); //Lock begin
    			FileWriter fw = null;			
    			try{
    				fw = new FileWriter(folderPath + getUintAsTimestamp(timestamp) + ".txt",true);
    			}catch(IOException e) {
    				textView.setText("Error creating buferWritter:"+e.getMessage());
    				lock.unlock(); //Release lock
    				return;
    			}			
    			BufferedWriter bufferWritter = new BufferedWriter(fw);
    			try {
    				bufferWritter.write(dataString+"\n");
    				bufferWritter.close();
    			} catch (IOException e) {
    				textView.setText("Error writing to and closing file:"+e.getMessage());
    				lock.unlock(); //Release lock
    				return;
    			}
    			lock.unlock();  //Release lock    			
    			//Update the UI of the application	                
    			textView.setText("<"+Integer.toString(count++)+">:"+UIString+"\n");	
       	 	}
       	 	//@Override
       	 	//public void onFinishSession(Context context, UUID logUuid, UnsignedInteger timestamp, UnsignedInteger tag){}      
       	 	//run the datasending process	   
        };  
        
        //***************check SD card exist or not************
        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if(isSDPresent) { // if SD-card is present
        	//creat /tmp/cache folder if not exist
            try {
    			File folder0 = new File(Environment.getExternalStorageDirectory() + File.separator + "tmp");
    			//boolean success = true;
    			if (!folder0.exists()) {
    				textView.append("I am creating the tmp folder \n");
    			    folder0.mkdir();
    			}
    			File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "tmp" +  File.separator + "cache");
    			
    			//boolean success = true;
    			if (!folder.exists()) {
    				textView.append("I am creating the cache folder \n");
    			    folder.mkdir();
    			}
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			textView.append("Can not creat the cache folder \n");
    		}
        }
        else
        {
        	textView.setText("ERROR! SD card is not detected!!\n Please check and try again!!");	
        }              
        
      //****************creat log file************
        FileWriter fLog = null;
        textView.append("Trying to create log file\n");
        try{
			fLog = new FileWriter(LogFolder + "log.txt",true);
			textView.append("Log file created.\n");
		}catch(IOException e) {
			textView.append("I can not creat log file, because:\n");
			textView.append("Error creating buferWritter:"+e.getMessage());
			return;			
		}    
        
        //********Begin receiving data*************
        if (mDataLogReceiver != null && !registered) {
        	registered = true;
        	textView.append("I am receiving data now...\n" + Environment.getExternalStorageDirectory().getPath());
        	//*******writing to log*******
        	BufferedWriter logWritter = new BufferedWriter(fLog);
			try {
				logWritter.write("I am receiving data now...\n");
				textView.append("log writed\n");
				logWritter.close();
			} catch (IOException e) {
				textView.append("Error writing to and closing file:"+e.getMessage());				
				return;
			}//*********
        	try {
				PebbleKit.registerDataLogReceiver(this, mDataLogReceiver);				
				PebbleKit.requestDataLogsForApp(this, PEBBLE_DATA_APP_UUID);
				
				textView.append("Data request sent!!Waiting data...\n"+mDataLogReceiver.getResultCode());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				textView.append("No data received!!! \n");
			}
        } else {
        	textView.append("No data received!!! \n");
        }
        
      //获取加速度传感器    
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);    
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);  //FASTEST: 0ms; DELAY_GAME: 20ms; UI: 60ms; NORMAL: 200ms
      
       //creat timer
        //startTimer();               
        
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	if (mDataLogReceiver != null) {
    		unregisterReceiver(mDataLogReceiver);
    		mDataLogReceiver = null;
    	}
    }
    
    //TODO onExit unregisterReceiver(mDataLogReceiver);
    @Override
    protected void onDestroy() {
    	super.onDestroy();    	
    	registered = false;
        if (mDataLogReceiver != null) {        	
            unregisterReceiver(mDataLogReceiver);
            mDataLogReceiver = null;
        }
    }
        
    @Override
    public void finish (){
    	registered = false;
        if (mDataLogReceiver != null) {        	
            unregisterReceiver(mDataLogReceiver);
            mDataLogReceiver = null;
        }
    	System.exit(0);
    	sensorManager.unregisterListener(sensorEventListener); 
    	timer.cancel();
    } 
    
    public void startTimer() {
    	//set a new Timer
    	timer = new Timer();
    	//initialize the TimerTask's job
    	initializeTimerTask();
    	//schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
    	timer.schedule(timerTask, 5000, 60000); //
    }
    
    public void initializeTimerTask() {
    	timerTask = new TimerTask() {
    	public void run() {
    		//***Get file list in the folder // stackoverflow.com/questions/8646984/how-to-list-files-in-an-android-directory
            String folderpath = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "cache";      
    		//String folderpath = File.separator + "tmp" +  File.separator + "cache";    
            Log.d("Files", "newPath: " + folderpath);
            File f = new File(folderpath);                    
			try {
				File file[] = f.listFiles();
	            Log.d("Files", "Size: "+ file.length);
	            for (int i=0; i < file.length; i++) //Send all the files to the server one by one.
	            {
	                Log.d("Files", "FileName:" + file[i].getName());
	                String datapath = folderpath +  File.separator +file[i].getName();
	                Log.d("Files", "path" + datapath);
	                new RetrieveFeedTask().execute(datapath); //execute new thread 执行同步线程
	                Log.d("Files", "i:" + i);            
	            }      
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d("Files", e.getLocalizedMessage() );
			}
                  
    	}
      };
    }
    
    
    
    //Change the date format
    private String getUintAsTimestamp(UnsignedInteger uint) {    	
        return DATE_FORMAT.format(new Date(uint.longValue() * 1000L)).toString();
    }   
    
    //Get the GPS information from the phone. //Reference: http://blog.csdn.net/cjjky/article/details/6557561
    private String getLocation(){    	
    	double latitude=0.0;
    	double longitude =0.0;	
    	double altitude =0.0;	
    	float accuracy=0;
    	long t_gps=0;
    	LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    			if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
    				Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    				if(location != null){
    					latitude = location.getLatitude();
    					longitude = location.getLongitude();
    					altitude = location.getAltitude();
    					accuracy=location.getAccuracy();
    					//t_gps=location.getTime();
    					t_gps=System.currentTimeMillis()+1*3600*1000;
    					}
    			}else{
    				LocationListener locationListener = new LocationListener() {    					
    					// Provider的状态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
    					@Override
    					public void onStatusChanged(String provider, int status, Bundle extras) {    						
    					}    					
    					// Provider被enable时触发此函数，比如GPS被打开
    					@Override
    					public void onProviderEnabled(String provider) {    						
    					}    					
    					// Provider被disable时触发此函数，比如GPS被关闭 
    					@Override
    					public void onProviderDisabled(String provider) {    						
    					}    					
    					//当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发 
    					@Override
    					public void onLocationChanged(Location location) {
    						if (location != null) {   
    							Log.e("Map", "Location changed : Lat: "  
    							+ location.getLatitude() + " Lng: "  + location.getLongitude()+ " Alt: "  
    	    					+ location.getAltitude()+" Acc: " + location.getAccuracy()+" t_gps:"+location.getTime());   
    						}
    					}
    				};
    				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,300, 0,locationListener);   
    				Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);   
    				if(location != null){   
    					latitude = location.getLatitude(); //经度   
    					longitude = location.getLongitude(); //纬度
    					altitude=location.getAltitude(); //海拔
    					accuracy=location.getAccuracy(); //精度, in meters
    					//t_gps=location.getTime(); //timestamp
    					t_gps=System.currentTimeMillis()+1*3600*1000;
    				}    
    			}
    			String location=",'lat':"+String.valueOf(latitude)+",'lng':"+String.valueOf(longitude)+",'alt':"+String.valueOf(altitude)
    					+",'acc':"+Float.toString(accuracy)+",'t_gps':"+Long.toString(t_gps);
    			return location;    	
    }
    
    //Get accelerometer values.  //Reference:http://blog.csdn.net/tangcheng_ok/article/details/6590493   
    private final class MySensorEventListener implements SensorEventListener {    
        @Override    
        public void onSensorChanged(SensorEvent event) {                  
           if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) { //得到加速度的值 
                float x = event.values[0]/0.00981f;          
                float y = event.values[1]/0.00981f;          
                float z = event.values[2]/0.00981f;
                //long t_acc=event.timestamp;	//nanoseconds since uptime
                //change the event.timestamp to milliseconds 
                //http://stackoverflow.com/questions/5500765/accelerometer-sensorevent-timestamp
                long timeInMillis = (new Date()).getTime() 
                        + (event.timestamp - System.nanoTime()) / 1000000L+1*3600*1000;
                long t_acc = timeInMillis;
                
                phoneAcc= ",'px':"+Float.toString(x)+",'py':"+Float.toString(y)+",'pz':"+Float.toString(z)+",'ta':"+Long.toString(t_acc);
            }               
        }     
        @Override    
        public void onAccuracyChanged(Sensor sensor, int accuracy) {    
        }    
    }    
    
    //Get MAC address of the phone, here we use the bluetooth MAC as the ID of the phone. 
    //Other method refer to cloudstack.blog.163.com/blog/static/1876981172012710823152/
    private String getMacAddress(){  
    	BluetoothAdapter m_BluetoothAdapter = null; // Local Bluetooth adapter 
    	m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();      
    	String m_szBTMAC = m_BluetoothAdapter.getAddress();  
    	return m_szBTMAC;
    }  
    
    //Get pebble MAC address
    private String getPebbleAddress(){
    	//获得BluetoothAdapter对象，该API是android 2.0开始支持的  
    	String pebbleAddress="";
        BluetoothAdapter adapter = null;
        adapter = BluetoothAdapter.getDefaultAdapter();          
    	Set<BluetoothDevice> devices = adapter.getBondedDevices();      	
        if(devices.size()>0){          	
            for(Iterator<BluetoothDevice> it = devices.iterator();it.hasNext();)
            {  //get the address of all paired devices
                BluetoothDevice device = (BluetoothDevice)it.next();  
                pebbleAddress = pebbleAddress + device.getAddress();  
            } 
            
        }else{  
            pebbleAddress = "ERR_NO_DEVICE";  
        }  
        pebbleAddress= pebbleAddress.substring(0,17); //only keep the first one
        return pebbleAddress;
    }
  

}

//Creat a new thread to send the data to server. Since Android 4.0, it is not allowed to access Internet in the main thread.
//创建同步线程 http://stackoverflow.com/questions/6343166/android-os-networkonmainthreadexception
class RetrieveFeedTask extends AsyncTask<String, Void, Void> {
    //private Exception exception;
    protected Void doInBackground(String... datapath) {
    	String  thisLine = null;
    	try{
    	  // open input stream test.txt for reading purpose.
    	  BufferedReader br = new BufferedReader(new FileReader(datapath[0]));    	  	  
    	  while ((thisLine = br.readLine()) != null) {    		 
    		  String obj = thisLine.replaceAll("\'","\"");    		  
    		  obj = obj.replaceAll("\"x\"","\"peb_a_x\"");
    		  obj = obj.replaceAll("\"y\"","\"peb_a_y\"");
    		  obj = obj.replaceAll("\"z\"","\"peb_a_z\"");
    		  obj = obj.replaceAll("\"a\"","\"peb_a_a\"");
    		  obj = obj.replaceAll("\"t\"","\"peb_time\"");
    		  obj = obj.replaceAll("\"px\"","\"phone_x\"");
    		  obj = obj.replaceAll("\"py\"","\"phone_y\"");
    		  obj = obj.replaceAll("\"pz\"","\"phone_z\"");
    		  obj = obj.replaceAll("\"ta\"","\"phone_acc_time\"");
    		  obj = obj.replaceAll("\"us\"","\"user_phone\"");
    		  obj = obj.replaceAll("\"pb\"","\"user_peb\"");   
    		  //obj = obj.replaceAll("\"","\\\\\"");    	
    		  
    		  JSONObject jobj = new JSONObject(obj); //build json object  
    		  HttpClient httpClient = new DefaultHttpClient();
    		  try {
    			System.out.println("Begin sending");  
    		  
  	            HttpPost httpPost = new HttpPost("http://138.100.72.5/pebbledatamanagement.php");
  	            StringEntity se = new StringEntity(jobj.toString(), "utf-8");
	            httpPost.setEntity(se);
	            httpPost.addHeader("content-type", "application/json; charset=utf8;");
	            httpPost.setHeader("Accept", "application/json");	            
	           
                HttpResponse httpResponse = httpClient.execute(httpPost);	
	            //int responseCode = httpResponse.getStatusLine().getStatusCode();    	           
	            //httpResponse.close();	
	        }catch(Exception e){
	      	  e.printStackTrace();		      	  
	      	}finally {
	            httpClient.getConnectionManager().shutdown();
	        }     	        
	  }     	      		  
	  br.close();
	  File file = new File(datapath[0]);
	  file.delete();
	  return null;
	}catch(Exception e){
	  //e.printStackTrace();
	  //this.exception = e;
      return null;
    }  	
  }
}








/************************Backup codes*************************************/

/*
public void dataupload(){
    	Timer timer = new Timer();   
        timer.schedule(new TimerTaskTest(), 3000, 10000); 
}
*/


/*	public void readline(){
String readOutStr = null;
String fullFilename = Environment.getExternalStorageDirectory().getPath() + File.separator + "tmp" +  File.separator + "cache" + File.separator + "20141010_133805.txt";
try {
    BufferedReader bufReader = new BufferedReader(new FileReader(fullFilename));
    String line = "";
    while( ( line = bufReader.readLine() ) != null)
    {
    	
    	readOutStr = line;
    }
    bufReader.close();
     
    Log.d("readFileContentStr", "Successfully to read out string from file "+ fullFilename);
} catch (IOException e) {
    readOutStr = null;     
    //e.printStackTrace();
    Log.d("readFileContentStr", "Fail to read out string from file "+ fullFilename);
}
 
teststr= readOutStr;
textView.setText(teststr);

}*/


/* public void senddata(){
//read txt file
String datapath = File.separator + "tmp" +  File.separator + "cache"+ File.separator + "20141010_133805.txt";    	 
//HttpClient httpClient = new DefaultHttpClient();
String  thisLine = null;
try{
  // open input stream test.txt for reading purpose.
  BufferedReader br = new BufferedReader(new FileReader(datapath));
  //BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(datapath), "UTF-8"));
  //thisLine = br.readLine();
  //teststr=thisLine;    	  
  while ((thisLine = br.readLine()) != null) {
	  //System.out.println(thisLine);
	  //textView.setText(thisLine);
	  String obj = thisLine.replaceAll("\'","\"");    		  
	  obj = obj.replaceAll("\"x\"","\"peb_a_x\"");
	  obj = obj.replaceAll("\"y\"","\"peb_a_y\"");
	  obj = obj.replaceAll("\"z\"","\"peb_a_z\"");
	  obj = obj.replaceAll("\"a\"","\"peb_a_a\"");
	  obj = obj.replaceAll("\"t\"","\"peb_time\"");
	  obj = obj.replaceAll("\"px\"","\"phone_x\"");
	  obj = obj.replaceAll("\"py\"","\"phone_y\"");
	  obj = obj.replaceAll("\"pz\"","\"phone_z\"");
	  obj = obj.replaceAll("\"ta\"","\"phone_acc_time\"");
	  obj = obj.replaceAll("\"us\"","\"user_phone\"");
	  obj = obj.replaceAll("\"pb\"","\"user_peb\"");   
	  //obj = obj.replaceAll("\"","\\\\\"");
	  // textView.setText(obj);   
	  //textView.append("000"); 
	  
	  JSONObject jobj = new JSONObject(obj); //build json object   		  
	 // textView.append(jobj.getString("user_peb")); ////test if the json object is correctly built
	  
	  HttpParams httpParameters = new BasicHttpParams();   
	  HttpConnectionParams.setConnectionTimeout(httpParameters, 5*1000);  //set connection timeout
	  HttpConnectionParams.setSoTimeout(httpParameters, 5*1000); 
	  HttpConnectionParams.setSocketBufferSize(httpParameters, 8192);  
	  HttpClient httpClient = new DefaultHttpClient(httpParameters); //built httpclient object
	  //HttpClient httpClient = new DefaultHttpClient();
	  //Scanner in = null;
	  //textView.append("111");
	  //HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 100000);
        try {
            HttpPost httpPost = new HttpPost("http://138.100.72.5/pebbledatamanagement.php");    	            
           
             **method 1: add each value separately**
            List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>();      	            
            nameValuePair.add(new BasicNameValuePair("peb_a_x", jobj.getString("peb_a_x"))); 
            nameValuePair.add(new BasicNameValuePair("peb_a_y", jobj.getString("peb_a_y")));
            nameValuePair.add(new BasicNameValuePair("peb_a_z", jobj.getString("peb_a_z")));
            nameValuePair.add(new BasicNameValuePair("peb_a_a", jobj.getString("peb_a_a")));
            nameValuePair.add(new BasicNameValuePair("peb_time", jobj.getString("peb_time")));
            nameValuePair.add(new BasicNameValuePair("lat", jobj.getString("lat")));
            nameValuePair.add(new BasicNameValuePair("lng", jobj.getString("lng")));
            nameValuePair.add(new BasicNameValuePair("alt", jobj.getString("alt")));
            nameValuePair.add(new BasicNameValuePair("acc", jobj.getString("acc")));
            nameValuePair.add(new BasicNameValuePair("t_gps", jobj.getString("t_gps")));
            nameValuePair.add(new BasicNameValuePair("phone_x", jobj.getString("phone_x")));
            nameValuePair.add(new BasicNameValuePair("phone_y", jobj.getString("phone_y")));
            nameValuePair.add(new BasicNameValuePair("phone_z", jobj.getString("phone_z")));
            nameValuePair.add(new BasicNameValuePair("peb_a_x", jobj.getString("peb_a_x")));    	            
            nameValuePair.add(new BasicNameValuePair("phone_acc_time", jobj.getString("phone_acc_time")));
            nameValuePair.add(new BasicNameValuePair("op", jobj.getString("op")));
            nameValuePair.add(new BasicNameValuePair("user_phone", jobj.getString("user_phone")));
            nameValuePair.add(new BasicNameValuePair("user_peb", jobj.getString("user_peb")));
            nameValuePair.add(new BasicNameValuePair("pass", jobj.getString("pass")));   	            
            
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair)); 
            textView.append("333");
            HttpResponse httpResponse = httpClient.execute(httpPost);  	
            
            
            
            *//****method2: send the json as whole***//*
            //textView.append(jobj.toString());
            //textView.append("222");
            //StringEntity se = new StringEntity("jsonString="+jobj.toString());
            StringEntity se = new StringEntity(jobj.toString(), "utf-8");
            httpPost.setEntity(se);
            httpPost.addHeader("content-type", "application/json; charset=utf8;");
            httpPost.setHeader("Accept", "application/json");
                	            
            
            Header[] headers = httpPost.getAllHeaders();
            String content = EntityUtils.toString(se);
            textView.append(httpPost.toString());
            for (Header header : headers) {
            	textView.append(header.getName() + ": " + header.getValue());
            }
            textView.append(content);
            
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String httpResponse = httpClient.execute(httpPost, responseHandler);
            //HttpResponse httpResponse = httpClient.execute(httpPost);	                
            
            //String resFromServer = org.apache.http.util.EntityUtils.toString(httpResponse.getEntity());
            textView.append(httpResponse);
            //textView.setText(resFromServer);
            //int responseCode = httpResponse.getStatusLine().getStatusCode();    	            
            //textView.append(Integer.toString(responseCode));
            
            //httpResponse.close();
            textView.append("after");  	            

        }catch(Exception e){
      	  e.printStackTrace();
      	  textView.append(e.getLocalizedMessage());
      	}finally {
            httpClient.getConnectionManager().shutdown();
            textView.append("finally");
        }     	        
  }     	      		  
  br.close();
}catch(Exception e){
  e.printStackTrace();
}  	   
    	
}   */
/**Android lifecycle***
 * START:onCreate()->onStart()->onResume
 * BACK key: onPause()->onStop()->onDestory()
 * HOME key: onPause()->onStop() when come back: onRestart()->onStart()->onResume()
 * 
 */
/*
Sensor.manager.SENSOR_DELAY_FASTEST ：0ms
Sensor.manager.SENSOR_DELAY_GAME ：20ms
Sensor.manager.SENSOR_DELAY_UI ：60ms
Sensor.manager.SENSOR_DELAY_NORMAL ：200ms
*/

/*Only send byte array
    short ax = (short)((accdata[0] << 8) | accdata[1]);
    short ay = (short)((accdata[2] << 8) | accdata[3]);
    short az = (short)((accdata[4] << 8) | accdata[5]);
 short aa = (short)((accdata[6] << 8) | accdata[7]);  
 
 byte[] timearray = Arrays.copyOfRange(accdata, 8, 16); 
 BigInteger at_big = new BigInteger(1, timearray);
 long at=at_big.longValue();
 */
	/*long at = 0;
for (int i = 0; i < 8; i++)
	 {
	    at += ((long) accdata[i+8] & 0xffL) << (64 + 8 * i);
	 }*/
//String dataString = "{'x':" + Short.toString(ax) + "'y':" + Short.toString(ay) + "'z':" + Short.toString(az) + "'a':" +Short.toString(aa) + "'t':" + Long.toString(at) +"}";
