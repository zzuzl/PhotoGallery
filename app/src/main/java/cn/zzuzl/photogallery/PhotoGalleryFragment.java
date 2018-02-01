package cn.zzuzl.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = PhotoGalleryFragment.class.getName();
    private RecyclerView mRecyclerView;
    private ImageDownloader<PhotoHolder> mDownloader = new ImageDownloader<PhotoHolder>(new Handler());
    private ConcurrentMap<String,Drawable> mDrawableMap = new ConcurrentHashMap<String,Drawable>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();

        mDownloader.start();
        mDownloader.getLooper();

        mDownloader.setListener(new ImageDownloader.ImageDownloadListener<PhotoHolder>() {
            @Override
            public void onFinished(PhotoHolder target, Bitmap bitmap, String url) {
                Drawable drawable = new BitmapDrawable(getResources(), bitmap);

                if (!mDrawableMap.containsKey(url)) {
                    mDrawableMap.put(url, drawable);
                    target.bindDrawable(drawable);
                }
            }
        });

        Log.i(TAG, "background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mRecyclerView = view.findViewById(R.id.photo_recycle_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setUpAdapter(new ArrayList<GalleryItem>());

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDownloader.quit();

        Log.i(TAG, "background thread destroyed");
    }

    // 构造adapter
    private void setUpAdapter(List<GalleryItem> mItems) {
        if (isAdded()) {
            mRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    /**
     * holder
     */
    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.item_image_view);
        }

        public void bind(GalleryItem item) {
            Log.i(TAG, this + ":url:" + item.getUrl());
            Drawable drawable = getResources().getDrawable(R.mipmap.ic_launcher);

            if (!mDrawableMap.containsKey(item.getUrl())) {
                mImageView.setImageDrawable(drawable);
                mDownloader.queue(this, item.getUrl());
            } else {
                mImageView.setImageDrawable(mDrawableMap.get(item.getUrl()));
            }
        }

        public void bindDrawable(Drawable drawable) {
            Log.i(TAG, this + "");
            mImageView.setImageDrawable(drawable);
        }
    }

    /**
     * adapter
     */
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mItems = null;

        public PhotoAdapter(List<GalleryItem> items) {
            mItems = items;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, parent, false);
            Log.i(TAG, "onCreateViewHolder");
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    /**
     * 后台处理task
     */
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return FlickrFetcher.fetchItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            setUpAdapter(galleryItems);
        }
    }

}
