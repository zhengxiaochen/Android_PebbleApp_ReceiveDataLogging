package com.pebble.getpebbledata;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity { 	
	private static Boolean WIFI_UPLAOD=false;	
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss"); //Set the format of the .txt file name.           
    TextView textView = null;
    TextView textView1 = null;
    TextView textView2 = null;
    TextView textView3 = null;    
    private String PebbleAddress=getPebbleAddress();    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.activity_main);          
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0")); 
        //start MyService **background service**           
        Intent startIntent = new Intent(this, MyService.class); 
        startIntent.putExtra("WIFI_UPLOAD", WIFI_UPLAOD); //WIFI_UPLOAD: true:only upload under wifi connection; false: all condition
        startService(startIntent); 
    }   
    
    @Override
    protected void onResume() {    	
        super.onResume();                    
        textView = (TextView) findViewById(R.id.textView9);		//Data string field
        textView1 = (TextView) findViewById(R.id.textView3);	//Pebble ID field
        textView2 = (TextView) findViewById(R.id.textView5);	//data receiving Status field
        textView3 = (TextView) findViewById(R.id.textView7);    //Upload status field    
		
        //check SD card exist or not
        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if(isSDPresent) { // if SD-card is present
        	//creat "/tmp/cache" folder
            try {
    			File folder0 = new File(Environment.getExternalStorageDirectory() + File.separator + "tmp");
    			//boolean success = true;
    			if (!folder0.exists()) {
    				textView.append("Step1: SD card found.\nCreating the tmp folder. \n");
    			    folder0.mkdir();
    			}
    			File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "tmp" +  File.separator + "cache");
    			//boolean success = true;
    			if (!folder.exists()) {
    				textView.append("Creating the cache folder. \n");
    			    folder.mkdir();
    			}
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    			textView.append("Can not creat the cache folder, because: "+e.getMessage());
    		}
        } else {
        	textView.setText("ERROR! SD card is not detected!!\n Please insert SD card and try again!!");	
        } 
        
        //***Receive textview information from MyService***
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                    	String UIString = intent.getStringExtra(MyService.DATA_STRING);
                    	String TextView2=intent.getStringExtra(MyService.TEXT_VIEW2);                        
                    	textView.setText(UIString);	
                    	textView2.setText(TextView2);	
                    }
                }, new IntentFilter(MyService.ACTION_UI_BROADCAST)
        );
		textView1.setText(PebbleAddress);        
        textView3.setText(checkwifi());        
        
        //if the checkbox is checked, uploading only via WiFi
        CheckBox repeatChkBx = (CheckBox) findViewById( R.id.checkBox1 );
        if (repeatChkBx.isChecked()) {
        	WIFI_UPLAOD=true;
        }else{
        	WIFI_UPLAOD=false;
        }
        //Monitor the status change of the check box
        repeatChkBx.setOnCheckedChangeListener(new OnCheckedChangeListener()  {  	
        	@Override
        	public void onCheckedChanged(CompoundButton repeatChkBx, boolean isChecked)	{
        		if ( isChecked ) {
        			WIFI_UPLAOD=true;        			
        		}else {
        			WIFI_UPLAOD=false;        	        
        		}
        		textView3.setText(checkwifi());
        		RestartService(WIFI_UPLAOD);  //**update the service
        	} });                 
    }
    
    @Override
    protected void onPause() {
    	super.onPause();        	 
    }
    
    //TODO onExit unregisterReceiver(mDataLogReceiver);
    @Override
    protected void onDestroy() {
    	super.onDestroy();  
    }
        
    @Override
    public void finish (){  
    	//*********Stop MyService***
    	Intent stopIntent = new Intent(this, MyService.class);  
        stopService(stopIntent); 
        //System.out.println("service is stopped!");
        System.exit(0);        
    }        
 
	//****Double click back key to exit activity***	 
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == KeyEvent.KEYCODE_BACK)
        {  
            exitBy2Click();		//调用双击退出函数
        }
		return false;
	}	
	private static Boolean isExit = false;
	private void exitBy2Click() {
		Timer tExit = null;
		if (isExit == false) {
			isExit = true; // 准备退出
			Toast.makeText(this, "Click again to exit", Toast.LENGTH_SHORT).show();
			tExit = new Timer();
			tExit.schedule(new TimerTask() {
				@Override
				public void run() {
					isExit = false; // 取消退出
				}
			}, 2000); // 如果2秒钟内没有按下返回键，则启动定时器取消掉刚才执行的任务
		} else {
			finish();
			System.exit(0);
		}
	}
     
	//Update the background service
	private void RestartService (boolean wifi) {		
		//start MyService           
        Intent startIntent = new Intent(this, MyService.class); 
        startIntent.putExtra("WIFI_UPLOAD", wifi);
        startService(startIntent);
		
	}
	
    //Check wifi connection
    private String checkwifi(){
    	String wifistatus="Uploading stopped!!";
    	CheckBox ckboxwifi = (CheckBox) findViewById( R.id.checkBox1 );
    	boolean checked = ckboxwifi.isChecked();
    	ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
        	if (ni.isConnected()){
        		if (ni.getTypeName().equalsIgnoreCase("WIFI")){
        			wifistatus="Uploading via WiFi...";
                    //wificon=true;
        		}
        		else if (ni.getTypeName().equalsIgnoreCase("MOBILE")){
        			if (checked){
        				wifistatus="Uploading blocked!!";
        			}else{
        				wifistatus="Uploading via 3G/4G!!";                    	
        			}
        			//wificon=false;	
        		}   
        	}
        } 
    	return wifistatus;
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






/*//Creat a new thread to send the data to server. Since Android 4.0, it is not allowed to access Internet in the main thread.
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
    			//System.out.println("Begin sending"); 	    		  
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
	  e.printStackTrace();
	  //this.exception = e;
      return null;
    }  	
  }
}*/


/************************Backup codes*************************************/

/**Android lifecycle***
 * START:onCreate()->onStart()->onResume
 * BACK key: onPause()->onStop()->onDestory()
 * HOME key: onPause()->onStop() when come back: onRestart()->onStart()->onResume()
 * 
 */

/*   //keep the activity running in background when press the back key
@Override  
    public boolean onKeyDown(int keyCode, KeyEvent event) {  
        if (keyCode == KeyEvent.KEYCODE_BACK) {  
            moveTaskToBack(false);  
            return true;  
        }  
        return super.onKeyDown(keyCode, event);  
    }  */

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