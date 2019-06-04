package com.example.filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class FileTransferService extends IntentService {

	private static final int SOCKET_TIMEOUT = 5000;
	public static final String ACTION_SEND_FILE = "com.example.filetransfer.SEND_FILE";
	public static final String ACTION_RECV_FILE = "com.example.filetransfer.RECV_FILE";
	public static final String EXTRA_FILE_PATH = "file_path";
	public static final String EXTRA_GROUP_OWNER_ADDRESS = "host";
	public static final String EXTRA_GROUP_OWNER_PORT = "port";
	
	private static final int FLAG_ERROR = -1;
	private static final int FLAG_SEND_SUCCESS = 1;
	private static final int FLAG_RECV_SUCCESS = 2;
	private static final int FLAG_READY_TO_SERVER = 3;
	private static final int FLAG_TRANSFER_PROGRESS = 4;
	private static final int FLAG_SHOW_NOTIFICATION = 5;
	private static final int FLAG_CANCEL_NOTIFICATION = 6;


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FLAG_ERROR:
                    Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                    break;
                case FLAG_SEND_SUCCESS:
                	complete();
                    Toast.makeText(getApplicationContext(), "send finish", Toast.LENGTH_SHORT).show();
                    break;
                case FLAG_RECV_SUCCESS:
                	complete();
                    Toast.makeText(getApplicationContext(), "recv finish", Toast.LENGTH_SHORT).show();
                    break;
                case FLAG_READY_TO_SERVER:
                    Toast.makeText(getApplicationContext(), "server ready", Toast.LENGTH_SHORT).show();
                    break;
                case FLAG_TRANSFER_PROGRESS:
                    int progress = msg.arg1;
                    builder.setContentTitle("Transfering...")
                            .setProgress(100, progress, false)
                            .setWhen(System.currentTimeMillis());
                    Notification notification = builder.build();
                    notification.flags = Notification.FLAG_AUTO_CANCEL;
                    notificationManager.notify(NOTIFY_ID, notification);
                    break;
				case FLAG_SHOW_NOTIFICATION:
					setNotification();
					break;

            }
        }
    };

	public FileTransferService(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	public FileTransferService() {
		super("FileTransferService");
	}

    @Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		if (ACTION_SEND_FILE.equals(intent.getAction())) {
			String filePath = intent.getExtras().getString(EXTRA_FILE_PATH);
			String host = intent.getExtras().getString(EXTRA_GROUP_OWNER_ADDRESS);
			int port = intent.getExtras().getInt(EXTRA_GROUP_OWNER_PORT);
			handler.sendEmptyMessage(FLAG_SHOW_NOTIFICATION);
			Socket socket = new Socket();
			try {
				socket.bind(null);
				socket.connect(new InetSocketAddress(host, port));
				File file = new File(filePath);
				OutputStream os = socket.getOutputStream();
				os.write(Utils.intToByteArray(file.getName().length()));
				os.write(file.getName().getBytes("UTF-8"));
				os.write(Utils.longToByteArray(file.length()));
				os.flush();
				FileInputStream fis = new FileInputStream(file);
				int len = 0;
				byte[] buffer = new byte[1024];
				long writeLen = 0;
				int progress = 0;
				while ((len = fis.read(buffer)) != -1) {
					os.write(buffer, 0, len);
					writeLen += len;
					int tempProgress = (int) (writeLen * 100 / file.length());
					if (progress != tempProgress) {
						handler.sendMessage(handler.obtainMessage(FLAG_TRANSFER_PROGRESS, tempProgress, 0));
						progress = tempProgress;
					}
				}
				os.flush();
				os.close();
				fis.close();
				handler.sendEmptyMessage(FLAG_SEND_SUCCESS);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				handler.sendEmptyMessage(FLAG_ERROR);
			} finally {
				if (socket.isConnected()) {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		} else if (ACTION_RECV_FILE.equals(intent.getAction())) {
			int port = intent.getExtras().getInt(EXTRA_GROUP_OWNER_PORT);
			try {
				ServerSocket serverSocket = new ServerSocket(port);
				handler.sendEmptyMessage(FLAG_READY_TO_SERVER);
				Socket socket = serverSocket.accept();
				handler.sendEmptyMessage(FLAG_SHOW_NOTIFICATION);
				InputStream is = socket.getInputStream();

				byte[] u4 = new byte[4];
				is.read(u4);
				int nameLen = Utils.byteArrayToInt(u4);
				byte[] nameBytes = new byte[nameLen];
				is.read(nameBytes);
				String filename = new String(nameBytes, "UTF-8");
				File saveFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "FileTransferDownload", filename);
				System.out.println("save to file :" + saveFile.getPath());
				if (!saveFile.getParentFile().exists()) {
					saveFile.getParentFile().mkdir();
				}
                byte[] u8 = new byte[8];
                is.read(u8);
                long fileLen = Utils.byteArrayToLong(u8);
				FileOutputStream fos = new FileOutputStream(saveFile);
				int len = 0;
				long readLen = 0;
				int progress = 0;
				byte[] buffer = new byte[1024*10];
				while ((len = is.read(buffer)) != -1) {
					fos.write(buffer, 0, len);
					readLen += len;
					int tempProgress = (int) (readLen * 100 / fileLen);
                    if (progress != tempProgress) {
                        handler.sendMessage(handler.obtainMessage(FLAG_TRANSFER_PROGRESS, tempProgress, 0));
						progress = tempProgress;
                    }
				}
				System.out.println("readlen="+readLen);
				fos.flush();
				fos.close();
				is.close();
				socket.close();
				serverSocket.close();
				handler.sendEmptyMessage(FLAG_RECV_SUCCESS);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				handler.sendEmptyMessage(FLAG_ERROR);
			}
		}
	}

	private static final int NOTIFY_ID = R.string.app_name;
	private static final String CHANNEL = "transfer";

	private NotificationManager notificationManager;
	private NotificationCompat.Builder builder;

    private void setNotification() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            builder = new NotificationCompat.Builder(this, CHANNEL);
            builder.setContentTitle("Start transfer")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setOngoing(true)
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis());
            notificationManager.notify(NOTIFY_ID, builder.build());
        }
    }

    private void complete() {
        if (builder != null) {
            builder.setContentTitle("Transfer Complete");
            Notification notification = builder.build();
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            notificationManager.notify(NOTIFY_ID, notification);
        }
        stopSelf();
    }

}
