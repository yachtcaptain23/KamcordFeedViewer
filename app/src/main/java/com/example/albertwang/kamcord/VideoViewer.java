package com.example.albertwang.kamcord;

import com.example.albertwang.kamcord.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class VideoViewer extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;
    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private static final String TAG = "Kamcord";
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<VideoEntry> mFeedEntryList;
    // private Toast mDebugToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_viewer);

        mContext = getApplicationContext();

        // Initialize the RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_feed);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mFeedEntryList = new ArrayList<VideoEntry>();

        // TODO: Start the Async task of FeedInformation
        new FeedInformation().execute("https://app.kamcord.com/app/v3/feeds/featured_feed");
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private ViewHolder mViewHolder;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public ImageView mThumbnailView;
            public TextView mTitleView;
            public ViewHolder(View v) {
                super(v);
                Log.d(TAG, "Called super from ViewHolder constructor for layout");
                mThumbnailView = (ImageView) v.findViewById(R.id.feed_entry_thumbnail);
                mTitleView = (TextView) v.findViewById(R.id.feed_entry_title);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter() {}

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View feed_entry = LayoutInflater.from(parent.getContext()).inflate(R.layout.feed_entry, parent, false);
            // set the view's size, margins, paddings and layout parameters
            // mViewHolder = new ViewHolder(todo_view, due_view, comment_view, estimatedCompletionTime_view);
            mViewHolder = new ViewHolder(feed_entry);
            Log.d(TAG, "Finished onCreate");
            return mViewHolder;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final VideoEntry videoEntry = mFeedEntryList.get(position);
            Log.d(TAG, "Binding for position:1" + position);
            holder.mThumbnailView.setImageBitmap(videoEntry.thumbnail);
            holder.mTitleView.setText(videoEntry.title);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mFeedEntryList.size();
        }
    }


    private class FeedInformation extends AsyncTask<String, Void, ArrayList<VideoEntry>> {
        protected ArrayList<VideoEntry> doInBackground(String... feedUrl) {
            // String feedUrlString = "https://app.kamcord.com/app/v3/feeds/featured_feed";
            HttpURLConnection connection = null;
            StringBuilder response = new StringBuilder();
            try {
                URL url = new URL(feedUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("device-token", "Hello");
                connection.setUseCaches(false);
                connection.setDoOutput(false);

                // Send Request
                // DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                // wr.close();

                // Get Response
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;

                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append("\n");
                    Log.i("albewang", line);
                }
                rd.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (response.toString().isEmpty()) {
                Log.i(TAG, "Returning null early!");
                return null;
            }

            Log.i(TAG, "Unparsing JSON");

            try {
                JSONObject jsonObject = new JSONObject(response.toString());
                Log.i(TAG, jsonObject.getJSONObject("status").getString("status_reason"));
                JSONArray jsonArray = jsonObject.getJSONObject("response").getJSONObject("video_list").getJSONArray("video_list");
                for (int i = 0; i < jsonArray.length(); i++) {
                    String thumbnailurl = jsonArray.getJSONObject(i).getJSONObject("thumbnails").getString("regular");
                    String title = jsonArray.getJSONObject(i).getString("title");
                    String video_url = jsonArray.getJSONObject(i).getString("video_url");
                    String next_page = jsonObject.getJSONObject("response").getJSONObject("video_list").getString("next_page");
                    Log.i(TAG, thumbnailurl);
                    Log.i(TAG, title);
                    Log.i(TAG, video_url);
                    Log.i(TAG, next_page);
                    VideoEntry videoEntry = new VideoEntry(thumbnailurl, title, video_url, next_page);
                    mFeedEntryList.add(videoEntry);
                }
            } catch (JSONException e) {}

            return new ArrayList<VideoEntry>();

            // Todo: Go through JSON list and create an ArrayList of VideoEntries
            // TODO: Each time the thumbnail gets downloaded:
            // TODO: (1) Generate the VideoEntry
            // TODO: (2) Notify the UI thread to update the RecyclerView
            // TOOD: (3) Add to ArrayList
        }

        protected void onPostExecute(ArrayList arr) {
            // mAdapter.notifyDataSetChanged();
        }
    }

    private class VideoEntry {
        Bitmap thumbnail;
        String title;
        String video_url;
        String next_page;

        private VideoEntry(String bitmap_url, String title, String video_url, String next_page) {
            this.title = title;
            this.video_url = video_url;
            this.next_page = next_page;
            new ThumbnailLoad().execute(bitmap_url);
        }

        private class ThumbnailLoad extends AsyncTask<String, Void, Bitmap> {
            protected Bitmap doInBackground(String... bitmap_url) {
                try {
                    URL url = new URL(bitmap_url[0]);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    InputStream input = connection.getInputStream();
                    return BitmapFactory.decodeStream(input);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to retrieve bitmap from server");
                }
                return null;
            }

            protected void onPostExecute(Bitmap bmp) {
                if (bmp != null) {
                    thumbnail = bmp;
                }
                mAdapter.notifyDataSetChanged();
            }
        }
    }



    public void launchVideo(View v) {
        /*
        String url = "http://www.sample-videos.com/video/mp4/720/big_buck_bunny_720p_1mb.mp4";
        VideoView vidView = (VideoView) findViewById(R.id.video);
        Uri vidUri = Uri.parse(url);
        vidView.setVideoURI(vidUri);
        vidView.start();
        */
        String feedUrlString =  "https://app.kamcord.com/app/v3/feeds/featured_feed";



    }
}
