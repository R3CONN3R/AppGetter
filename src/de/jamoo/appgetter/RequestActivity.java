package de.jamoo.appgetter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import de.jamoo.appgetter.helpers.AppInfo;

@SuppressWarnings("rawtypes")
public class RequestActivity extends Activity {

	@SuppressWarnings("unchecked")
	private ArrayList<String> list_activities = new ArrayList();
	@SuppressWarnings("unchecked")
	private static ArrayList<AppInfo> list_activities_final = new ArrayList();
	private AppAdapter appInfoAdapter = null;
	private static Context context;
	private ViewSwitcher switcherLoad;
	private final AsyncWorkerList taskList = new AsyncWorkerList();
	private Toast toast;
	private Typeface tf;
	private static final int BUFFER = 2048;
	private static final String SD = Environment.getExternalStorageDirectory().getAbsolutePath();

	private static final String font = "fonts/Roboto-Condensed.ttf"; //TODO Set Path to font relative to assets folder
	private static final String SAVE_LOC = SD + "/.icon_request/files"; //TODO Set own file path.
	private static final String SAVE_LOC2 = SD + "/.icon_request"; //TODO Change also this one.
	private static final String appfilter_path = "empty_appfilter.xml"; //TODO Define path to appfilter.xml in assets folder.
	private boolean email_sent;

	private static final int numCol_Portrait = 1; //For Portrait orientation. Tablets have +1 and LargeTablets +2 Columns.
	private static final int numCol_Landscape = 5; //For Landscape orientation. Tablets have +1 and LargeTablets +2 Columns.

	private static final String TAG = "RequestActivity";
	private static final boolean DEBUG = true; //TODO Set to false for PlayStore Release

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.request_grid);
		switcherLoad = (ViewSwitcher)findViewById(R.id.viewSwitcherLoadingMain);
		context = this;

		getActionBar().setDisplayHomeAsUpEnabled(true);

		if(savedInstanceState == null){

			//Loading Logo Animation
			ImageView logo = (ImageView)findViewById(R.id.imageViewLogo);
			ObjectAnimator logoAni = (ObjectAnimator)AnimatorInflater.loadAnimator(context, R.animator.request_flip);
			logoAni.setRepeatCount(Animation.INFINITE);
			logoAni.setRepeatMode(Animation.RESTART);
			logoAni.setTarget(logo);
			logoAni.setDuration(2000);
			logoAni.start();

			taskList.execute();
		}
		else 
		{
			populateView(list_activities_final);
			switcherLoad.showNext();
		}
	}

	public class AsyncWorkerList extends AsyncTask<String, Integer, String>{

		public AsyncWorkerList(){}

		@Override
		protected String doInBackground(String... arg0) {
			try {
				//Get already styled apps
				parseXML();
				// Compare them to installed apps
				prepareData();

				return null;
			} 
			catch (Throwable e) {e.printStackTrace();}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			// Display the unstyled app
			populateView(list_activities_final);
			//Switch from loading screen to the main view
			switcherLoad.showNext();

			super.onPostExecute(result);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState){
		if(DEBUG)Log.v(TAG,"onSaveInstanceState");
		super.onSaveInstanceState(savedInstanceState);
	}


	@Override
	public void onStop(){
		super.onStop();
		if(DEBUG)Log.v(TAG,"onStop");
		//Stops the activity when the user opens the email app from the send intent.
		if(email_sent)finish();
	}

	@Override
	public void onResume(){
		super.onResume();
		if(DEBUG)Log.v(TAG,"onResume");
		//When the user dismisses the email intent reset the boolean.
		if(email_sent)email_sent = false;
	}

	@Override
	public void onBackPressed(){
		super.onBackPressed();
		if(DEBUG)Log.v(TAG,"onBackPressed");
		finish();
	}

	// Creates the Overflow Menu Button(s)
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.request_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId())
		{
		case R.id.action_sender:
		{
			// Called when the "Send Requests" Overflow Menu button is pressed
			actionSend();
			return true;
		}
		case android.R.id.home:{
			// When the user presses on the home button
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		default:
		{
			super.onOptionsItemSelected(item);
			return true;
		}
		}
	}

	//Toast wrapper to prevent showing each toast. Change text of current toast instead.
	public void makeToast (String text){
		try{
			toast.getView().isShown();
			toast.setText(text);
		}
		catch (Exception e)
		{
			toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
		}
		toast.show();
	}

	// Handler for sending messages out of separate Threads
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch(msg.what)
			{
			case 0:
				if(DEBUG)Log.v(TAG,"Handler case 0");

				makeToast(getString(R.string.request_toast_no_apps_selected));
				return;

			case 1:
				if(DEBUG)Log.v(TAG,"Handler case 1");

				makeToast("There are no email clients installed. Weird");
				return;

			case 2:
				if(DEBUG)Log.v(TAG,"Handler case 1");

				makeToast("Make sure you copied appfilter.xml in assets folder!");
				return;

			default:
				return;
			}
		}
	};

	private void actionSend()
	{
		Thread actionSend_Thread = new Thread()
		{
			@Override
			public void run()
			{
				final File save_loc = new File(SAVE_LOC);
				final File save_loc2 = new File(SAVE_LOC2 + "/");

				deleteDirectory(save_loc2); //This deletes old zips

				save_loc.mkdirs(); // recreates the directory
				save_loc2.mkdirs();

				Intent intent;
				ArrayList arrayList = list_activities_final;
				StringBuilder stringBuilderEmail = new StringBuilder();
				StringBuilder stringBuilderXML = new StringBuilder();
				stringBuilderEmail.append(getString(R.string.request_email_text));
				int amount = 0;

				// Get all selected apps
				for (int i = 0; i < arrayList.size(); i++)
				{
					if (((AppInfo)arrayList.get(i)).isSelected())
					{
						stringBuilderEmail.append(((AppInfo)arrayList.get(i)).getName() + "\n");
						stringBuilderXML.append("<!-- " + ((AppInfo)arrayList.get(i)).getName() +" -->\n<item component=\"ComponentInfo{"+((AppInfo)arrayList.get(i)).getCode()+"}\" drawable=\""+((AppInfo)arrayList.get(i)).getCode().split("/")[0]+"\"/>"+"\n");			

						Bitmap bitmap = ((BitmapDrawable)((AppInfo)arrayList.get(i)).getImage()).getBitmap();
						FileOutputStream fOut;

						try {
							fOut = new FileOutputStream(SAVE_LOC + "/" + ((AppInfo)arrayList.get(i)).getCode().split("/")[0] + "_" +((AppInfo)arrayList.get(i)).getCode().split("/")[1]+ ".png");
							bitmap.compress(Bitmap.CompressFormat.PNG,100,fOut);
							fOut.flush();
							fOut.close();
						}
						catch (FileNotFoundException e) {if(DEBUG)Log.v(TAG, "FileNotFoundException");}
						catch (IOException e) {	if(DEBUG)Log.v(TAG, "IOException");}
						amount++;
					}
				}
				if (amount == 0){//When there's no app selected show a toast and return.
					handler.sendEmptyMessage(0);
					return;
				}
				else // write zip and start email intent.
				{
					try {
						FileWriter fstream = new FileWriter(SAVE_LOC + "/appfilter.xml");
						BufferedWriter out = new BufferedWriter(fstream);
						out.write(stringBuilderXML.toString());
						out.close();
					} catch (Exception e){ return;}


					SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_hhmmss");
					String zipName = date.format(new Date());

					createZipFile(SAVE_LOC, true, SAVE_LOC2 + "/" + zipName + ".zip");

					deleteDirectory(save_loc); //This deletes all generated files except the zip

					intent = new Intent(android.content.Intent.ACTION_SEND);
					intent.setType("application/zip");

					String[] arrayOfString = new String[1];
					arrayOfString[0] = getString(R.string.request_email_addr);

					final Uri uri = Uri.parse("file://" + SAVE_LOC2 + "/" + zipName + ".zip");
					intent.putExtra(Intent.EXTRA_STREAM, uri);//TODO
					intent.putExtra("android.intent.extra.EMAIL", arrayOfString);
					intent.putExtra("android.intent.extra.SUBJECT", getString(R.string.request_email_subject));
					intent.putExtra("android.intent.extra.TEXT", stringBuilderEmail.toString());

					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					try{
						startActivity(Intent.createChooser(intent, "Send Email"));
						email_sent = true;
						return;
					}
					catch (ActivityNotFoundException localActivityNotFoundException){ //Just in case there is no email app installed
						handler.sendEmptyMessage(1);
						return;
					}
				}
			}
		};
		if(!actionSend_Thread.isAlive()) //Prevents the thread to be executed twice (or more) times.
		{
			actionSend_Thread.start();
		}
	}

	// Read the appfilter.xml from assets and get all activities
	private void parseXML()
	{
		try{
			XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
			XmlPullParser myparser = xmlFactoryObject.newPullParser();

			AssetManager am = context.getAssets();
			InputStream inputStream = am.open(appfilter_path);
			myparser.setInput(inputStream, null);

			int activity = myparser.getEventType();
			while (activity != XmlPullParser.END_DOCUMENT)
			{
				String name=myparser.getName();
				switch (activity){
				case XmlPullParser.START_TAG:
					break;
				case XmlPullParser.END_TAG:
					if(name.equals("item"))
					{	
						try	{
							String tmp_act = myparser.getAttributeValue(null,"component").split("/")[1];
							String t_activity= tmp_act.substring(0, tmp_act.length()-1);

							String tmp_pack = myparser.getAttributeValue(null,"component").split("/")[0];
							String t_package= tmp_pack.substring(14, tmp_pack.length());

							list_activities.add(t_package + "/" + t_activity);

							if(DEBUG)Log.v(TAG,"Added Styled App: \"" +t_package + "/" + t_activity+"\"");
						}
						catch(ArrayIndexOutOfBoundsException e){}
					}
					break;
				}
				activity = myparser.next();
			}
		}
		catch(IOException exIO){handler.sendEmptyMessage(2);return;} //Show toast when there's no appfilter.xml in assets
		catch(XmlPullParserException exXPPE){return;}
	}

	@SuppressWarnings("unchecked")
	private void prepareData() throws Throwable // Sort the apps
	{
		ArrayList<AppInfo> arrayList = new ArrayList();
		PackageManager pm = getPackageManager();
		Intent intent = new Intent("android.intent.action.MAIN", null);
		intent.addCategory("android.intent.category.LAUNCHER");
		List list = pm.queryIntentActivities(intent, 0);
		Iterator localIterator = list.iterator();
		if(DEBUG)Log.v(TAG,"list.size(): "+list.size());

		for (int i = 0; i < list.size(); i++)
		{
			ResolveInfo resolveInfo = (ResolveInfo)localIterator.next();

			// This is the main part where the already styled apps are sorted out.
			if ((list_activities.indexOf(resolveInfo.activityInfo.packageName + "/" + resolveInfo.activityInfo.name) == -1)) {

				AppInfo tempAppInfo = new AppInfo(
						resolveInfo.activityInfo.packageName + "/" + resolveInfo.activityInfo.name, //Get package/activity
						resolveInfo.loadLabel(pm).toString(), //Get the app name
						getHighResIcon(pm, resolveInfo), //Loads xxxhdpi icon, returns normal if it on fail
						false //Unselect icon per default
						);
				arrayList.add(tempAppInfo);

				// This is just for debugging
				if(DEBUG)Log.i(TAG,"Added app: " + resolveInfo.loadLabel(pm));
			} else {
				// This is just for debugging
				if(DEBUG)Log.v(TAG,"Removed app: " + resolveInfo.loadLabel(pm));
			}
		}

		Collections.sort(arrayList, new Comparator<AppInfo>() { //Custom comparator to ensure correct sorting for characters like 'Ü' and apps starting with a small letter like iNex
			@Override
			public int compare(AppInfo object1, AppInfo object2) {
				Locale locale = Locale.getDefault();
				Collator collator = Collator.getInstance(locale);
				collator.setStrength(Collator.TERTIARY);

				if(DEBUG)Log.v(TAG,"Comparing \""+object1.getName()+"\" to \"" + object2.getName()+"\"");

				return collator.compare(object1.getName(), object2.getName());
			}
		});

		list_activities_final = arrayList;
		return;
	}


	private Drawable getHighResIcon(PackageManager pm, ResolveInfo resolveInfo){

		Resources resources;
		Drawable icon;

		try {
			ComponentName componentName = new ComponentName(resolveInfo.activityInfo.packageName , resolveInfo.activityInfo.name);

			resources = pm.getResourcesForActivity(componentName);//Get resources for the activity

			int iconId = resolveInfo.getIconResource();//Get the resource Id for the activity icon

			if(iconId != 0) {
				icon = resources.getDrawableForDensity(iconId, 640);//Loads the icon at xxhdpi resolution or lower.
				return icon;
			}
			return resolveInfo.loadIcon(pm);

		} catch (PackageManager.NameNotFoundException e) {
			return resolveInfo.loadIcon(pm);//If it fails return the normal icon
		} catch (Resources.NotFoundException e) {
			return resolveInfo.loadIcon(pm);
		}
	}

	@SuppressWarnings("deprecation")
	public int getDisplaySize(String which){
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		if(which.equals("height")){
			return display.getHeight();
		}
		if(which.equals("width"))
		{
			return display.getWidth();
		}
		if(DEBUG)Log.v(TAG, "Normally unreachable. Line. What happened??");
		return 1000;
	}
	private boolean isPortrait() {
		return (getDisplaySize("height") > getDisplaySize("width"));
	}
	private static boolean isTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}


	@SuppressWarnings("unchecked")
	private void populateView(ArrayList arrayListFinal){
		ArrayList<AppInfo> local_arrayList = new ArrayList();
		local_arrayList = arrayListFinal;

		GridView grid = (GridView)findViewById(R.id.appgrid);

		grid = (GridView)findViewById(R.id.appgrid);

		grid.setVerticalSpacing(GridView.AUTO_FIT);
		grid.setHorizontalSpacing(GridView.AUTO_FIT);
		grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		grid.setFastScrollEnabled(true);
		grid.setFastScrollAlwaysVisible(true);

		if(DEBUG)Log.v(TAG,"height: "+getDisplaySize("height")+"; width: "+getDisplaySize("width"));

		if(isPortrait())
		{
			grid.setNumColumns(numCol_Portrait);

			if(isTablet(context)) {
				grid.setNumColumns(numCol_Portrait + 1 ); //Here you can change the number of columns for Tablets
				if(DEBUG)Log.v(TAG,"isTablet");
			}
			if(isXLargeTablet(context)) {
				grid.setNumColumns(numCol_Portrait + 2 ); //Here you can change the number of columns for Large Tablets
				if(DEBUG)Log.v(TAG,"isXLargeTablet");
			}

			appInfoAdapter = new AppAdapter(this, R.layout.request_item_list, local_arrayList);
		}

		else
		{
			grid.setNumColumns(numCol_Landscape);

			if(isTablet(context)) {
				grid.setNumColumns(numCol_Landscape + 1 ); //Here you can change the number of columns for Tablets
				if(DEBUG)Log.v(TAG,"isTablet");
			}
			if(isXLargeTablet(context)) {
				grid.setNumColumns(numCol_Landscape + 2 ); //Here you can change the number of columns for Large Tablets
				if(DEBUG)Log.v(TAG,"isXLargeTablet");
			}

			appInfoAdapter = new AppAdapter(this, R.layout.request_item_grid, local_arrayList);
		}

		grid.setAdapter(appInfoAdapter);
		grid.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> AdapterView, View view, int position, long row)
			{
				AppInfo appInfo = (AppInfo)AdapterView.getItemAtPosition(position);
				CheckBox checker = (CheckBox)view.findViewById(R.id.CBappSelect);
				ViewSwitcher icon = (ViewSwitcher)view.findViewById(R.id.viewSwitcherChecked);
				LinearLayout localBackground = (LinearLayout)view.findViewById(R.id.card_bg);				
				Animation aniIn = AnimationUtils.loadAnimation(context, R.anim.request_flip_in_half_1);
				Animation aniOut = AnimationUtils.loadAnimation(context, R.anim.request_flip_in_half_2);

				checker.toggle();
				appInfo.setSelected(checker.isChecked());

				icon.setInAnimation(aniIn);
				icon.setOutAnimation(aniOut);

				if(appInfo.isSelected())
				{
					if(DEBUG)Log.v(TAG,"Selected App: "+appInfo.getName());
					localBackground.setBackgroundColor(Color.parseColor(getString(R.color.request_card_pressed)));
					if(icon.getDisplayedChild() == 0){
						icon.showNext();
					}
				}
				else{
					if(DEBUG)Log.v(TAG,"Deselected App: "+appInfo.getName());
					localBackground.setBackgroundColor(Color.parseColor(getString(R.color.request_card_unpressed)));
					if(icon.getDisplayedChild() == 1){
						icon.showPrevious();
					}
				}
			}
		});
	}

	private class AppAdapter extends ArrayAdapter<AppInfo>
	{
		@SuppressWarnings("unchecked")
		private ArrayList<AppInfo> appList = new ArrayList();

		public AppAdapter(Context context, int position, ArrayList<AppInfo> adapterArrayList)
		{
			super(context, position, adapterArrayList);
			appList.addAll(adapterArrayList);
		}
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if(tf == null){
				tf = Typeface.createFromAsset(context.getAssets(), font);
			}

			ViewHolder holder;
			if (convertView == null) {
				if(isPortrait())
				{
					convertView = ((LayoutInflater)getSystemService("layout_inflater")).inflate(R.layout.request_item_list, null);
				} else {
					convertView = ((LayoutInflater)getSystemService("layout_inflater")).inflate(R.layout.request_item_grid, null);
				}
				holder = new ViewHolder();
				holder.apkIcon = (ImageView) convertView.findViewById(R.id.IVappIcon);
				holder.apkName = (TextView) convertView.findViewById(R.id.TVappName);
				holder.apkPackage = (TextView) convertView.findViewById(R.id.TVappPackage);
				holder.checker = (CheckBox) convertView.findViewById(R.id.CBappSelect);
				holder.cardBack = (LinearLayout) convertView.findViewById(R.id.card_bg);
				holder.switcherChecked = (ViewSwitcher)convertView.findViewById(R.id.viewSwitcherChecked);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			AppInfo appInfo = (AppInfo)appList.get(position);

			if(isPortrait()) {
				holder.apkPackage.setText(appInfo.getCode().split("/")[0]+"/"+appInfo.getCode().split("/")[1]);
				holder.apkPackage.setTypeface(tf);
			} else {
				holder.apkPackage.setVisibility(View.GONE);
			}

			holder.apkName.setText(appInfo.getName());

			holder.apkIcon.setImageDrawable(appInfo.getImage());

			holder.switcherChecked.setInAnimation(null);
			holder.switcherChecked.setOutAnimation(null);

			holder.checker.setChecked(appInfo.isSelected());
			if(appInfo.isSelected())
			{
				holder.cardBack.setBackgroundColor(Color.parseColor(getString(R.color.request_card_pressed)));
				if(holder.switcherChecked.getDisplayedChild() == 0){
					holder.switcherChecked.showNext();
				}
			} else {
				holder.cardBack.setBackgroundColor(Color.parseColor(getString(R.color.request_card_unpressed)));
				if(holder.switcherChecked.getDisplayedChild() == 1){
					holder.switcherChecked.showPrevious();
				}
			}
			holder = (ViewHolder)convertView.getTag();
			return convertView;
		}
	}

	private class ViewHolder {
		TextView apkName;
		TextView apkPackage;
		ImageView apkIcon;
		CheckBox checker;
		LinearLayout cardBack;
		ViewSwitcher switcherChecked;
	}

	//Zip Stuff. Better leave that Alone ^^

	public static boolean deleteDirectory(File path) {
		if( path.exists() ) {
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					deleteDirectory(files[i]);
				}
				else {
					files[i].delete();
				}
			}
		}
		return( path.delete() );
	}

	public static boolean createZipFile(final String path, final boolean keepDirectoryStructure, final String out_file) {
		final File f = new File(path);
		if (!f.canRead() || !f.canWrite())
		{
			if(DEBUG)Log.d(TAG, path + " cannot be compressed due to file permissions");
			return false;
		}
		try {
			ZipOutputStream zip_out = new ZipOutputStream(
					new BufferedOutputStream(
							new FileOutputStream(out_file), BUFFER));
			if (keepDirectoryStructure)
			{
				zipFile(path, zip_out, "");
			}
			else
			{
				final File files[] = f.listFiles();
				for (final File file : files) {
					zip_folder(file, zip_out);
				}
			}
			zip_out.close();
		} 
		catch (FileNotFoundException e){ if(DEBUG)Log.e("File not found", e.getMessage()); return false; } 
		catch (IOException e){ if(DEBUG)Log.e("IOException", e.getMessage()); return false; }
		return true;
	}

	// StahP !! Turn around ! Nothing to see here!

	// keeps directory structure
	public static void zipFile(final String path, final ZipOutputStream out, final String relPath) throws IOException 
	{
		final File file = new File(path);
		if (!file.exists()){if(DEBUG)Log.d(TAG, file.getName() + " does NOT exist!");return;}
		final byte[] buf = new byte[1024];
		final String[] files = file.list();
		if (file.isFile())
		{   
			FileInputStream in = new FileInputStream(file.getAbsolutePath()); 

			try
			{
				out.putNextEntry(new ZipEntry(relPath + file.getName()));
				int len; 
				while ((len = in.read(buf)) > 0) 
				{ 
					out.write(buf, 0, len); 
				}
				out.closeEntry(); 
				in.close();
			}
			catch (ZipException zipE)
			{
				if(DEBUG)Log.d(TAG, zipE.getMessage());
			}
			finally
			{
				if (out != null) out.closeEntry(); 
				if (in != null) in.close();
			}
		}
		else if (files.length > 0) // non-empty folder
		{
			for (int i = 0, length = files.length; i < length; ++i)
			{
				zipFile(path + "/" + files[i], out, relPath + file.getName() + "/");
			}
		}
	}

	private static void zip_folder(File file, ZipOutputStream zout) throws IOException {
		byte[] data = new byte[BUFFER];
		int read;
		if(file.isFile()){
			ZipEntry entry = new ZipEntry(file.getName());
			zout.putNextEntry(entry);
			BufferedInputStream instream = new BufferedInputStream(
					new FileInputStream(file));
			while((read = instream.read(data, 0, BUFFER)) != -1)
				zout.write(data, 0, read);
			zout.closeEntry();
			instream.close();
		} else if (file.isDirectory()) {
			String[] list = file.list();
			int len = list.length;
			for(int i = 0; i < len; i++)
				zip_folder(new File(file.getPath() +"/"+ list[i]), zout);
		}
	}
	// Sigh...
}