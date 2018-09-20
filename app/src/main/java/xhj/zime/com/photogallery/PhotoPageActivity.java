package xhj.zime.com.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;

public class PhotoPageActivity extends SingleFragmentActivity {


    public static Intent newIntent(Context context, Uri photoUri){
        Intent intent = new Intent(context,PhotoPageActivity.class);
        intent.setData(photoUri);
        return intent;
    }

    @Override
    public Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }
}
