package com.example.facefrontier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.Build;
import android.provider.MediaStore;

public class MainActivity extends ActionBarActivity {

	static final int REQUEST_IMAGE_CAPTURE = 1;
	static final int REQUEST_TAKE_PHOTO = 1;
	private static final int ACTION_TAKE_PHOTO_B = 1;
	String mCurrentPhotoPath;
	String mTempPhotoPath;
	int mCurrentPhotoId;
	HttpClient httpClient = new DefaultHttpClient();
	HttpPost httpPost;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}

	}

	public void takePicture(View v) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			// Create the File where the photo should go
			File photoFile = null;
			try {
				photoFile = createImageFile();
			} catch (IOException ex) {
				// Error occurred while creating the File
				ex.printStackTrace();
			}
			// Continue only if the File was successfully created
			if (photoFile != null) {
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(photoFile));
				startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
			}
		}
	}
	
	 private class UploadFile extends AsyncTask<Integer, Integer, JSONObject> {
		 
		 @Override
	     protected JSONObject doInBackground(Integer... exp) {
			 
			 JSONObject jsonOb = null;
	         
	    	 if (mCurrentPhotoPath != null) {
	 			httpPost = new HttpPost("http://katfaces.facefrontier.com:8080/add");
	 			//File file = new File(mCurrentPhotoPath);
	 			
	 			
	 			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
				bmOptions.inSampleSize = 4;
				Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath,
						bmOptions);
				File file = new File(mTempPhotoPath);

			    FileOutputStream fOut = null;
				try {
					fOut = new FileOutputStream(file);
				} catch (FileNotFoundException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}

			    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
			    try {
					fOut.flush();
					fOut.close();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			    
	 			
	 			ContentBody fb = new FileBody(file, "image/jpeg");

	 			// Building post parameters, key and value pair
	 			MultipartEntity multipartEntity = new MultipartEntity();
	 			multipartEntity.addPart("file_data", fb);
	 			
	 			try {
					multipartEntity.addPart("user_id", new StringBody("0"));
					multipartEntity.addPart("source", new StringBody("test_api"));
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}

	 			httpPost.setEntity(multipartEntity);

	 			try {
	 				Log.d("http request", "start upload");
	 				HttpResponse response = httpClient.execute(httpPost);
	 				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
	 				String json = reader.readLine();
	 				try {
						jsonOb = new JSONObject(json);
						jsonOb.put("exp", exp[0]);
						Log.d("response", jsonOb.toString());
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

	 			} catch (ClientProtocolException e) {
	 				// writing exception to log
	 				e.printStackTrace();
	 			} catch (IOException e) {
	 				// writing exception to log
	 				e.printStackTrace();

	 			}
	 		 } else {
	 			 showToast("take a new picture first");
	 		 }
	         return jsonOb;
	     }

	     protected void onPostExecute(JSONObject jsonOb) {
	    	 
	    	 Log.d("postexecute", jsonOb.toString());
	    	 
	    	 if(jsonOb!=null){
	    		 try {
					if(jsonOb.getBoolean("detected")==true){
						Log.d("Face", "found");
						
						mCurrentPhotoId = jsonOb.getInt("face_id");
						
						new TestAge().execute(jsonOb.getInt("exp"));
					} else {
						showToast("No face detected!");
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
	    	 } else {
	    		 Log.d("JSON", "empty");
	    	 }
	     }
	 }
	 
	 private class TestAge extends AsyncTask<Integer, Integer, JSONObject> {
		 
		 JSONObject jsonOb;
		 
		 @Override
	     protected JSONObject doInBackground(Integer... exp) {
	         
	    	 if (mCurrentPhotoId != 0) {
	 			httpPost = new HttpPost("http://katfaces.facefrontier.com:8080/classifier/testClassifier");

	 			// Building post parameters, key and value pair
	 			MultipartEntity multipartEntity = new MultipartEntity();
	 			
	 			try {
					multipartEntity.addPart("exp_id", new StringBody(exp[0]+""));
					multipartEntity.addPart("face_id", new StringBody(mCurrentPhotoId+""));
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}

	 			httpPost.setEntity(multipartEntity);

	 			try {
	 				Log.d("http request", "start upload");
	 				HttpResponse response = httpClient.execute(httpPost);
	 				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
	 				String json = reader.readLine();
	 				try {
						jsonOb = new JSONObject(json);
						jsonOb.put("exp", exp[0]);
						Log.d("repsonse", jsonOb.toString());
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

	 			} catch (ClientProtocolException e) {
	 				// writing exception to log
	 				e.printStackTrace();
	 			} catch (IOException e) {
	 				// writing exception to log
	 				e.printStackTrace();

	 			}
	 		 }
	         return jsonOb;
	     }
		 
		 @Override
		protected void onPostExecute(JSONObject result) {
			 
			JSONObject res  = null;
			String msg = null;
			try {
				res = (JSONObject) result.get(result.get("exp")+"");
				Log.d("new json", res.toString());
				msg = ""+res.get("probability");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			showToast("Probability: "+msg);
		}

	 }
	 
	public void showToast(String msg){
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
	}

	public void testAge(View v) {

		new UploadFile().execute(123);

	}
	
	public void testSex(View v) {

		new UploadFile().execute(92);

	}
	
	public void testAfraid(View v)  {

		new UploadFile().execute(4);

	}

	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		File f = new File(mCurrentPhotoPath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		this.sendBroadcast(mediaScanIntent);
	}

	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		File storageDir = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File image = File.createTempFile(imageFileName, /* prefix */
				".jpg", /* suffix */
				storageDir /* directory */
		);
		
		File tempimage = File.createTempFile(imageFileName+"small", /* prefix */
				".jpg", /* suffix */
				storageDir /* directory */
		);

		// Save a file: path for use with ACTION_VIEW intents
		mCurrentPhotoPath = image.getAbsolutePath();
		mTempPhotoPath = tempimage.getAbsolutePath();
		Log.d("Photo: ", mCurrentPhotoPath);
		return image;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inSampleSize = 4;
			Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath,
					bmOptions);
			ImageView im = (ImageView) findViewById(R.id.image1);
			im.setImageBitmap(imageBitmap);
			galleryAddPic();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
