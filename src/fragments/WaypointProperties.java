package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import info.andreynovikov.androidcolorpicker.ColorPickerDialog;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch;
import info.andreynovikov.androidcolorpicker.ColorPickerSwatch.OnColorSelectedListener;
import mobi.maptrek.R;
import mobi.maptrek.data.style.MarkerStyle;

public class WaypointProperties extends Fragment implements OnBackPressedListener {
    public static final String ARG_COLOR = "color";
    public static final String ARG_NAME = "name";
    private int mColor;
    private ColorPickerSwatch mColorSwatch;
    private FragmentHolder mFragmentHolder;
    private OnWaypointPropertiesChangedListener mListener;
    private String mName;
    private EditText mNameEdit;

    public interface OnWaypointPropertiesChangedListener {
        void onWaypointPropertiesChanged(String str, int i);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_track_properties, container, false);
        this.mNameEdit = (EditText) rootView.findViewById(R.id.nameEdit);
        this.mColorSwatch = (ColorPickerSwatch) rootView.findViewById(R.id.colorSwatch);
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mName = getArguments().getString("name");
        this.mColor = getArguments().getInt("color");
        String name = this.mName;
        int color = this.mColor;
        if (savedInstanceState != null) {
            name = savedInstanceState.getString("name");
            color = savedInstanceState.getInt("color");
        }
        this.mNameEdit.setText(name);
        this.mNameEdit.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == 6) {
                    WaypointProperties.this.returnResult();
                    WaypointProperties.this.mFragmentHolder.popCurrent();
                }
                return false;
            }
        });
        this.mColorSwatch.setColor(color);
        this.mColorSwatch.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ColorPickerDialog dialog = new ColorPickerDialog();
                dialog.setColors(MarkerStyle.DEFAULT_COLORS, WaypointProperties.this.mColor);
                dialog.setArguments(R.string.color_picker_default_title, 4, 2);
                dialog.setOnColorSelectedListener(new OnColorSelectedListener() {
                    public void onColorSelected(int color) {
                        WaypointProperties.this.mColorSwatch.setColor(color);
                    }
                });
                dialog.show(WaypointProperties.this.getFragmentManager(), "ColorPickerDialog");
            }
        });
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnWaypointPropertiesChangedListener) context;
            this.mFragmentHolder = (FragmentHolder) context;
            this.mFragmentHolder.addBackClickListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnWaypointPropertiesChangedListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mFragmentHolder.removeBackClickListener(this);
        this.mFragmentHolder = null;
        this.mListener = null;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("name", this.mNameEdit.getText().toString());
        outState.putInt("color", this.mColorSwatch.getColor());
    }

    public boolean onBackClick() {
        returnResult();
        return false;
    }

    private void returnResult() {
        String name = this.mNameEdit.getText().toString();
        int color = this.mColorSwatch.getColor();
        if (!name.equals(this.mName) || color != this.mColor) {
            this.mListener.onWaypointPropertiesChanged(name, color);
            this.mName = name;
            this.mColor = color;
        }
    }
}
