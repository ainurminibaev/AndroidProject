package com.ainurpack.filedownloaddrive;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

public class MainActivity extends Activity  implements android.view.View.OnClickListener{
  static final int REQUEST_ACCOUNT_PICKER = 1;
  static final int REQUEST_AUTHORIZATION = 2;
  static final int CAPTURE_IMAGE = 3;
  static final int PICKFILE_RESULT_CODE = 4;
 static final int ACTION_TAKE_VIDEO = 0;
  
  private Thread senderThread;
  private boolean stopThread;
  private  boolean authWindowOpened;
  private List<MyType> fileList;

  private static Uri fileUri;
  private static Drive service;
  private GoogleAccountCredential credential;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    View openBtn = findViewById(R.id.openFileBtn);
    View takePhotoBtn = findViewById(R.id.takePhotoBtn);
    View takeVideoBtn = findViewById(R.id.takeVideoButton);
    
    openBtn.setOnClickListener(this);
    takePhotoBtn.setOnClickListener(this);
    takeVideoBtn.setOnClickListener(this);
    
    Log.d("OnCreate", "created");
    credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    
    /**
     * Описание runnable для Sender
     */
    fileList =  Collections.synchronizedList(new ArrayList<MyType>());
    senderThread = new Thread(new Sender());
    stopThread = false;
    senderThread.start();
    authWindowOpened = false;
	/**
	 * Конец описания
	 */
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    showToast(String.valueOf(requestCode));
	  switch (requestCode) {
    case ACTION_TAKE_VIDEO:
    		showToast("Video Loading");
    		//fileUri = data.getData();
    		saveFileToDrive();
    	break;     
    case REQUEST_ACCOUNT_PICKER:
      if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
        String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        if (accountName != null) {
          credential.setSelectedAccountName(accountName);
          service = getDriveService(credential);
         // startCameraIntent();
        }
      }
      break;
    case REQUEST_AUTHORIZATION:
      if (resultCode == Activity.RESULT_OK) {
    	  saveFileToDrive();
      } else { 
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
      }
      break;
    case CAPTURE_IMAGE:
      if (resultCode == Activity.RESULT_OK) {
        saveFileToDrive();
      }
      break; 
    case PICKFILE_RESULT_CODE:
    	if(resultCode == Activity.RESULT_OK){
    		fileUri = data.getData();
    		saveFileToDrive();
    	}
    	break;   	
    	
    }
  }

  private void startCameraIntent() {
    String mediaStorageDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES).getPath();
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    fileUri = Uri.fromFile(new java.io.File(mediaStorageDir + java.io.File.separator + "IMG_"
        + timeStamp + ".jpg"));

    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
    startActivityForResult(cameraIntent, CAPTURE_IMAGE);
  }

  private void saveFileToDrive() {
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
          // File's binary content
          java.io.File fileContent = new java.io.File(fileUri.getPath());
          FileContent mediaContent = new FileContent(null, fileContent);

          // File's metadata.
          File body = new File();
          body.setTitle(fileContent.getName());
          body.setMimeType(null);
          Log.d("Load","File Loading: "+fileUri.getPath());
          MyType data = new MyType(mediaContent, body);
          fileList.add(data);
          Log.d("Load","List size: "+fileList.size()+". Last file"+fileList.get(fileList.size()-1).body.getTitle());
          //startCameraIntent();
      }
    });
    t.start();
  }

  private Drive getDriveService(GoogleAccountCredential credential) {
    return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
        .build();
  }

  public void showToast(final String toast) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
      }
    });
  }
  
  
  
  public class Sender implements Runnable{
	  
	@Override
	public void run() {
		while((!stopThread||fileList.size()!=0)){
			if(fileList.size()==0){
				continue;
			}
			int cursorPos = fileList.size()-1;
			MyType data = fileList.get(cursorPos);
			File file=new File();
			try {
				file = service.files().insert(data.getBody(), data.getMediaContent()).execute();
				if (file != null) {
		            showToast("Photo uploaded: " + file.getTitle());
		            showToast("Files in Q:"+(fileList.size()-1));
		            fileList.remove(cursorPos); 
		          }
				
			} 
			 catch (UserRecoverableAuthIOException e) {
		        	showToast("UserRecoverableAuthIOException");
		          if(!authWindowOpened){
		        	  authWindowOpened = true;
		        	  startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
		          }
		         }
			
			catch (IOException e) {
				showToast("Some exception. when uploading");
				fileList.add(data);
				e.printStackTrace();
			}
	          
		}
		showToast("Close the prog");
	}
	  
  }
  
  
  private void openFile(){
	  Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
      intent.setType("file/*");
      startActivityForResult(intent,PICKFILE_RESULT_CODE);
  }
  
  
  private void takeVideo(){
	  String mediaStorageDir = Environment.getExternalStoragePublicDirectory(
		        Environment.DIRECTORY_PICTURES).getPath();
		    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		    fileUri = Uri.fromFile(new java.io.File(mediaStorageDir + java.io.File.separator+ timeStamp + ".mp4"));

	  Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	  takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
	    startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
  }
  
  public void finish(){
	  stopThread = true;
	  super.finish();
  }

@Override
public void onClick(View v) {
	switch(v.getId())
	{
	case R.id.takePhotoBtn:
		Toast.makeText(this, "Photo Taking", Toast.LENGTH_SHORT).show();
		startCameraIntent();
		break;
		
	case R.id.openFileBtn:
		Toast.makeText(this, "File Openining", Toast.LENGTH_SHORT).show();
		openFile();
		break;	
	
	case R.id.takeVideoButton:
	Toast.makeText(this, "Video Taking", Toast.LENGTH_SHORT).show();
	takeVideo();
	break;	
}
}
  
  
}