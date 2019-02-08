package mobi.maptrek.fragments;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import com.github.paolorotolo.appintro.AppIntroBaseFragment;
import com.github.paolorotolo.appintro.model.SliderPage;
import de.hdodenhof.circleimageview.CircleImageView;
import mobi.maptrek.R;
import mobi.maptrek.io.gpx.GpxFile;

public class IntroductionFragment extends AppIntroBaseFragment {
    static final /* synthetic */ boolean $assertionsDisabled = (!IntroductionFragment.class.desiredAssertionStatus());
    private static final String ARG_CUSTOM_DRAWABLE = "custom_drawable";
    private CircleImageView mImageView;

    public static IntroductionFragment newInstance(SliderPage sliderPage) {
        IntroductionFragment slide = new IntroductionFragment();
        Bundle args = new Bundle();
        args.putString("title", sliderPage.getTitleString());
        args.putString("title_typeface", sliderPage.getTitleTypeface());
        args.putString(GpxFile.TAG_DESC, sliderPage.getDescriptionString());
        args.putString("desc_typeface", sliderPage.getDescTypeface());
        args.putInt(ARG_CUSTOM_DRAWABLE, sliderPage.getImageDrawable());
        args.putInt("bg_color", sliderPage.getBgColor());
        args.putInt("title_color", sliderPage.getTitleColor());
        args.putInt("desc_color", sliderPage.getDescColor());
        slide.setArguments(args);
        return slide;
    }

    public void onDestroy() {
        super.onDestroy();
        this.mImageView = null;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = super.onCreateView(inflater, container, savedInstanceState);
        if ($assertionsDisabled || rootView != null) {
            this.mImageView = (CircleImageView) rootView.findViewById(R.id.image);
            ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int viewWidth = IntroductionFragment.this.mImageView.getMeasuredWidth();
                        int viewHeight = IntroductionFragment.this.mImageView.getMeasuredHeight();
                        if (viewWidth < viewHeight) {
                            viewHeight = viewWidth;
                        }
                        if (viewWidth > viewHeight) {
                            viewWidth = viewHeight;
                        }
                        IntroductionFragment.this.mImageView.setImageBitmap(IntroductionFragment.decodeSampledBitmapFromResource(IntroductionFragment.this.getResources(), IntroductionFragment.this.getArguments().getInt(IntroductionFragment.ARG_CUSTOM_DRAWABLE), viewWidth, viewHeight));
                    }
                });
            }
            return rootView;
        }
        throw new AssertionError();
    }

    protected int getLayoutId() {
        return R.layout.fragment_introduction;
    }

    public static int calculateInSampleSize(Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            while (height / inSampleSize > reqHeight && width / inSampleSize >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}
