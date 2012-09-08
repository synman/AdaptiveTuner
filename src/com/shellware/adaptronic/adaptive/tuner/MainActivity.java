package com.shellware.adaptronic.adaptive.tuner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shellware.adaptronic.adaptive.tuner.modbus.CRC16;

public class MainActivity extends Activity {
	
//	static final private String MAC_ADDR = "00:18:DB:00:D6:FA";
	private static final UUID UUID_RFCOMM_GENERIC = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private ConnectedThread connected;
	
	private TextView txtData;
	private ListView lvDevices;
	private RelativeLayout layoutDevices;
	private MenuItem menuConnect;
	private GridView gridData;
	
	private ProgressDialog progress;
	private MessageHandler msgHandler = new MessageHandler(this);
	private DataHandler dataHandler = new DataHandler();
	private Handler refreshHandler = new Handler();
	
	private BluetoothAdapter bt;
	private BluetoothDevice btd;
	private BluetoothSocket bts;
	
	private ArrayAdapter<String> devices;
	private ArrayAdapter<String> dataArray;
	
	private DecimalFormat myFormatter = new DecimalFormat("00");
	private StringBuffer dataBuffer = new StringBuffer(1024);
	
	private boolean doLearningFlags = false;
		
	private final byte[] forty96Register = { 0x01, 0x03, 0x10, 0x00, 0x00, 0x06 };

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        txtData = (TextView) findViewById(R.id.txtData);
        lvDevices = (ListView) findViewById(R.id.lvDevices);
        layoutDevices = (RelativeLayout) findViewById(R.id.layoutDevices);
        gridData = (GridView) findViewById(R.id.gridData);
        
        dataArray = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        gridData.setAdapter(dataArray);
        
        lvDevices.setOnItemClickListener(DevicesClickListener);

//        connect(MAC_ADDR);
//        sendAndReceive();

    }
    
    
    @Override
	protected void onResume() {

    	super.onResume();
	}

    final Runnable RefreshRunnable = new Runnable()
    {
        public void run() 
        {
        	final String data = getDataBuffer();
        	if (data.length() > 0 && data.startsWith("1 3 ")) {
        		// RPM, MAP, MAT, WAT, AUXT, & AFR
        		if (data.contains("1 3 C")) {
        			final String[] buf = data.substring(data.indexOf("1 3 C"), data.length() - 1).split(" ");
            		
        			if (buf.length > 13) {        			
	            		dataArray.clear();
		        		dataArray.add(String.format("RPM\n%d", Integer.parseInt(buf[3] + buf[4], 16)));
		        		dataArray.add(String.format("MAP\n%d", Integer.parseInt(buf[5] + buf[6], 16)));
		        		dataArray.add(String.format("MAT\n%d", Integer.parseInt(buf[7] + buf[8], 16)));
		        		dataArray.add(String.format("WAT\n%d", Integer.parseInt(buf[9] + buf[10], 16)));
		        		dataArray.add(String.format("AFR\n%.1f", Integer.parseInt(buf[13], 16) / 10f));
        			}
	        	}
        	}   
        	
        	int[] crc = CRC16.getCRC(forty96Register, forty96Register.length);
        	final byte[] MESSAGE = { forty96Register[0], 
        							 forty96Register[1], 
        							 forty96Register[2], 
        							 forty96Register[3], 
        							 forty96Register[4], 
        							 forty96Register[5], 
        							 (byte) crc[0], 
        							 (byte) crc[1] };

			if (connected != null && connected.isAlive()) connected.write(MESSAGE);        		
            refreshHandler.postDelayed(this, 250);
        }
    };
    			
//        		String[] vals = data.split(" ");

//        		if (!doLearningFlags) {
//        		if (vals.length > 13) {
//            		dataArray.clear();
//	        		dataArray.add(String.format("RPM\n%d", Integer.parseInt(vals[3] + vals[4], 16)));
//	        		dataArray.add(String.format("MAP\n%d", Integer.parseInt(vals[5] + vals[6], 16)));
//	        		dataArray.add(String.format("MAT\n%d", Integer.parseInt(vals[7] + vals[8], 16)));
//	        		dataArray.add(String.format("WAT\n%d", Integer.parseInt(vals[9] + vals[10], 16)));
//	        		dataArray.add(String.format("AFR\n%.1f", Integer.parseInt(vals[13], 16) / 10f));	 
//        		}
//        		} else {
//        			txtData.setText(vals[4]);
//        		}
//            	
//            	doLearningFlags =! doLearningFlags;
//        		Log.d("Adaptive", data);
//        	}

//        	if (doLearningFlags) {
//            	
//            	final byte[] COMMAND = { 0x01, 0x03, 0x10, 0x32, 0x00, 0x1 };
//            	
//            	int[] crc = CRC16.getCRC(COMMAND, COMMAND.length);
//            	final byte[] MESSAGE = {  0x01, 0x03, 0x10, 0x32, 0x00, 0x1, (byte) crc[0], (byte) crc[1] };
//            	
//    			if (connected != null && connected.isAlive()) connected.write(MESSAGE);
//        		
//        	} else {

//        	}
    		
	private class MessageHandler extends Handler {

    	Context ctx;
    	String title;
    	
    	public MessageHandler(Context ctx) {
    		this.ctx = ctx;
    	}
    	
    	public void setTitle(final String title) {
    		this.title = title;
    	}

		@Override
		public void handleMessage(Message message) {
		
			AlertDialog alert = new AlertDialog.Builder(ctx).create();
			alert.setTitle(title);
			alert.setMessage("\n" + message.getData().getString("message") + "\n");
			alert.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", 
					new DialogInterface.OnClickListener() {	
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			alert.show();
		}	
    }    
    
    private class DataHandler extends Handler {

		@Override
		public void handleMessage(Message message) {
			
			byte[] data = message.getData().getByteArray("data");
			int length = message.getData().getInt("length");
			
			if (length > 0) setDataBuffer(data, length);			
		}
    }
    
    private String getDataBuffer() {
    	synchronized(this) {
			final String ret = dataBuffer.toString();
			dataBuffer.setLength(0);
			return ret.trim();
    	}
    }

    private void setDataBuffer(final byte[] data, final int length) {
    	synchronized(this) {
	        for (int x = 0; x < length; x++) {
//	        	dataBuffer.append(myFormatter.format(data[x]));
	        	dataBuffer.append(String.format("%X ", data[x]));
	        }
    	}
    }
    
    private OnItemClickListener DevicesClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

    	// Get the device MAC address, which is the last 17 chars in the View
            final String[] info = ((TextView) v).getText().toString().split("\n");
            final String name = info[0];
            final String address = info[1];
            
            layoutDevices.setVisibility(View.INVISIBLE);
            connect(name, address);
    	}
    };
    
    private void showDevices() {
    	
    	try {    		
	    	if (bt == null) bt = BluetoothAdapter.getDefaultAdapter();
	    	if (devices == null) devices = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice);
	    	devices.clear();
	    	
	    	Set<BluetoothDevice> pairedDevices = bt.getBondedDevices();
	    	// If there are paired devices
	    	if (pairedDevices.size() > 0) {
	    	    // Loop through paired devices
	    	    for (BluetoothDevice device : pairedDevices) {
	    	        // Add the name and address to an array adapter to show in a ListView
	    	        devices.add(device.getName() + "\n" + device.getAddress());
	    	    }
	    	}
	    	
	    	lvDevices.setAdapter(devices);
    	} catch(Exception ex) {
    		// do nothing
    	}
    	
    	layoutDevices.setVisibility(View.VISIBLE);
    }
    
    
    private void connect(final String name, final String macAddr) {
    	
    	progress = ProgressDialog.show(this, "Bluetooth Connection" , "Connecting to " + name);
    	
    	msgHandler.setTitle(name);
    	Thread doConnect = new ConnectThread(macAddr);
    	doConnect.start();
    }

    private class ConnectThread extends Thread {

    	private String addr;
    	
    	public ConnectThread(final String addr) {
    		super();
    		this.addr = addr;
    	}
    	
		@Override
		public void run() {

	        int counter = 0;
	        
	        while (true) {
		
	        	try {            	
	    	        if (bt == null) bt = BluetoothAdapter.getDefaultAdapter();
	    	        btd = bt.getRemoteDevice(addr);        		
	        	} catch (Exception ex) {
	        		// do nothing -- let it fall thru and eventually crash
	        	}
		        
		        try {
		        	bt.cancelDiscovery();
					bts = btd.createRfcommSocketToServiceRecord(UUID_RFCOMM_GENERIC);
				} catch (IOException e) {
					// try an insecure connection
					try {
						bts = btd.createInsecureRfcommSocketToServiceRecord(UUID_RFCOMM_GENERIC);
					} catch (IOException e1) {
						// increment counter
						counter++;
					}
				}
		        
		        try {
					bts.connect();
					break;
				} catch (IOException e) {
					counter++;
					
			        // bail if we've tried 10 times
			        if (counter >= 10) {
				        progress.dismiss();
				        
				        Bundle b = new Bundle();
				        b.putString("message", "Connection attempt failed");
				        Message msg = new Message();
				        msg.setData(b);
				        
				        msgHandler.sendMessage(msg);
				        return;
			        }
				}
	        }
	        
	        progress.dismiss();
    		menuConnect.setTitle(R.string.menu_disconnect);
    		
    		connected = new ConnectedThread(bts);
    		connected.start();
    		
        	int[] crc = CRC16.getCRC(forty96Register, forty96Register.length);
        	final byte[] MESSAGE = { forty96Register[0], 
        							 forty96Register[1], 
        							 forty96Register[2], 
        							 forty96Register[3], 
        							 forty96Register[4], 
        							 forty96Register[5], 
        							 (byte) crc[0], 
        							 (byte) crc[1] };
        	
			connected.write(MESSAGE);  
			
	    	refreshHandler.postDelayed(RefreshRunnable, 500);
		}
    	
    }
    
    private void disconnect() {
    	
    	refreshHandler.removeCallbacks(RefreshRunnable);
    	dataArray.clear();
    	
		try {
	    	if (connected != null && connected.isAlive()) connected.cancel();
	    	if (bts != null && bts.isConnected()) bts.close();
		} catch (Exception e) {
			// do nothing
		}
		
		btd = null;
		bts = null;		
    }
    
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
     
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
     
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
     
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
     
        public void run() {
            byte[] buffer = new byte[512];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                                        
                    // Send the obtained bytes to the UI activity
			        Bundle b = new Bundle();
			        
			        for (int x = 0; x < bytes; x++) {
			        	Log.d("Adaptive", String.format("%X", buffer[x]));
			        }
			        
			        b.putByteArray("data", buffer);
			        b.putInt("length", bytes);
			        
			        Message msg = new Message();
			        msg.setData(b);
			        
			        dataHandler.sendMessage(msg);
			        
                } catch (IOException e) {
                    break;
                }
            }
        }
     
        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
     
        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void sleep(final int millis) {
    	try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// do nothing
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		
        // Handle item selection
        switch (item.getItemId()) {
	        case android.R.id.home:
	        	return false;
	        case R.id.menu_exit:
	        	System.exit(0);
	        case R.id.menu_connect:
	        	if (menuConnect == null) menuConnect = item;
	        	if (item.getTitle().toString().equalsIgnoreCase(getResources().getString(R.string.menu_connect))) {
	        		showDevices();
	        	} else {
	        		disconnect();
	        		item.setTitle(R.string.menu_connect);
	        	}
	            return false;
        	default:
                return super.onOptionsItemSelected(item);
        }
    }
    
}
