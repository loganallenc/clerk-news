package com.loganfynne.clerk;

import com.loganfynne.clerk.AuthDatabase.authBinder;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ClerkActivity extends Activity {
	String access = null;
	String refresh = null;
	String userId = null;
	String feedId = null;
	String url = "http://sandbox.feedly.com";
	AuthDatabase mService;
	boolean mBound = false;
	boolean started = false;
	boolean received = false;
	IntentFilter filter = new IntentFilter("com.loganfynne.clerk.AuthCall");
	IntentFilter filterTwo = new IntentFilter("com.loganfynne.clerk.AuthFinished");
	
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (!received) {
        		startOAuth();
        		received = true;
        	}
        }
    };
    
    private BroadcastReceiver broadcastReceiverTwo = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	access = intent.getStringExtra("access");
        	userId = intent.getStringExtra("userid");
        	onAccessToken();
        }
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		this.registerReceiver(broadcastReceiver, filter);
		
		this.registerReceiver(broadcastReceiverTwo, filterTwo);
		
		if (!started) {
			Intent servei = new Intent(this, AuthDatabase.class);
			if (access != null) {
				servei.putExtra("access", access);
			}
			bindService(servei, mConnection, Context.BIND_AUTO_CREATE);
			startService(servei);
			started = true;
		}
	}
	
	@Override
	protected void onStart() {
        super.onStart();
        
        if (!mBound) {
        	Intent servei = new Intent(this, AuthDatabase.class);
            bindService(servei, mConnection, Context.BIND_AUTO_CREATE);
            mBound = true;
        }
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            authBinder binder = (authBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    public void startOAuth() {
    	Intent feedi = new Intent(this, FeedlyOAuthActivity.class);
		startActivityForResult(feedi, 0);
    }

	public void onAccessToken() {
		Fragment fragment = new FeedsFragment();
		Bundle bundle = new Bundle();
		bundle.putString("access", access);
		Log.d("access",access);
		bundle.putString("url", url);
		bundle.putString("userid", userId);
		fragment.setArguments(bundle);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		refresh = data.getStringExtra("refresh");
		access = data.getStringExtra("access");
		if (refresh != null && access != null) {
			mService.setRefresh(refresh, access);
		} else {
			Intent intent = new Intent(this, FeedlyOAuthActivity.class);
			startActivityForResult(intent, 0);
		}
	}

	@Override
	protected void onStop() {
        super.onStop();
        
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		this.registerReceiver(broadcastReceiver, filter);
		this.registerReceiver(broadcastReceiverTwo, filterTwo);
	}
	
	@Override
	protected void onPause() {
        super.onPause();
        
        this.unregisterReceiver(broadcastReceiver);
        this.unregisterReceiver(broadcastReceiverTwo);
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
	}
}