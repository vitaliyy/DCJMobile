package org.diacalc.android;

import org.diacalc.android.manager.DatabaseManager;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

public class DCJStart extends Activity{
	private static final String DCJ_TAG = "DCJmobile";
	private static final int DIALOG_ABOUTBOX_ID = 0;
	
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        setContentView(R.layout.main);
        Log.i(DCJ_TAG, "on create!!!");       
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
        	showDialog(DIALOG_ABOUTBOX_ID);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    public void onMenuBtnClick(View v){
    	Intent intent = new Intent();
        intent.setClass(this, MenuForm.class);
        startActivity(intent);
    }
    public void onSettingsBtnClick(View v){
    	Intent intent = new Intent();
        intent.setClass(this, SettingsForm.class);
        startActivity(intent);
        
    }
    public void onProductsBtnClick(View v){
    	Intent intent = new Intent();
        intent.setClass(this, ProdForm.class);
        startActivity(intent);
    }
    @Override
    public Dialog onCreateDialog(int id) {
    	switch(id){
    	case DIALOG_ABOUTBOX_ID:
    			 return createAboutBox();    		
    	default: return null;	
    	}
    }
    
    private Dialog createAboutBox(){
    	Dialog dialog = new Dialog(this);
    	dialog.setContentView(R.layout.aboutbox);
    	dialog.setTitle(getString(R.string.aboutBoxTitle));

    	return dialog;
    }
    @Override
    public void onPause(){
    	super.onPause();
    	if (isFinishing()){
    		DatabaseManager mgr = new DatabaseManager(this);
    		((DataPocket)getApplication()).storeMenuProds(mgr);
    		((DataPocket)getApplication()).storeUser(mgr);
    		((DataPocket)this.getApplication()).setAllPointers2Null();
    	}
    }
    
}