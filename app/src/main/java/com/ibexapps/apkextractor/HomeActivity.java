package com.ibexapps.apkextractor;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.shahroz.svlibrary.interfaces.onSearchListener;
import com.shahroz.svlibrary.interfaces.onSimpleSearchActionsListener;
import com.shahroz.svlibrary.utils.Util;
import com.shahroz.svlibrary.widgets.MaterialSearchView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements onSimpleSearchActionsListener, onSearchListener {
    private boolean mSearchViewAdded = false;
    private MaterialSearchView mSearchView;
    private WindowManager mWindowManager;
    private Toolbar mToolbar;
    private MenuItem searchItem;
    private boolean searchActive = false;
    private FloatingActionButton fab;
    private List<AppInfo> applicationList = new ArrayList<AppInfo>();
    private String filePath;

    private ApplicationAdapter mAdapter;

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mSearchView = new MaterialSearchView(this);
        mSearchView.setOnSearchListener(this);
        mSearchView.setSearchResultsListener(this);
        mSearchView.setHintText("Search");

        if (mToolbar != null) {
            // Delay adding SearchView until Toolbar has finished loading
            mToolbar.post(new Runnable() {
                @Override
                public void run() {
                    if (!mSearchViewAdded && mWindowManager != null) {
                        mWindowManager.addView(mSearchView,
                                MaterialSearchView.getSearchViewLayoutParams(HomeActivity.this));
                        mSearchViewAdded = true;
                    }
                }
            });
        }
// Handle ProgressBar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);


        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //  mRecyclerView.setItemAnimator(new CustomItemAnimator());
        //  mRecyclerView.setItemAnimator(new ReboundItemAnimator());

        mAdapter = new ApplicationAdapter(new ArrayList<AppInfo>(), R.layout.row_application, HomeActivity.this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.theme_accent));
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new InitializeApplicationsTask().execute();
            }
        });

        new InitializeApplicationsTask().execute();


        //show progress
        mRecyclerView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchMarket();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        searchItem = menu.findItem(R.id.search);
        searchItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                mSearchView.display();
                openKeyboard();
                return true;
            }
        });
        if (searchActive)
            mSearchView.display();
        return true;

    }

    private void openKeyboard() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                mSearchView.getSearchView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                mSearchView.getSearchView().dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
            }
        }, 200);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSearch(String query) {
        Log.v("query--", " " + query + "----");

        final List<AppInfo> applicationFilteredList = filter(applicationList, query);
        mAdapter.setFilter(applicationFilteredList);
        //mAdapter.clearApplications();
       // mAdapter.addApplications(applicationFilteredList);
    }

    @Override
    public void searchViewOpened() {

    }

    @Override
    public void searchViewClosed() {


    }

    @Override
    public void onItemClicked(String item) {

    }

    @Override
    public void onScroll() {

    }

    @Override
    public void error(String localizedMessage) {

    }

    @Override
    public void onCancelSearch() {

        searchActive = false;
        mSearchView.hide();
    }

    /**
     * helper class to start the new detailActivity animated
     *
     * @param appInfo
     * @param appIcon
     */
    public void animateActivity(AppInfo appInfo, View appIcon) {

        new GenerateApk(appInfo).execute();


    }

    private void extractApk(AppInfo appInfo) {
        final ResolveInfo info = appInfo.getResolveInfo();
        File f1 = new File(info.activityInfo.applicationInfo.publicSourceDir);

        Log.v("file--", " " + f1.getName().toString() + "----" + info.loadLabel(getPackageManager()));
        try {

            String file_name = info.loadLabel(getPackageManager()).toString();
            Log.d("file_name--", " " + file_name);

            // File f2 = new File(Environment.getExternalStorageDirectory().toString()+"/Folder/"+file_name+".apk");
            // f2.createNewFile();
            filePath = Environment.getExternalStorageDirectory().toString() + "/Apk Extractor";
            File f2 = new File(filePath);
            f2.mkdirs();
            f2 = new File(f2.getPath() + "/" + file_name + ".apk");
            f2.createNewFile();

            InputStream in = new FileInputStream(f1);

            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            filePath = "Success!  "+file_name+".apk generated at " + Environment.getExternalStorageDirectory().toString() + "/Apk Extractor";
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage() + " in the specified directory.");
            filePath = "Error!  File not  found";
        } catch (IOException e) {
            System.out.println(e.getMessage());
            filePath = "Error!  Please try again later";
        }
    }

        /**
         * A simple AsyncTask to load the list of applications and display them
         */
        private class InitializeApplicationsTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected void onPreExecute() {
                mAdapter.clearApplications();
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                applicationList.clear();

                //Query the applications
                final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                List<ResolveInfo> ril = getPackageManager().queryIntentActivities(mainIntent, 0);
                for (ResolveInfo ri : ril) {
                    applicationList.add(new AppInfo(HomeActivity.this, ri));
                }
                Collections.sort(applicationList);

                for (AppInfo appInfo : applicationList) {
                    //load icons before shown. so the list is smoother
                    appInfo.getIcon();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                //handle visibility
                mRecyclerView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);

                //set data for list
                mAdapter.addApplications(applicationList);
                mSwipeRefreshLayout.setRefreshing(false);

                super.onPostExecute(result);
            }
        }
        /**
         * A simple AsyncTask to load the list of applications and display them
         */
        private class GenerateApk extends AsyncTask<Void, Void, Void> {
            ProgressDialog progressDialog;
            AppInfo appInfo;

            GenerateApk(AppInfo appInfo) {
                this.appInfo = appInfo;
            }

            @Override
            protected void onPreExecute() {
                progressDialog = ProgressDialog.show(HomeActivity.this,
                        "Extracting Apk",
                        "Please Wait...");
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                extractApk(appInfo);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {

                if(filePath.startsWith("E"))
                Util.showSnackBarMessage(fab, filePath);
                else
                showSnackBarMessageWithCallAction(fab, filePath);
                progressDialog.dismiss();

                super.onPostExecute(result);
            }
        }
    public static void showSnackBarMessageWithCallAction(View view, String message){
        final Snackbar snackbar = Snackbar
                .make(view, message, Snackbar.LENGTH_INDEFINITE)
                .setAction("Got it", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
        View snackbarView = snackbar.getView();
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setMaxLines(3);
        snackbar.show();
    }
    private List<AppInfo> filter(List<AppInfo> applications, String query) {
        query = query.toLowerCase();

        final List<AppInfo> applicationFilteredList = new ArrayList<>();
        for (AppInfo app : applications) {
            final String text = app.getName().toLowerCase();
            if (text.contains(query)) {
                applicationFilteredList.add(app);
            }
        }
        return applicationFilteredList;
    }
    private void launchMarket() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Util.showSnackBarMessage(fab, "Sorry!  unable to find market app");
        }
    }
}

