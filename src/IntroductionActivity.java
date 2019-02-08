package mobi.maptrek;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.model.SliderPage;
import mobi.maptrek.fragments.IntroductionFragment;

public class IntroductionActivity extends AppIntro {
    public static final int CURRENT_INTRODUCTION = 3;
    int mLastSeenIntroduction;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(1024, 1024);
        this.mLastSeenIntroduction = Configuration.getLastSeenIntroduction();
        SliderPage sliderPage = new SliderPage();
        sliderPage.setBgColor(Color.parseColor("#2196F3"));
        if (this.mLastSeenIntroduction < 1) {
            sliderPage.setTitle("Hillshades");
            sliderPage.setDescription("Now you can download and view hillshades atop maps. Your custom maps will be shaded as well.");
            sliderPage.setImageDrawable(R.mipmap.hillshades);
            addSlide(IntroductionFragment.newInstance(sliderPage));
            this.mLastSeenIntroduction = 1;
        }
        if (this.mLastSeenIntroduction < 2) {
            sliderPage.setTitle("Location sharing");
            sliderPage.setDescription("Now you have more options on how to share your place or location on map. Not only you can send its coordinates as text, but you can open it in some other map application or share it as GPX or KML file.");
            sliderPage.setImageDrawable(R.mipmap.sharepoint);
            addSlide(IntroductionFragment.newInstance(sliderPage));
            this.mLastSeenIntroduction = 2;
        }
        if (this.mLastSeenIntroduction < 3) {
            sliderPage.setTitle("Hiking");
            sliderPage.setDescription("Hiking activity mode emphasises paths and tracks. It visualizes path difficulty and visibility and shows hiking routes.");
            sliderPage.setImageDrawable(R.mipmap.hiking);
            addSlide(IntroductionFragment.newInstance(sliderPage));
            sliderPage.setTitle("Skiing and skating");
            sliderPage.setDescription("Skiing activity mode displays clean winter map with mostly all skiing activities: downhill, nordic, hiking and touring. As a bonus freestyle snow-boarding, skating and sleighing areas are displayed.");
            sliderPage.setImageDrawable(R.mipmap.skiing);
            addSlide(IntroductionFragment.newInstance(sliderPage));
            this.mLastSeenIntroduction = 3;
        }
    }

    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        Configuration.setLastSeenIntroduction(this.mLastSeenIntroduction);
        finish();
    }

    public void onDonePressed(Fragment currentFragment) {
        Configuration.setLastSeenIntroduction(this.mLastSeenIntroduction);
        finish();
    }
}
