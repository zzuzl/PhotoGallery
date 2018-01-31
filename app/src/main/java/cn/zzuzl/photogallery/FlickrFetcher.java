package cn.zzuzl.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanglei53 on 2018/1/31.
 */

public class FlickrFetcher {
    private static final String TAG = FlickrFetcher.class.getName();
    private static final String API_KEY = "a2bae02f21c07c06cebb49553a9cc71d";
    private static final String API_PASSWORD = "a510250592f297d8";

    public static List<GalleryItem> fetchItems() {
        String url = Uri.parse("https://api.flickr.com/services/rest/")
                .buildUpon()
                .appendQueryParameter("method", "flickr.photos.getRecent")
                .appendQueryParameter("api_key", API_KEY)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("nojsoncallback", "1")
                .appendQueryParameter("extras", "url_s")
                .build()
                .toString();
        try {
            String json = getUrlString(url);
            Log.i(TAG, "json:" + json);
            return parseItems(json);
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }
        return new ArrayList<GalleryItem>();
    }

    private static List<GalleryItem> parseItems(String json) throws JSONException {
        List<GalleryItem> list = new ArrayList<GalleryItem>();

        JSONObject object = new JSONObject(json);
        object = object.getJSONObject("photos");
        JSONArray array = object.getJSONArray("photo");

        for (int i=0;i<array.length();i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            if (!jsonObject.has("url_s")) {
                continue;
            }
            GalleryItem item = new GalleryItem();
            item.setId(jsonObject.getString("id"));
            item.setCaption(jsonObject.getString("title"));
            item.setUrl(jsonObject.getString("url_s"));
            list.add(item);
        }
        return list;
    }

    /**
     * 访问网络
     * @param urlParam
     * @return
     * @throws IOException
     */
    public static byte[] getUrlBytes(String urlParam) throws IOException {
        URL url = new URL(urlParam);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {

            InputStream is = urlConnection.getInputStream();
            if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(urlParam + ":" + urlConnection.getResponseMessage());
            }
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            out.close();
            urlConnection.disconnect();
        }
    }

    public static String getUrlString(String url) throws IOException {
        return new String(getUrlBytes(url));
    }
}
