package xhj.zime.com.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Boolean mHasQuit = false;
    private Handler mRequestHandler;
    //线程安全的map,T泛型指的是PhotoHolder,一张照片对应一个URL地址,因为RecyclerView是会回收利用的,所以要确保是最新的url来更新图片
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();

    private Handler mResponseHandler;
    private ThumbnailDownloaderListener<T> mThumbnailDownloaderListener;

    public interface ThumbnailDownloaderListener<T>{
        void onThumbnailDownloaded(T target,Bitmap bitmap);
    }

    public void setThumbnailDownloaderListener(ThumbnailDownloaderListener<T> listener){
        mThumbnailDownloaderListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    //在准备Looper对象的时候创建Handler是最合适的,一个Message对应一个Handler,首次检查消息队列前调用
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //处理信息,下载图片,然后再发回去主线程更新UI
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }
            byte[] bitmapBytes = new FlickrFectchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            Log.i(TAG, "Bitmap created");
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url || mHasQuit){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloaderListener.onThumbnailDownloaded(target,bitmap);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
        }
    }

    //结束下载
    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        //没有下载地址就移除,有下载地址就存进map然后发送该目标
        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }
}
