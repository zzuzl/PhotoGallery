package cn.zzuzl.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by zhanglei53 on 2018/1/31.
 */

public class ImageDownloader<T> extends HandlerThread {
    private static final String TAG = ImageDownloader.class.getName();
    private static final int MSG_DOWNLOAD = 0;
    private boolean mQuit = false;
    private Handler mHandler = null;
    private ConcurrentMap<T, String> mMap = new ConcurrentHashMap<T, String>();
    private Handler mResHandler = null;
    private ImageDownloadListener<T> mListener = null;

    public ImageDownloader(Handler handler) {
        super(TAG);
        mResHandler = handler;
    }

    /**
     * 放入队列
     * @param t
     * @param url
     */
    public void queue(T t, String url) {
        if (url == null) {
            mMap.remove(t);
        } else {
            mMap.put(t, url);
            mHandler.obtainMessage(MSG_DOWNLOAD, t).sendToTarget();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_DOWNLOAD:
                        T target = (T) msg.obj;
                        handleRequest(target);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mQuit = true;
        return super.quit();
    }

    /**
     * 处理请求
     * @param target
     */
    private void handleRequest(final T target) {
        try {
            final String url = mMap.get(target);
            if (url == null) {
                return;
            }

            byte[] bytes = FlickrFetcher.getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            // 线程安全
            mResHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!url.equals(mMap.get(target)) || mQuit) {
                        return;
                    }
                    mMap.remove(target);
                    mListener.onFinished(target, bitmap, url);
                }
            });
            Log.i(TAG, "bitmap created");
        } catch (IOException e) {
            Log.e(TAG, "handleRequest,error", e);
        }
    }

    public void setListener(ImageDownloadListener<T> listener) {
        mListener = listener;
    }

    public interface ImageDownloadListener<T> {
        void onFinished(T target, Bitmap bitmap, String url);
    }
}
