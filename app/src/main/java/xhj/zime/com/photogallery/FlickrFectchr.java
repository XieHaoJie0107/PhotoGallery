package xhj.zime.com.photogallery;

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

public class FlickrFectchr {

    private static final String TAG = "FlickrFectchr";
    private static final String FETCH_RECENTS_METHOD = "全部";
    private static final String SEARCH_METHOD = "";
    private static final Uri ENDPOINT = Uri.parse("http://image.baidu.com/channel/listjson/")
            .buildUpon()
            .appendQueryParameter("pn","0")
            .appendQueryParameter("rn","30")
            .appendQueryParameter("tag1","明星")
            .appendQueryParameter("ie","utf8")
            .build();

    public List<GalleryItem> fetchRecentPhotos(){
        String url = buildUrl(FETCH_RECENTS_METHOD,null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query){
        String url = buildUrl(SEARCH_METHOD,query);
        return downloadGalleryItems(url);
    }

    private String buildUrl(String method,String query){
        Uri.Builder uriBuild = ENDPOINT.buildUpon().appendQueryParameter("tag2", method);
        if (method.equals(SEARCH_METHOD)) {
            uriBuild.appendQueryParameter("ftags",query);
        }
        return uriBuild.build().toString();
    }

    private void parseItems(List<GalleryItem> items,JSONObject jsonBody) throws JSONException {
        JSONArray photoJsonArray = jsonBody.getJSONArray("data");
        for (int i = 0; i < photoJsonArray.length() ; i++){
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            if (!photoJsonObject.has("image_url")){
                continue;
            }
            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("abs"));
            item.setUrl(photoJsonObject.getString("image_url"));
            items.add(item);
        }
    }


    private List<GalleryItem> downloadGalleryItems(String url){
        List<GalleryItem> items = new ArrayList<>();
        try {
            String jsonString = getUrlString(url);
            JSONObject jsonObject = new JSONObject(jsonString);
            parseItems(items,jsonObject);
            Log.i(TAG, "Received JSON: "+jsonString);
        } catch (IOException e) {
            Log.i(TAG, "Failed to fetch items",e);
        } catch (JSONException e) {
            Log.i(TAG, "Failed to parse JSON",e);
        }
        return items;
    }

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        url);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }
}
