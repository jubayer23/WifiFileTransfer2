package com.creative.wififiletransfer2;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.creative.wififiletransfer2.model.WifiFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by comsol on 02-May-17.
 */
public class MakeWifiConnection implements WifiP2pManager.ConnectionInfoListener {

    private static MainActivity context;
    private WifiP2pInfo info;
    private static ProgressDialog mProgressDialog;

    MakeWifiConnection(MainActivity context) {

        this.context = context;

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

        this.info = info;
        if (info.groupFormed && info.isGroupOwner) {

            Log.d("DEBUG", "I am Server Means Reveiver");
            new FileServerAsyncTask(context)
                    .execute();
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            Log.d("DEBUG", "I am Client Means Sender");


            initiateTransfer();
        }
    }

    private void initiateTransfer() {
        Uri uri = MainActivity.uri;
        File myFile = new File(uri.getPath());
        WifiFile wifiFile = new WifiFile(myFile.getName(), myFile.length());

        Log.d("DEBUG", "Intent----------- " + uri);
        Intent serviceIntent = new Intent(context, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_OBJ, wifiFile);
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        context.startService(serviceIntent);
        showprogress("Sending...");
    }


    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    static Handler handler;
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {
        private Context context;

        /**
         * @param context
         */
        public FileServerAsyncTask(Context context) {
            this.context = context;
            handler = new Handler();
            if (mProgressDialog == null)
                mProgressDialog = new ProgressDialog(context,
                        ProgressDialog.THEME_HOLO_LIGHT);
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(MainActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(MainActivity.TAG, "Server: connection done");

                ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                WifiFile wifiFile = null;
                try {
                    wifiFile = (WifiFile) ois.readObject();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                final Runnable r = new Runnable() {

                    public void run() {
                        // TODO Auto-generated method stub
                        mProgressDialog.setMessage("Receiving...");
                        mProgressDialog.setIndeterminate(false);
                        mProgressDialog.setMax(100);
                        mProgressDialog.setProgress(0);
                        mProgressDialog.setProgressNumberFormat(null);
//						mProgressDialog.setCancelable(false);
                        mProgressDialog
                                .setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        mProgressDialog.show();
                    }
                };
                handler.post(r);


                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/JUBAYER3/" + wifiFile.getFileName());
                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                Log.d(MainActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f),wifiFile.getFileLength());
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {

        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out, long fileLength) {

        byte buf[] = new byte[1024];
        long total = 0;
        int Percentage = 0;
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                try {
                    total += len;
                    if (fileLength > 0) {
                        Percentage = (int) ((total * 100) / fileLength);
                    }
                    // Log.e("Percentage--->>> ", Percentage+"   FileLength" +
                    // EncryptedFilelength+"    len" + len+"");
                    mProgressDialog.setProgress(Percentage);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                    dismissDialog();
                }
            }

            dismissDialog();

            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(MainActivity.TAG, e.toString());
            return false;
        }

        context.resetData();

        return true;
    }



    public void showprogress(final String task) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(context,
                    ProgressDialog.THEME_HOLO_LIGHT);
        }
        Handler handle = new Handler();
        final Runnable send = new Runnable() {

            public void run() {
                // TODO Auto-generated method stub
                mProgressDialog.setMessage(task);
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax(100);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setProgressNumberFormat(null);
                mProgressDialog
                        .setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
            }
        };
        handle.post(send);
    }

    private static void dismissDialog() {
        if (mProgressDialog != null) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
    }
}
