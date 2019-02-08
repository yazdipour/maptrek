package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.os.EnvironmentCompat;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import mobi.maptrek.R;

public class About extends Fragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        updateAboutInfo(view);
        return view;
    }

    private void updateAboutInfo(View view) {
        String versionName;
        Reader in;
        Throwable th;
        int rsz;
        Throwable th2;
        try {
            versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            int versionBuild = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            versionName = EnvironmentCompat.MEDIA_UNKNOWN;
        }
        ((TextView) view.findViewById(R.id.version)).setText(getString(R.string.version, new Object[]{versionName}));
        StringBuilder links = new StringBuilder();
        links.append("<a href=\"");
        links.append("http://maptrek.mobi/");
        links.append("\">");
        links.append("http://maptrek.mobi/");
        links.append("</a>");
        TextView homeLinks = (TextView) view.findViewById(R.id.links);
        homeLinks.setText(Html.fromHtml(links.toString()));
        homeLinks.setMovementMethod(LinkMovementMethod.getInstance());
        Resources resources = getResources();
        InputStream is = resources.openRawResource(R.raw.license);
        char[] buffer = new char[100];
        StringBuilder out = new StringBuilder();
        try {
            in = new InputStreamReader(is, "UTF-8");
            th = null;
            while (true) {
                try {
                    rsz = in.read(buffer, 0, buffer.length);
                    if (rsz < 0) {
                        break;
                    }
                    out.append(buffer, 0, rsz);
                } catch (Throwable th3) {
                    Throwable th4 = th3;
                    th3 = th2;
                    th2 = th4;
                }
            }
            if (in != null) {
                if (null != null) {
                    try {
                        in.close();
                    } catch (Throwable th22) {
                        null.addSuppressed(th22);
                    }
                } else {
                    in.close();
                }
            }
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        TextView license = (TextView) view.findViewById(R.id.license);
        license.setText(Html.fromHtml(out.toString()));
        license.setMovementMethod(LinkMovementMethod.getInstance());
        is = resources.openRawResource(R.raw.credits);
        out = new StringBuilder();
        try {
            in = new InputStreamReader(is, "UTF-8");
            th3 = null;
            while (true) {
                try {
                    rsz = in.read(buffer, 0, buffer.length);
                    if (rsz < 0) {
                        break;
                    }
                    out.append(buffer, 0, rsz);
                } catch (Throwable th32) {
                    th4 = th32;
                    th32 = th22;
                    th22 = th4;
                }
            }
            if (in != null) {
                if (null != null) {
                    try {
                        in.close();
                    } catch (Throwable th222) {
                        null.addSuppressed(th222);
                    }
                } else {
                    in.close();
                }
            }
        } catch (IOException e22) {
            e22.printStackTrace();
        }
        TextView credits = (TextView) view.findViewById(R.id.credits);
        credits.setText(Html.fromHtml(out.toString()));
        credits.setMovementMethod(LinkMovementMethod.getInstance());
        return;
        throw th222;
        if (in != null) {
            if (th32 != null) {
                try {
                    in.close();
                } catch (Throwable th5) {
                    th32.addSuppressed(th5);
                }
            } else {
                in.close();
            }
        }
        throw th222;
        throw th222;
        if (in != null) {
            if (th32 != null) {
                try {
                    in.close();
                } catch (Throwable th52) {
                    th32.addSuppressed(th52);
                }
            } else {
                in.close();
            }
        }
        throw th222;
    }
}
