package de.jamoo.appgetter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	public ProgressDialog progBar;

	public final static boolean DEBUG = false;
	public final static String TAG = "AppGetter";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button start_button = (Button) findViewById(R.id.button2);
		start_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				start_request();

			}
		});

	}

	public void start_request()
	{
		String pkg = getPackageName();
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setComponent(new ComponentName(pkg,pkg+".RequestActivity"));
		startActivity(intent);

		if(DEBUG)Log.v(TAG,"Intent intent: "+intent);
	}

}