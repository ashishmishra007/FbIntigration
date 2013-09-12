package com.example.myfacebook;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.my.facebookutils.FacebookUtility;
import com.my.facebookutils.FacebookUtility.FacebookCallbackListner;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        FacebookUtility facebookUtility = new FacebookUtility(this, "Your Key");
        try {
			facebookUtility.share(new FacebookCallbackListner() {
				
				@Override
				public void fbCallback(boolean success, String response) {
				Log.d("response", response);
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
}
