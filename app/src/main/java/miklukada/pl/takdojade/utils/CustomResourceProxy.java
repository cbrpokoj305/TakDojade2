package miklukada.pl.takdojade.utils;

/**
 * Created by Barca on 2016-04-09.
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import org.osmdroid.DefaultResourceProxyImpl;

public class CustomResourceProxy extends DefaultResourceProxyImpl {

    private final Context mContext;
    public CustomResourceProxy(Context pContext) {
        super(pContext);
        mContext = pContext;
    }

    @Override
    public Bitmap getBitmap(final bitmap pResId) {
           /* switch (pResId){
                case person:
                    //your image goes here!!!
                    return BitmapFactory.decodeResource(mContext.getResources(), org.osmdroid.example.R.drawable.sfgpuci);
            }*/
        return super.getBitmap(pResId);
    }

    @Override
    public Drawable getDrawable(final bitmap pResId) {
           /* switch (pResId){
                case person:
                    return mContext.getResources().getDrawable(org.osmdroid.example.R.drawable.sfgpuci);
            }*/
        return super.getDrawable(pResId);
    }
}

