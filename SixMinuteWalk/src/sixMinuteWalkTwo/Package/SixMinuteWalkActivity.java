/*
 * Bidirectional Android -> Arduino TCP Client
 * 
 * 28.04.2012
 * by Laurid Meyer
 * 
 * http://www.lauridmeyer.com
 * 
 */
package sixMinuteWalkTwo.Package;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SixMinuteWalkActivity extends Activity implements OnInitListener {
	
	//TTS object
    private TextToSpeech myTTS;
    //status check code
    private static final int MY_DATA_CHECK_CODE = 0;
	
	EditText editIp_start;
	EditText editIp_return;
	
	Button buttonConnect_start;//(dis)connect Button
	Button buttonConnect_return;//(dis)connect Button
	
	EditText edit_time;
	EditText edit_distance;
	
	Button buttonSave_Configuration;//(dis)connect Button
	
	
    NetworkTaskStart networktask_start;//networktask is the included class to handle the socketconnection
    NetworkTaskReturn networktask_return;//networktask is the included class to handle the socketconnection
    
    String ipTail_start = "108";
    String ipTail_return = "102";
    public Timer checkConnection_timer = null;
    boolean Connect_flag_start = false;
    boolean Connect_flag_return = false;
    static int Fail_Count_Start = 0;
    static int Fail_Count_Return = 0;
    final int Fail_Threshold = 20;
    boolean StartConnected = false;
    boolean ReturnConnected = false;
    
    // -------------------- Declarations of variables for measurement - begin -----------------
    private boolean startFlag = false;
    
    final static private long ONE_MINUTE = 60000;
    private long disOfPath = 30;
    private long travelTime = 6;
    private long FINAL_MINUTES = ONE_MINUTE * travelTime;
    
    private PendingIntent pi;
    private BroadcastReceiver br;
    private AlarmManager am;
    
    private int travelWholeCount = 0;
    private float traveledDistance = 0.0f;
    private float AverageSpeed = 0.0f;
    
    private long startTimeMill = 0;
    private long currentTimeMill = 0;
    private long lastCheckedTimeMill = 0;
    
    
    private String strfinalDistance = null;
    private String strfinalAverSpeed = null;
    private String strTimeTraveled = null;
    
    public String User_Name = null;
    
    public int Selected_Time = 6;
    
    private TextView TextTraveledDistance = null;
    private TextView TextAverSpeed = null;
    private TextView TextTimeTraveled = null;
    private TextView TextFinalDistance = null;
    private TextView TextTestStatus = null;
    private TextView TextWalkingDirection = null;
    private TextView TextResult = null;
    private TextView TextStartPoint = null;
    private TextView TextReturnPoint = null;    
    
    
    
    // -------------------- Declarations of variables for measurement - end -----------------
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
        
        //connect the view and the objects
	    buttonConnect_start = (Button)findViewById(R.id.connect_start);
	    buttonConnect_return = (Button)findViewById(R.id.connect_return);
	    
	    editIp_start = (EditText)findViewById(R.id.editIpTail_start);
	    editIp_return = (EditText)findViewById(R.id.editIpTail_return);
	    
	    // configuration
	    buttonSave_Configuration = (Button)findViewById(R.id.btn_save);
	    
	    edit_time = (EditText)findViewById(R.id.edit_time);
	    edit_distance = (EditText)findViewById(R.id.edit_distance);
	    
	    //changeConnectionStatusStart(false);//change connectionstatus to "disconnected"
	    //changeConnectionStatusReturn(false);//change connectionstatus to "disconnected"
	    
	    //add Eventlisteners
	    buttonConnect_start.setOnClickListener(buttonConnectStartOnClickListener);
	    buttonConnect_return.setOnClickListener(buttonConnectReturnOnClickListener);
	    
	    buttonSave_Configuration.setOnClickListener(buttonSaveConfigurationOnClickListener);
	    
	    TextTraveledDistance = (TextView)findViewById(R.id.Traveled_Distance);
		TextAverSpeed = (TextView)findViewById(R.id.Average_Speed);
		TextTimeTraveled = (TextView)findViewById(R.id.Remaining_Time);
		TextFinalDistance = (TextView)findViewById(R.id.Final_Distance);
		TextTestStatus = (TextView)findViewById(R.id.TestStatus);
		TextWalkingDirection = (TextView)findViewById(R.id.WalkingDirection);
		TextResult = (TextView)findViewById(R.id.Result);
		TextStartPoint = (TextView)findViewById(R.id.ConnectionStartPoint);
		TextReturnPoint = (TextView)findViewById(R.id.ConnectionReturnPoint);	    
	    
        networktask_start = new NetworkTaskStart();//Create initial instance so SendDataToNetwork doesn't throw an error.
        networktask_return = new NetworkTaskReturn();//Create initial instance so SendDataToNetwork doesn't throw an error.
        
        checkConnection_timer = new Timer();
        checkConnection_timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable() {
					public void run() {
						// TODO Auto-generated method stub
						new Thread(new Runnable() {
							public void run() {
								if (Connect_flag_start) 
								{
									try {
										if(!networktask_start.nsocket.getInetAddress().isReachable(100)) {
											if ((++Fail_Count_Start) >= Fail_Threshold) {
												runOnUiThread(new Runnable() {
													public void run() {
														// TODO Auto-generated method stub
														changeConnectionStatusStart(false);
													}
												});
											}											
										} else {
											Fail_Count_Start = 0;
										}
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								
								if (Connect_flag_return)
								{	
									try {
										if(!networktask_return.nsocket.getInetAddress().isReachable(100)) {
											if ((++Fail_Count_Return) >= Fail_Threshold) {
												runOnUiThread(new Runnable() {
													public void run() {
														// TODO Auto-generated method stub
														changeConnectionStatusReturn(false);
													}
												});
											}
										} else {
											Fail_Count_Return = 0;
										}
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
						}).start();
					}

				});
			}
		}, 0, 1000);
        
        
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
            	speakWords("Time is up");
            	startFlag = false;
                
            	Calendar rightNow = Calendar.getInstance();
				// offset to add since we're not UTC
				long offset = rightNow.get(Calendar.ZONE_OFFSET) +
				    rightNow.get(Calendar.DST_OFFSET);
				long finalTimeMill = (rightNow.getTimeInMillis() + offset) %
				    (24 * 60 * 60 * 1000);
					
				long temp = finalTimeMill - lastCheckedTimeMill;
				
				double temp_final = 0.0f;
				if ((AverageSpeed * temp / 1000) >= disOfPath) {
					temp_final = disOfPath;
				} else {
					temp_final = AverageSpeed * temp / 1000;
				}
				
				double finalDistance = travelWholeCount * disOfPath + temp_final;
				double finalAverageSpeed = finalDistance / (60 * travelTime);
				strfinalDistance = new DecimalFormat("###.##").format(finalDistance);
				strfinalAverSpeed = new DecimalFormat("##.##").format(finalAverageSpeed);
				runOnUiThread(new Runnable() {
					public void run() {
	   					// TODO Auto-generated method stub
	   					TextFinalDistance.setText("Final Distance: " + strfinalDistance + " meters");
	   					TextAverSpeed.setText("Average Speed: " + strfinalAverSpeed + " meters/s");
	   					TextTimeTraveled.setText("Time Traveled:" + travelTime + " minutes");
	   					TextResult.setText("Result: " + strfinalDistance);
	   				}
				});
				speakWords("Your Final Traveled Distance is " + strfinalDistance + "meters" + " and your Final Average Speed is " + strfinalAverSpeed + "meters per second");
				writeFileToSD(User_Name + ".txt", "\nFinal Distance: " + strfinalDistance + " meters\n" + "Average Speed: " + strfinalAverSpeed + " meters/s\n" + "Time Traveled:" + travelTime + " minutes\n\n\n\n\n");
				travelWholeCount = 0;
				traveledDistance = 0.0f;
				AverageSpeed = 0.0f;
				
				startTimeMill = 0;
				currentTimeMill = 0;
				lastCheckedTimeMill = 0;
				TextTestStatus.setText("Test Status: OFF");
				TextWalkingDirection.setText("Walking Direction:");
			}
        };
        registerReceiver(br, new IntentFilter("TravelDistance") );
        pi = PendingIntent.getBroadcast( this, 0, new Intent("TravelDistance"), 0);
        am = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
    }
    
 // ----------------------- CONNECT BUTTON EVENTLISTENER - end ----------------------------
    
    public void showToast(final String toast)
	{
	    runOnUiThread(new Runnable() {
	        public void run()
	        {
	            Toast.makeText(SixMinuteWalkActivity.this, toast, Toast.LENGTH_SHORT).show();
	        }
	    });
	}
    
 // ----------------------- CONNECT BUTTON EVENTLISTENER - begin ----------------------------
    private OnClickListener buttonConnectStartOnClickListener = new OnClickListener() {
        public void onClick(View v){
        	ipTail_start = editIp_start.getText().toString();
        	
        	if(!StartConnected){//if not connected
        		
        		networktask_start = new NetworkTaskStart(); //New instance of NetworkTask
        		networktask_start.sockaddr = new InetSocketAddress("192.168.0." + ipTail_start, 4567);
        		networktask_start.execute();
        	}else{
        		
        		if(networktask_start!=null){
        			networktask_start.closeSocket();
        			networktask_start.cancel(true);
        		}
        	}
        }
    };
 
    
    private OnClickListener buttonConnectReturnOnClickListener = new OnClickListener() {
        public void onClick(View v){
        	ipTail_return = editIp_return.getText().toString();
        	
        	if(!ReturnConnected){//if not connected
        		
        		networktask_return = new NetworkTaskReturn(); //New instance of NetworkTask
        		networktask_return.sockaddr = new InetSocketAddress("192.168.0." + ipTail_return, 4567);
        		networktask_return.execute();
        	}else{
        		
        		if(networktask_return!=null){
        			networktask_return.closeSocket();
        			networktask_return.cancel(true);
        		}
        	}
        }
    };
 // ----------------------- CONNECT BUTTON EVENTLISTENER - end ----------------------------
    
 // ----------------------- CONNECT BUTTON EVENTLISTENER - begin ----------------------------
    private OnClickListener buttonSaveConfigurationOnClickListener = new OnClickListener() {
        public void onClick(View v){
        	
        	disOfPath = Long.parseLong(edit_distance.getText().toString());
        	travelTime = Long.parseLong(edit_time.getText().toString());
        	
        	showToast("Set travel time to " + travelTime + "; and path length to " + disOfPath);
        	
        	FINAL_MINUTES = ONE_MINUTE * travelTime;

        }
    };
    
    
 // ----------------------- THE NETWORK TASK - begin ----------------------------
    public class NetworkTaskStart extends AsyncTask<Void, byte[], Boolean> {
        Socket nsocket; //Network Socket
        InputStream nis; //Network Input Stream
        OutputStream nos; //Network Output Stream
        BufferedReader inFromServer;//Buffered reader to store the incoming bytes
        SocketAddress sockaddr;

        @Override
        protected void onPreExecute() {
        	//change the connection status to "connected" when the task is started
        	Connect_flag_start = true;
        	changeConnectionStatusStart(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) { //This runs on a different thread
            boolean result = false;
            try {
            	//create a new socket instance
            	
                nsocket = new Socket();
                nsocket.connect(sockaddr, 5000);//connect and set a 10 second connection timeout
                if (nsocket.isConnected()) {//when connected
                    nis = nsocket.getInputStream();//get input
                    nos = nsocket.getOutputStream();//and output stream from the socket
                    inFromServer = new BufferedReader(new InputStreamReader(nis));//"attach the inputstreamreader"
                    while(true){//while connected
                    	String msgFromServer = inFromServer.readLine();//read the lines coming from the socket
                    	System.out.println(msgFromServer);
                    	byte[] theByteArray = msgFromServer.getBytes();//store the bytes in an array
                    	String Test [] = msgFromServer.split(",");
	    				signalProcessing(Test[0]);
                    }
                }
            //catch exceptions
            } catch (IOException e) {
                e.printStackTrace();
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
                result = true;
            } finally {
            	closeSocket();
            }
            return result;
        }
        
        //Method closes the socket
        public void closeSocket(){
        	try {
                nis.close();
                nos.close();
                nsocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        //Method tries to send Strings over the socket connection
        public void SendDataToNetwork(String cmd) { //You run this from the main thread.
            try {
                if (nsocket.isConnected()) {
                    nos.write(cmd.getBytes());
                } else {
                	showToast("SendDataToNetwork: Cannot send message. Socket is closed");
                }
            } catch (Exception e) {
            	showToast("SendDataToNetwork: Message send failed. Caught an exception");
            }
        }

        //Methods is called everytime a new String is recieved from the socket connection
        @Override
        protected void onProgressUpdate(byte[]... values) {
            if (values.length > 0) {//if the recieved data is at least one byte
                String command=new String(values[0]);//get the String from the recieved bytes
            }
        }
        
        //Method is called when task is cancelled
        @Override
        protected void onCancelled() {
        	Connect_flag_start = false;//change the connection to "disconnected"
        	changeConnectionStatusStart(false);
        }
        
        //Method is called after taskexecution
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
            	showToast("onPostExecute: Completed with an Error.");
            } else {
            	showToast("onPostExecute: Completed.");
            }
            Connect_flag_start = false;//change the connection to "disconnected"
        	changeConnectionStatusStart(false);
        }
    }
    // ----------------------- THE NETWORK TASK - end ----------------------------
    
    
 // ----------------------- THE NETWORK TASK - begin ----------------------------
    public class NetworkTaskReturn extends AsyncTask<Void, byte[], Boolean> {
        Socket nsocket; //Network Socket
        InputStream nis; //Network Input Stream
        OutputStream nos; //Network Output Stream
        BufferedReader inFromServer;//Buffered reader to store the incoming bytes
        SocketAddress sockaddr;
        boolean serverConnected = false;

        @Override
        protected void onPreExecute() {
        	//change the connection status to "connected" when the task is started
        	Connect_flag_return = true;//change the connection to "disconnected"
        	changeConnectionStatusReturn(true);
        }

        @Override
        protected Boolean doInBackground(Void... params) { //This runs on a different thread
            boolean result = false;
            try {
            	//create a new socket instance
            	
                nsocket = new Socket();
                nsocket.connect(sockaddr, 5000);//connect and set a 10 second connection timeout
                if (nsocket.isConnected()) {//when connected
                    nis = nsocket.getInputStream();//get input
                    nos = nsocket.getOutputStream();//and output stream from the socket
                    inFromServer = new BufferedReader(new InputStreamReader(nis));//"attach the inputstreamreader"
                    while(true){//while connected
                    	String msgFromServer = inFromServer.readLine();//read the lines coming from the socket
                    	System.out.println(msgFromServer);
                    	byte[] theByteArray = msgFromServer.getBytes();//store the bytes in an array
                    	//showToast(msgFromServer);
                    	String Test [] = msgFromServer.split(",");
	    				signalProcessing(Test[0]);
                    }
                }
            //catch exceptions
            } catch (IOException e) {
                e.printStackTrace();
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
                result = true;
            } finally {
            	closeSocket();
            }
            return result;
        }
        
        //Method closes the socket
        public void closeSocket(){
        	try {
                nis.close();
                nos.close();
                nsocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        //Method tries to send Strings over the socket connection
        public void SendDataToNetwork(String cmd) { //You run this from the main thread.
            try {
                if (nsocket.isConnected()) {
                    nos.write(cmd.getBytes());
                } else {
                	showToast("SendDataToNetwork: Cannot send message. Socket is closed");
                }
            } catch (Exception e) {
            	showToast("SendDataToNetwork: Message send failed. Caught an exception");
            }
        }

        //Methods is called everytime a new String is recieved from the socket connection
        @Override
        protected void onProgressUpdate(byte[]... values) {
            if (values.length > 0) {//if the recieved data is at least one byte
                String command=new String(values[0]);//get the String from the recieved bytes
            }
        }
        
        //Method is called when task is cancelled
        @Override
        protected void onCancelled() {
        	Connect_flag_return = false;//change the connection to "disconnected"
        	changeConnectionStatusReturn(false);
        }
        
        //Method is called after taskexecution
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
            	showToast("onPostExecute: Completed with an Error.");
            } else {
            	showToast("onPostExecute: Completed.");
            }
            Connect_flag_return = false;//change the connection to "disconnected"
        	changeConnectionStatusReturn(false);
        }
    }
    // ----------------------- THE NETWORK TASK - end ----------------------------
    
    // Method changes the connection status
 	public void changeConnectionStatusStart(Boolean isConnected) {
 		StartConnected=isConnected;//change variable
 		if(isConnected){//if connection established
 			showToast("successfully connected to server");//log
 			speakWords("Start terminal is connected!");
 			TextStartPoint.setText("Start terminal: Online");
 			buttonConnect_start.setText("Disconnect from Start Terminal");//change Buttontext
 		}else{
 			showToast("disconnected from Server!");//log
 			speakWords("Lost connection to start terminal!");
 			Connect_flag_start = false;
 			TextStartPoint.setText("Start terminal: Offline");
 			buttonConnect_start.setText("Connect To Start Terminal");//change Buttontext
 		}
 	}
 	
 	public void changeConnectionStatusReturn(Boolean isConnected) {
 		ReturnConnected=isConnected;//change variable
 		if(isConnected){//if connection established
 			showToast("successfully connected to server");//log
 			speakWords("Return terminal is connected!");
 			TextReturnPoint.setText("Return terminal: Online");
 			buttonConnect_return.setText("Disconnect from Return Terminal");//change Buttontext
 		}else{
 			showToast("disconnected from Server!");//log
 			speakWords("Lost connection to return terminal!");
 			Connect_flag_return = false;
 			TextReturnPoint.setText("Return terminal: Offline");
 			buttonConnect_return.setText("Connect To Return Terminal");//change Buttontext
 		}
 	}
 	
 	private void signalProcessing(String text) {
        String textToSpeak = null;
        
        if(text.equalsIgnoreCase("B1")) {
        	if (!startFlag){
        		textToSpeak = "Measurement is started";
        		am.set( AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 
        				FINAL_MINUTES, pi );
        		 
        		Calendar rightNow = Calendar.getInstance();
        		// offset to add since we're not UTC
        		long offset = rightNow.get(Calendar.ZONE_OFFSET) +
        		    rightNow.get(Calendar.DST_OFFSET);
        		startTimeMill = (rightNow.getTimeInMillis() + offset) %
        		    (24 * 60 * 60 * 1000);

        		lastCheckedTimeMill = startTimeMill;

        		startFlag = true;
        		
        		runOnUiThread(new Runnable() {

					public void run() {
						// TODO Auto-generated method stub
						TextTestStatus.setText("Test Status: ON");
		        		TextWalkingDirection.setText("Walking Direction: Start --> Return");
					}
        			
        		});
        		
        		
        	} else {
        		travelWholeCount++;
        		Calendar rightNow = Calendar.getInstance();
        		// offset to add since we're not UTC
        		long offset = rightNow.get(Calendar.ZONE_OFFSET) +
        		    rightNow.get(Calendar.DST_OFFSET);
        		currentTimeMill = (rightNow.getTimeInMillis() + offset) %
        		    (24 * 60 * 60 * 1000);
        		
        		lastCheckedTimeMill = currentTimeMill;
        		
        		AverageSpeed = (travelWholeCount*(float)disOfPath) / ((currentTimeMill - startTimeMill) / 1000.0f);
        		
        		strTimeTraveled = String.valueOf((currentTimeMill - startTimeMill)/1000) + " seconds";
        		
        		runOnUiThread(new Runnable() {

					public void run() {
						// TODO Auto-generated method stub
						TextTimeTraveled.setText("Time Traveled: " + strTimeTraveled);
		        		TextTraveledDistance.setText("Traveled Distance: " + String.valueOf(travelWholeCount*disOfPath) + " meters");
		        		
		        		String strtempAverSpeed = new DecimalFormat("##.##").format(AverageSpeed);
		        		
		        		TextAverSpeed.setText("Average Speed: " + strtempAverSpeed + "meters/s");
		        		
		        		TextWalkingDirection.setText("Walking Direction: Start --> Return");
					}
        			
        		});
        		
        		textToSpeak = "Button of start point is pushed";
        	}
        }
        
        if(text.equalsIgnoreCase("B2")){
        	if (startFlag){
        		travelWholeCount++;
            	
        		Calendar rightNow = Calendar.getInstance();
        		// offset to add since we're not UTC
        		long offset = rightNow.get(Calendar.ZONE_OFFSET) +
        		    rightNow.get(Calendar.DST_OFFSET);
        		currentTimeMill = (rightNow.getTimeInMillis() + offset) %
        		    (24 * 60 * 60 * 1000);
        		
        		lastCheckedTimeMill = currentTimeMill;
        		
        		AverageSpeed = (travelWholeCount*(float)disOfPath) / ((currentTimeMill - startTimeMill) / 1000.0f);
        		
        		strTimeTraveled = String.valueOf((currentTimeMill - startTimeMill)/1000) + " seconds";
        		
        		runOnUiThread(new Runnable() {

    				public void run() {
    					// TODO Auto-generated method stub
    					TextTimeTraveled.setText("Time Traveled: " + strTimeTraveled);
    		    		TextTraveledDistance.setText("Traveled Distance: " + String.valueOf(travelWholeCount*disOfPath) + " meters");
    		    		String strtempAverSpeed = new DecimalFormat("##.##").format(AverageSpeed);
    	        		
    	        		TextAverSpeed.setText("Average Speed: " + strtempAverSpeed + " meters/s");
    	        		
    	        		TextWalkingDirection.setText("Walking Direction: Return --> Start");
    				}
        			
        		});

            	textToSpeak = "Button of return point is pushed";
        	}
        	
        }
        
        if(text.equalsIgnoreCase("Mickey")){
        	if (!startFlag){
        		
        		User_Name = "Mickey";
        		textToSpeak = "Mickey, Please go ahead to push the button and start to walk";
            	
            	runOnUiThread(new Runnable() {

    				public void run() {
    					// TODO Auto-generated method stub
    					TextTimeTraveled.setText("Time Traveled: ");
    		    		TextTraveledDistance.setText("Traveled Distance: ");	        		
    	        		TextAverSpeed.setText("Average Speed: ");
    	        		TextFinalDistance.setText("Final Distance: ");
    				}
        			
        		});
        	}
        }
        
        if(text.equalsIgnoreCase("Minny")){
        	if (!startFlag){
        		User_Name = "Minny";
        		textToSpeak = "Minny, Please go ahead to push the button and start to walk";
            	
            	runOnUiThread(new Runnable() {

    				public void run() {
    					// TODO Auto-generated method stub
    					TextTimeTraveled.setText("Time Traveled: ");
    		    		TextTraveledDistance.setText("Traveled Distance: ");	        		
    	        		TextAverSpeed.setText("Average Speed: ");
    	        		TextFinalDistance.setText("Final Distance: ");
    				}
        			
        		});
        	}
        }
        
    	speakWords(textToSpeak);
    }
    
    
  //speak the user text
    private void speakWords(String speech) {
            //speak straight away
            myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
    }
 	 	
 	//Method is called when app is closed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        am.cancel(pi);
	    unregisterReceiver(br);
        if(networktask_start!=null){//In case the task is currently running
        	networktask_start.cancel(true);//cancel the task
		}
        if(networktask_return!=null){//In case the task is currently running
        	networktask_return.cancel(true);//cancel the task
		}
    }
    
    public static boolean isNumeric(String str) {  
		try  
		{  
			double d = Double.parseDouble(str);  
		}  
		catch(NumberFormatException nfe)  
		{  
			return false;  
		}  
		return true;  
	}
    
  //act on result of TTS data check
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode){
    	case MY_DATA_CHECK_CODE:
    		if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                //the user has the necessary data - create the TTS
            myTTS = new TextToSpeech(this, this);
            }
            else {
                    //no data - install it now
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
    		break; 
    	}
        
    }
        //setup TTS
    public void onInit(int initStatus) {
            //check for successful instantiation
        if (initStatus == TextToSpeech.SUCCESS) {
            if(myTTS.isLanguageAvailable(Locale.US)==TextToSpeech.LANG_AVAILABLE)
                myTTS.setLanguage(Locale.US);
        }
        else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
	 * write data to SD card
	 */
	public void writeFileToSD(String File_name, String gait_speed_data) {
		String sdStatus = Environment.getExternalStorageState();
		if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
			Log.d("TestFile", "SD card is not avaiable/writeable right now.");
			return;
		}
		try {
			String pathName = Environment.getExternalStorageDirectory()
					.getPath() + "/Six Minutes Walk/";
			String fileName = File_name;
			File path = new File(pathName);
			File file = new File(pathName + fileName);
			if (!path.exists()) {
				Log.d("TestFile", "Create the path:" + pathName);
				path.mkdir();
			}
			if (!file.exists()) {
				Log.d("TestFile", "Create the file:" + fileName);
				file.createNewFile();
			}
			FileOutputStream stream = new FileOutputStream(file, true);
			// String s = "this is a test string writing to file.";
			byte[] buf = gait_speed_data.getBytes();
			stream.write(buf);
			
			stream.close();

		} catch (Exception e) {
			Log.e("TestFile", "Error on writeFilToSD.");
			e.printStackTrace();
		}
	}
	
	@Override
	  public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return super.onCreateOptionsMenu(menu);
	  }
	  @Override
	  public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    case R.id.opt_about:
	    	showToast("This is the app for six minute walk");
	      break;
	    case R.id.opt_exit:
	    	finish();
	    	break;
	    case R.id.Registration:
	    	startActivity(new Intent(this, RFIDDatabaseWiFiActivity.class));
	    	break;
	    default:
	      return super.onOptionsItemSelected(item);
	    }
	    return true;
	  }
}