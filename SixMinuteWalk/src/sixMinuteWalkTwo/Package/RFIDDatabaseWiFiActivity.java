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
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RFIDDatabaseWiFiActivity extends Activity implements OnInitListener {
	
	//TTS object
    private TextToSpeech myTTS;
    //status check code
    private static final int MY_DATA_CHECK_CODE = 0;
	
	Button buttonConnect_registeration;//(dis)connect Button
	Button buttonSave;//(dis)connect Button
	
    public NetworkTaskRegisteration networktask_registeration;//networktask is the included class to handle the socketconnection

    public Timer checkConnection_timer = null;
    boolean Connect_flag_registeration = false;
    static int Fail_Count_Registeration = 0;
    final int Fail_Threshold = 100;
    boolean RegisterationConnected = false;
    
    // -------------------- Declarations of variables for measurement - begin -----------------    
    private EditText editUserFirstName;
    private EditText editUserLastName;
    private EditText editAge;
    private EditText editGender;
    private EditText editDescription;
    
    public String User_ID = null;
    public String User_First_Name = null;
    public String User_Last_Name = null;
    public String Age = null;
    public String Gender = null;
    public String Description = null;
    
    public DatabaseHandler db;
    
    private TextView TextRegisterationPoint = null; 
    private TextView TextID = null;
    
    // -------------------- Declarations of variables for measurement - end -----------------
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.database);
        
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
        
        //connect the view and the objects
	    buttonConnect_registeration = (Button)findViewById(R.id.connect_registeration);
	    buttonSave = (Button)findViewById(R.id.Save);
	    
	    //add Eventlisteners
	    buttonConnect_registeration.setOnClickListener(buttonConnectRegisterationOnClickListener);
        networktask_registeration = new NetworkTaskRegisteration();//Create initial instance so SendDataToNetwork doesn't throw an error.
        
        buttonSave.setOnClickListener(buttonSaveOnClickListener);
        
        editUserFirstName = (EditText)findViewById(R.id.edit_User_First);
        editUserLastName = (EditText)findViewById(R.id.edit_User_Last);
        editAge = (EditText)findViewById(R.id.edit_Age);
        editGender = (EditText)findViewById(R.id.edit_Gender);
        editDescription = (EditText)findViewById(R.id.edit_Des);
        
        TextRegisterationPoint = (TextView)findViewById(R.id.ConnectionRegisterationPoint);
        TextID = (TextView)findViewById(R.id.CurrentID);
        
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
								if (Connect_flag_registeration) 
								{
									try {
										if(!networktask_registeration.nsocket.getInetAddress().isReachable(100)) {
											if ((++Fail_Count_Registeration) >= Fail_Threshold) {
												runOnUiThread(new Runnable() {
													public void run() {
														// TODO Auto-generated method stub
														changeConnectionStatusRegisteration(false);
													}
												});
											}											
										} else {
											Fail_Count_Registeration = 0;
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
        
        
        db = new DatabaseHandler(this);
        
        
        
    }
    
 // ----------------------- CONNECT BUTTON EVENTLISTENER - end ----------------------------
    
    public void showToast(final String toast)
	{
	    runOnUiThread(new Runnable() {
	        public void run()
	        {
	            Toast.makeText(RFIDDatabaseWiFiActivity.this, toast, Toast.LENGTH_SHORT).show();
	        }
	    });
	}
    
    
 // ----------------------- THE NETWORK TASK - begin ----------------------------
    public class NetworkTaskRegisteration extends AsyncTask<Void, byte[], Boolean> {
        Socket nsocket; //Network Socket
        InputStream nis; //Network Input Stream
        OutputStream nos; //Network Output Stream
        BufferedReader inFromServer;//Buffered reader to store the incoming bytes
        SocketAddress sockaddr;

        @Override
        protected void onPreExecute() {
        	//change the connection status to "connected" when the task is started
        	Connect_flag_registeration = true;
        	changeConnectionStatusRegisteration(true);
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
        	Connect_flag_registeration = false;//change the connection to "disconnected"
        	changeConnectionStatusRegisteration(false);
        }
        
        //Method is called after taskexecution
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
            	showToast("onPostExecute: Completed with an Error.");
            } else {
            	showToast("onPostExecute: Completed.");
            }
            Connect_flag_registeration = false;//change the connection to "disconnected"
        	changeConnectionStatusRegisteration(false);
        }
    }
    // ----------------------- THE NETWORK TASK - end ----------------------------
    
    
 // ----------------------- CONNECT BUTTON EVENTLISTENER - begin ----------------------------
    private OnClickListener buttonConnectRegisterationOnClickListener = new OnClickListener() {
        public void onClick(View v){
        	//ipTail_master = editIp_master.getText().toString();
        	
        	//System.out.println("192.168.0." + ipTail_master);
        	
        	if(!RegisterationConnected){//if not connected
        		
        		networktask_registeration = new NetworkTaskRegisteration(); //New instance of NetworkTask
        		
        		networktask_registeration.sockaddr = new InetSocketAddress("192.168.0.104", 4567);
        		networktask_registeration.execute();
        		
        		
        	}else{
        		
        		if(networktask_registeration!=null){
        			networktask_registeration.closeSocket();
        			networktask_registeration.cancel(true);
        		}
        	}
        }
    };
    
 // ----------------------- CONNECT BUTTON EVENTLISTENER - begin ----------------------------
    private OnClickListener buttonSaveOnClickListener = new OnClickListener() {
        public void onClick(View v){
        	
        	if(User_ID != null) {
        		User_First_Name = editUserFirstName.getText().toString();
        		User_Last_Name = editUserLastName.getText().toString();
            	Age = editAge.getText().toString();
            	Gender = editGender.getText().toString();
            	Description = editDescription.getText().toString();
            	
            	db.addContact(new Contact(User_ID, User_First_Name + " " + User_Last_Name, Integer.parseInt(Age), Gender, Description)); 
            	 
            	showToast("Finish registeration");        	
            	speakWords("Finish registeration for " + User_First_Name + User_Last_Name);
            	
        	}
        	
        }
    };
    
 
    
    // Method changes the connection status
 	public void changeConnectionStatusRegisteration(Boolean isConnected) {
 		RegisterationConnected=isConnected;//change variable
 		if(isConnected){//if connection established
 			showToast("successfully connected to server");//log
 			speakWords("Registeration terminal is connected!");
 			TextRegisterationPoint.setText("Registeration terminal: Online");
 			buttonConnect_registeration.setText("Disconnect from Registeration Terminal");//change Buttontext
 		}else{
 			showToast("disconnected from Server!");//log
 			speakWords("Lost connection to Registeration terminal!");
 			Connect_flag_registeration = false;
 			TextRegisterationPoint.setText("Registeration terminal: Offline");
 			buttonConnect_registeration.setText("Connect To Registeration Terminal");//change Buttontext
 		}
 	}
 	
 	private void signalProcessing(String text) {
        String textToSpeak = null;
        if(text.startsWith("16") || text.startsWith("66") || text.startsWith("6A") ){//16002FDD8D69
        	textToSpeak = text;
        	User_ID = text;
        	runOnUiThread(new Runnable() {
				public void run() {
					// TODO Auto-generated method stub
					TextID.setText("Current ID: " + User_ID);
				}
    			
    		});
        	
        	speakWords("Current ID is " + textToSpeak);
        }
    }
    
    
  //speak the user text
    private void speakWords(String speech) {
            //speak straight away
            myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
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
					.getPath() + "/5 Minutes Walk/";
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
}