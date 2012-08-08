package org.diacalc.android;

import java.util.Calendar;





import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

@TargetApi(3)
public class CoeffMenu extends Activity {
  
  protected static final String DateFormat = null;
  static final int DIALOG_TIME = 0;
  int myHour = 00;
  int myMinute = 00;
  TextView tvTime;
  
  
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time);
        tvTime = (TextView) findViewById(R.id.tvTime);
        final Calendar c = Calendar.getInstance();
        myHour = c.get(Calendar.HOUR_OF_DAY);
        myMinute = c.get(Calendar.MINUTE);
        updateDisplay();
    }
    private void updateDisplay() {
    	tvTime.setText(
            new StringBuilder()
                    .append(pad(myHour)).append(":")
                    .append(pad(myMinute)));
    }
    private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }
    public void btnMenuCoefTimeOk(View v) {
      showDialog(DIALOG_TIME);
    }
    
    protected Dialog onCreateDialog(int id) {
      if (id == DIALOG_TIME) {
    	  final Calendar c = Calendar.getInstance();
          int myHour = c.get(Calendar.HOUR_OF_DAY);
          int myMinute = c.get(Calendar.MINUTE);

        TimePickerDialog tpd = new TimePickerDialog(this, myCallBack, myHour, myMinute, true);
        return tpd;
      }
      return super.onCreateDialog(id);
    }
    
      
    OnTimeSetListener myCallBack = new OnTimeSetListener() {
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
      myHour = hourOfDay;
      myMinute = minute; 
      updateDisplay();
  
    }
  };
  
}
