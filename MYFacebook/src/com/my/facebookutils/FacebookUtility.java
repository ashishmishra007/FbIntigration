package com.my.facebookutils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.URLUtil;

import com.my.facebookutils.Facebook.DialogListener;

/**
 * @author Ashish
 * 
 */
public class FacebookUtility {
	private Activity activity;
	private String facebookAppId = "Your App id";
	private ProgressDialog progressDialog;
	private Facebook mFacebook;

	private final String[] PERMISSIONS = { "publish_checkins,publish_actions,publish_stream,user_photos,user_videos"}; // offline_access

	private final int POST = 0, IMAGE = 1, VIDEO = 2, DIALOG = 3, LOGIN = 4;

	private int METHOD_TYPE = -1;

	private Bundle params;

	private FacebookCallbackListner fbcallbackListner = null;

	/**
	 * @param a
	 *            context of Activity in which you want to use this Utility
	 * @param facebookAppId
	 *            appId of your application which you have registered in
	 *            Facebook
	 */
	public FacebookUtility(Activity a, String facebookAppId) 
	{
		activity = a;
		
		progressDialog = new ProgressDialog(activity);
		progressDialog.setMessage("Updating Status");
		
		params = new Bundle();
		this.facebookAppId = facebookAppId;
		mFacebook = new Facebook(this.facebookAppId);
		
		// 187943967969061 my appid
		// Visit http://www.facebook.com/developers/createapp.php to create your
		// own Facebook App id and replace the one given here.
	}

	public boolean autheticateData() 
	{
		boolean success = false;
		try 
		{
			    success = SessionStore.restore(mFacebook, activity);
			
			   /* long l = mFacebook.getAccessExpires();
		        if(l>0)
		        {
			        Calendar calendar = Calendar.getInstance();
			        calendar.setTimeInMillis(l);
			        
			        int hour = calendar.get(Calendar.HOUR);
			        int hour24 = calendar.get(Calendar.HOUR_OF_DAY);
			        int min = calendar.get(Calendar.MINUTE);
			        Log.d("time", ""+l);
		        }*/
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return success;
	}

	public void facebooklogin(FacebookCallbackListner fbcallbacklistner) {
		this.METHOD_TYPE = LOGIN;
		this.fbcallbackListner = fbcallbacklistner;
		fblogin();
	}

	private void fblogin()
	{
		if (isNetworkAvailable()) 
		{
			if (!autheticateData()) 
			{
				mFacebook.authorize(activity, PERMISSIONS, -1,new FbLoginDialogListener());
			} 
			else
			{
				if ((METHOD_TYPE == LOGIN) && (this.fbcallbackListner != null)) 
				{
				    this.fbcallbackListner.fbCallback(false, "Already Logged In");
				}
			}
		} 
		else if(this.fbcallbackListner != null)
		{
		    this.fbcallbackListner.fbCallback(false, "No network connection available.");
		}
	}

	/**
	 * @return logout from your facebook session
	 */
	public void logout(FacebookCallbackListner facebookcallback) {
		this.fbcallbackListner = facebookcallback;
		LogoutTask logouttask = new LogoutTask();
		logouttask.execute();
	}

	
	private class LogoutTask extends AsyncTask<Object, Object, String>
	{

		@Override
		protected String doInBackground(Object... params) 
		{
			return fbLogout();
		}
		
		@Override
		protected void onPostExecute(String result) 
		{
			super.onPostExecute(result);
			if(fbcallbackListner!=null)
		    fbcallbackListner.fbCallback(result.contains("successfully"), result);
		}
	}
	
	
	private String fbLogout() 
	{
        String response = "You are not logged in";		
		if (mFacebook != null) 
		{
			if (autheticateData()) 
			{
				try 
				{
					SessionStore.clear(activity);
					mFacebook.logout(activity);
					response = "Logged out successfully";
				} catch (Exception ex) 
				{
				   response = ex.getMessage();
				}
			} 
		} 
		return response;
	}

	private final class FbLoginDialogListener implements DialogListener 
	{
		public void onComplete(Bundle values) 
		{
			//mFacebook.setAccessExpires(0);
			SessionStore.save(mFacebook, activity);

			switch (METHOD_TYPE) {
			case POST:
				fbPost();
				break;

			case IMAGE:
				fbUploadImageToALbum();
				break;

			case VIDEO:
				fbUploadVideoToAlbum();
				break;

			case DIALOG:
				fbdialog();
				break;

			case LOGIN:   
				if((METHOD_TYPE == LOGIN)&& (fbcallbackListner!=null))
				{
					fbcallbackListner.fbCallback(true,"Logged in successfully");
			    }
				break;		
			}

		}

		public void onFacebookError(FacebookError error) 
		{
			if (fbcallbackListner != null) 
			{
				fbcallbackListner.fbCallback(false,error.getMessage());
			}
		}

		public void onError(DialogError error) {
			if (fbcallbackListner != null) {
				fbcallbackListner.fbCallback(false,error.getFailingUrl());
			}
		}

		public void onCancel() {

			if (fbcallbackListner != null) 
			{
				 fbcallbackListner.fbCallback(false,"Operation Cancelled");
			}

		}
	}

	private final class WallPostListener extends BaseRequestListener {

		@Override
		public void onComplete(final String response, Object state) {
			
			/*  Check Error Code here */
			
			
			if(progressDialog.isShowing())
				progressDialog.dismiss();
				
			if ((response != null && response.contains("error"))||(response==null)) // {"id":"424647147560692","post_id":"100000462326257_424647167560690"}
			{
				if (fbcallbackListner != null) 
				{
					fbcallbackListner.fbCallback(false,response);
				}

			} 
			else if (fbcallbackListner != null) 
			{
					fbcallbackListner.fbCallback(true,response);
			}
		}

		@Override
		public void onIOException(IOException e, Object state) {

			if(progressDialog.isShowing())
				progressDialog.dismiss();
			
			if (fbcallbackListner != null) 
			{
				fbcallbackListner.fbCallback(false,e.getMessage());
			}
		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e,
				Object state) {

			if(progressDialog.isShowing())
				progressDialog.dismiss();
			
			if (fbcallbackListner != null) {
				fbcallbackListner.fbCallback(false,e.getMessage());
			}
		}

		@Override
		public void onMalformedURLException(MalformedURLException e,
				Object state) {

			if(progressDialog.isShowing())
				progressDialog.dismiss();
			
			if (fbcallbackListner != null)
			{
				fbcallbackListner.fbCallback(false,e.getMessage());
			}
		}

		@Override
		public void onFacebookError(FacebookError e, Object state) {

			if(progressDialog.isShowing())
				progressDialog.dismiss();
			
			if (fbcallbackListner != null) {
				fbcallbackListner.fbCallback(false,e.getMessage());
			}
		}
	}

	private void fbPost() {

		if(!progressDialog.isShowing())
		progressDialog.show();
		
		AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(mFacebook);
		//mAsyncRunner.request("me/photos", params, new WallPostListener());  --> gets wallpaper
		//mAsyncRunner.request("me/feed", params, new WallPostListener());     --> get first 10 posts
		mAsyncRunner.request("me/feed", params, "POST", new WallPostListener(),null);
	}

	public void fbdialog() {
		mFacebook.dialog(activity, "feed", params, new SampleDialogListener());
	}

	private void fbUploadImageToALbum() {

		if(!progressDialog.isShowing())
			progressDialog.show();
			
		
		params.putString("method", "photos.upload");

		AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(mFacebook);
		mAsyncRunner
				.request(null, params, "POST", new WallPostListener(), null);
	}

	private void fbUploadVideoToAlbum() {

		if(!progressDialog.isShowing())
			progressDialog.show();
			
		
		params.putString("method", "videos.upload");

		AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(mFacebook);
		mAsyncRunner
				.request(null, params, "POST", new WallPostListener(), null);
	}

	/**
	 * @param message
	 *            message that is to be posted on your wall
	 * @throws Exception
	 */
	public void post(String message, FacebookCallbackListner fbcallbackListner)
			throws Exception {
		this.fbcallbackListner = fbcallbackListner;
		params.clear();

		params.putString("message", message);

		METHOD_TYPE = POST;

		if (autheticateData()) {
			fbPost();

		} else {
			fblogin();
		}
	}

	
	
	/**
	 * @param linkUrl
	 *            link address which you want to share
	 *
	 * @throws Exception
	 */
	public void shareLinkWithDialog(String linkUrl,FacebookCallbackListner fbcallback) throws Exception {
		this.fbcallbackListner = fbcallback;
		params.clear();

		if(isurlValid(linkUrl))
	   {	
		params.putString("link", linkUrl);
//		params.putString("message", "Sharing Link");		
//		params.putString("name", "");
//		params.putString("caption", "");
//		params.putString("description", "");
//		params.putString("picture", "");

		METHOD_TYPE = DIALOG;

		if (autheticateData()) {
			mFacebook.dialog(activity, "feed", params,
					new SampleDialogListener());
		} else {
			fblogin();
		}
	   }
	  else
	  {
		  if(fbcallbackListner!=null)
		  {
			  fbcallbackListner.fbCallback(false,"Invalid Url format");
		  }
	  }
	}
	
	
	/**
	 * @param message
	 *            message that is to be posted on your wall
	 * @param link
	 *            link address which you want to share
	 * @param linkName
	 *            name of the link address which you have specified above
	 * @throws Exception
	 */
	public void share(FacebookCallbackListner fbcallback) throws Exception {
		this.fbcallbackListner = fbcallback;
		params.clear();

		METHOD_TYPE = DIALOG;

		if (autheticateData()) {
			mFacebook.dialog(activity, "feed", params,
					new SampleDialogListener());
		} else {
			fblogin();
		}
	}

	public class SampleDialogListener extends BaseDialogListener 
	{
		@Override
		public void onComplete(Bundle values) 
		{
			
			/*  Check Error Code here */
			
			boolean success=false;
			String response="";
			
			if(values==null)
			{
				   response = "Not posted to facebook";
			}
			else if(values.containsKey("error_code")||values.containsKey("error_msg"))
			{
				String errorcode = values.getString("error_code");
				String errormessage = values.getString("error_msg");
				
				Log.d("error",errorcode + " "+errormessage);
					//relogin();
			}
			else
			{	
			      //final String postId = values.getString("post_id");
				  success= true;
				  response = values.toString();
				// mAsyncRunner.request(postId, new WallPostRequestListener());
			}
			
			if(fbcallbackListner!=null)
			fbcallbackListner.fbCallback(success, response);
		}
		@Override
		public void onCancel() 
		{ 
			super.onCancel();
			if(fbcallbackListner!=null)
				fbcallbackListner.fbCallback(false, "Dialog cancelled by user");
		}
		@Override
		public void onError(DialogError e) 
		{
			super.onError(e);
			if(fbcallbackListner!=null)
			fbcallbackListner.fbCallback(false, "ERROR:"+e.getErrorCode()+ " "+e.getMessage());
		}
		@Override
		public void onFacebookError(FacebookError e) 
		{
			super.onFacebookError(e);
			if(fbcallbackListner!=null)
			fbcallbackListner.fbCallback(false, "ERROR:"+e.getErrorCode()+ " "+e.getErrorType());
		}
	}

	public class WallPostRequestListener extends BaseRequestListener {

		public void onComplete(final String response, final Object state) 
		{
		}
	}

	
	/**
	 * @param message
	 * @param linkUrl
	 * @param linkCaption
	 * @param linkName
	 * @param linkDescription
	 * @param linkPictureURL
	 * @param fbcallback
	 * @throws Exception
	 */
	public void shareLink(String message, String linkUrl, String linkCaption,
			String linkName, String linkDescription, String linkPictureURL,
			FacebookCallbackListner fbcallback) throws Exception {
		this.fbcallbackListner = fbcallback;
		params.clear();

		if(!isurlValid(linkUrl))
		{
				  if(fbcallbackListner!=null)
				  {
					  fbcallbackListner.fbCallback(false,"Invalid Link Url Format");
				  }
		}
		else
	  {
		params.putString("message", message);
		params.putString("link", linkUrl);
		params.putString("name", linkName);
		params.putString("caption", linkCaption);
		params.putString("description", linkDescription);
		
		if(isurlValid(linkPictureURL))
		params.putString("linkPictureURL", linkPictureURL);

		METHOD_TYPE = POST;

		if (autheticateData()) {
			fbPost();
		} else {
			fblogin();
		}
	  }
	}

	/*private void relogin() {
		try 
		{
			mFacebook.logout(activity);
			mFacebook.authorize(activity, PERMISSIONS, -1,new FbLoginDialogListener());
		}  
		catch (Exception e) 
		{
			e.printStackTrace();
		}		
	}*/

	
	/**
	 * @param imageUrl
	 *            url of the image which you want to share
	 * @param imageCaption
	 *            caption to above image
	 * @param imageDescription
	 *            description about your image
	 * @param message
	 *            message to post on your wall
	 * @throws Exception
	 */
	public void shareImage(String imageUrl, String imageCaption,String message,
			String imageDescription, FacebookCallbackListner fbcallback)
			throws Exception // Working
	{
		
		this.fbcallbackListner = fbcallback;
		params.clear();

		if(isurlValid(imageUrl))
	  {
		
		params.putString("message", message);
		params.putString("picture", imageUrl.toString().trim());
		params.putString("caption", imageCaption);
		params.putString("description", imageDescription);

		METHOD_TYPE = POST;

		if (autheticateData()) {
			fbPost();
		} else {
			fblogin();
		}
	  }
		else
		  {
			  if(fbcallbackListner!=null)
			  {
				  fbcallbackListner.fbCallback(false,"Invalid Url Format");
			  }
		  }

		// AsyncFacebookRunner mAsyncRunner = new
		// AsyncFacebookRunner(mFacebook);
		// mAsyncRunner.request("me/feed", params, "POST", new
		// WallPostListener());
	}

	/**
	 * @param filePath
	 *            path of the image which you want to share
	 * @param imageCaption
	 *            caption to above image
	 * @param imageDescription
	 *            description about your image
	 * @throws Exception
	 */
	public void shareLocalImage(String filePath, String imageCaption,
			String imageDescription, FacebookCallbackListner pfbcallbackListner)
			throws Exception // Working
	{
		this.fbcallbackListner = pfbcallbackListner;

		final String FilePath = filePath;
		final String iCaption = imageCaption;
		final String iDescription = imageDescription;

		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				byte[] byteArray = readBytes(FilePath);

				params.clear();

				params.putByteArray("picture", byteArray);
				params.putString("description", iDescription);
				params.putString("caption", iCaption);

				METHOD_TYPE = IMAGE;

				if (autheticateData()) {
					fbUploadImageToALbum();
				} else {
					fblogin();
				}

			}
		});
	}

	/**
	 * @param filePath
	 *            path of the video which you want to share
	 * @param description
	 *            description about your video
	 * @param caption
	 *            caption to above video
	 * @throws Exception
	 */
	public void shareVideo(String filePath, String description, String caption,
			FacebookCallbackListner fbcallback) throws Exception // working
	{
		this.fbcallbackListner = fbcallback;
		byte[] byteArray = readBytes(filePath);

		params.clear();

		params.putByteArray("video", byteArray);
		params.putString("description", description);
		params.putString("caption", caption);

		METHOD_TYPE = VIDEO;

		if (autheticateData()) {
			fbUploadVideoToAlbum();
		} else {
			fblogin();
		}
	}

	private byte[] readBytes(String filePath) {
		byte[] byteArray = null;

		// This dynamically extends to take the bytes you read.
		ByteArrayOutputStream byteBuffer;
		try {
			File f = new File(filePath);

			InputStream inputStream = new FileInputStream(f);

			byteBuffer = new ByteArrayOutputStream();

			// This is storage overwritten on each iteration with bytes.
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];

			// We need to know how may bytes were read to write them to the
			// byteBuffer.
			int len = 0;
			while ((len = inputStream.read(buffer)) != -1) {
				byteBuffer.write(buffer, 0, len);

				byteArray = byteBuffer.toByteArray();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// And then we can return your byte array.
		return byteArray;
	}

	private boolean isNetworkAvailable() {
		boolean connection = false;
		try {

			ConnectivityManager cm = (ConnectivityManager) activity
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm != null) 
			{
				NetworkInfo net_info = cm.getActiveNetworkInfo();
				if (net_info != null && net_info.isConnected())
					connection = true;
			}

		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return connection;
	}

	private boolean isurlValid(String url)
	{
		return URLUtil.isValidUrl(url);
	}
	
	public interface FacebookCallbackListner
	{
		public abstract void fbCallback(boolean success,String response);
	}
	
}