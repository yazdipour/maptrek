package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.util.Locale;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.provider.ExportProvider;

public class CrashReport extends Fragment implements OnBackPressedListener {
    private FragmentHolder mFragmentHolder;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crash_report, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FloatingActionButton floatingButton = this.mFragmentHolder.enableActionButton();
        floatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_send));
        floatingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.SEND");
                intent.putExtra("android.intent.extra.EMAIL", new String[]{"novikov+maptrek@gmail.com"});
                intent.putExtra("android.intent.extra.STREAM", ExportProvider.getUriForFile(CrashReport.this.getContext(), MapTrek.getApplication().getExceptionLog()));
                intent.setType("vnd.android.cursor.dir/email");
                intent.putExtra("android.intent.extra.SUBJECT", "MapTrek crash report");
                StringBuilder text = new StringBuilder();
                text.append("Device : ").append(Build.DEVICE);
                text.append("\nBrand : ").append(Build.BRAND);
                text.append("\nModel : ").append(Build.MODEL);
                text.append("\nProduct : ").append(Build.PRODUCT);
                text.append("\nLocale : ").append(Locale.getDefault().toString());
                text.append("\nBuild : ").append(Build.DISPLAY);
                text.append("\nVersion : ").append(VERSION.RELEASE);
                try {
                    PackageInfo info = CrashReport.this.getActivity().getPackageManager().getPackageInfo(CrashReport.this.getActivity().getPackageName(), 0);
                    if (info != null) {
                        text.append("\nApk Version : ").append(info.versionCode).append(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR).append(info.versionName);
                    }
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
                intent.putExtra("android.intent.extra.TEXT", text.toString());
                CrashReport.this.startActivity(Intent.createChooser(intent, CrashReport.this.getString(R.string.send_crash_report)));
                CrashReport.this.mFragmentHolder.disableActionButton();
                CrashReport.this.mFragmentHolder.popCurrent();
            }
        });
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mFragmentHolder = (FragmentHolder) context;
            this.mFragmentHolder.addBackClickListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mFragmentHolder.removeBackClickListener(this);
        this.mFragmentHolder = null;
    }

    public boolean onBackClick() {
        this.mFragmentHolder.disableActionButton();
        return false;
    }
}
