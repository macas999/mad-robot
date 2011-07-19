package com.madrobot.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import com.madrobot.io.file.FileUtils;
import com.madrobot.io.file.SDCardUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/**
 * Task to back the application's database file to the sdcard
 * Parameters
 * <pre>
 * Object[] param = new Object[4];
 * // database package
 * param[0] = "com.myapp.databasebu";
 * // database name
 * param[1] = "mydata.db";
 *  //database path
 *  param[2]="data/data/
 *  //path on the sdcard
 *  param[3]=new File("sdcard/mybackup");
 *  //execute the task
 *  new DownloadBitmapTask(this, this).execute(param);
 * </pre>
 * 
 * @author elton.kent
 * 
 */
public class ExportDatabaseFileTask extends AbstractTask {// AsyncTask<String,
															// Void, Boolean> {
	private static final String TAG = ExportDatabaseFileTask.class
			.getSimpleName();

	public ExportDatabaseFileTask(Context context, TaskNotifier notifier) {
		super(context, notifier);
	}

	// automatically done on worker thread (separate from UI thread)
	@Override
	protected Object doInBackground(Object... params) {
		taskStarted();
		String dbPackage=(String)params[0];
		String dbName=(String)params[1];
		String dbPath=(String)params[2];
		File dbFile = new File(dbPath + dbName);
		// path on sd by convention
		File exportDir =(File) params[3];
		if (!exportDir.exists()) {
			// boolean result =
			exportDir.mkdirs();
			// Log.i(TAG, "create directory " + (result ? "succesful" :
			// "failed"));
		}

		File file = new File(exportDir, dbName);

		try {
			file.createNewFile();
			FileUtils.copyFileNio(dbFile, file);
			return true;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		}
	}


	protected void onPostExecute(Object result) {
		notifier.onTaskCompleted();
	}

}