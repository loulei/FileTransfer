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

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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

    private MyHandler handler = new MyHandler(this);

	private static class MyHandler extends  Handler{
	    private WeakReference<FileTransferService> fileTransferService;

        public MyHandler(FileTransferService service) {
            fileTransferService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            FileTransferService service = fileTransferService.get();
            if (service != null) {
                switch (msg.what) {
                    case FLAG_ERROR:
                        Toast.makeText(service.getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                        break;
                    case FLAG_SEND_SUCCESS:
                        Toast.makeText(service.getApplicationContext(), "send finish", Toast.LENGTH_SHORT).show();
                        break;
                    case FLAG_RECV_SUCCESS:
                        Toast.makeText(service.getApplicationContext(), "recv finish", Toast.LENGTH_SHORT).show();
                        break;
                    case FLAG_READY_TO_SERVER:
                        Toast.makeText(service.getApplicationContext(), "server ready", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

        }
    }
	
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
			Socket socket = new Socket();
			try {
				socket.bind(null);
				socket.connect(new InetSocketAddress(host, port));
				File file = new File(filePath);
				OutputStream os = socket.getOutputStream();
				os.write(Utils.intToByteArray(file.getName().length()));
				os.write(file.getName().getBytes("UTF-8"));
				os.write(Utils.intToByteArray((int) file.length()));
				FileInputStream fis = new FileInputStream(file);
				int len = 0;
				byte[] buffer = new byte[1024];
				while ((len = fis.read(buffer)) != -1) {
					os.write(buffer, 0, len);
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
				socket.setSoTimeout(SOCKET_TIMEOUT);

				InputStream is = socket.getInputStream();

				byte[] u4 = new byte[4];
				is.read(u4);
				int nameLen = Utils.byteArrayToInt(u4);
				byte[] nameBytes = new byte[nameLen];
				is.read(nameBytes);
				String filename = new String(nameBytes, "UTF-8");
				is.read(u4);
				int fileLen = Utils.byteArrayToInt(u4);
				byte[] fileContent = new byte[fileLen];
				is.read(fileContent);

				File saveFile = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "FileTransferDownload", filename);
				System.out.println("save to file :" + saveFile.getPath());
				if (!saveFile.getParentFile().exists()) {
					saveFile.getParentFile().mkdir();
				}
				FileOutputStream fos = new FileOutputStream(saveFile);
				fos.write(fileContent);
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

}
