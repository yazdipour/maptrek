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

public class TrackProperties extends Fragment {
    public static final String ARG_COLOR = "color";
    public static final String ARG_NAME = "name";
    private int mColor;
    private ColorPickerSwatch mColorSwatch;
    private FragmentHolder mFragmentHolder;
    private OnTrackPropertiesChangedListener mListener;
    private String mName;
    private EditText mNameEdit;

    public interface OnTrackPropertiesChangedListener {
        void onTrackPropertiesChanged(String str, int i);
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
        this.mNameEdit.setText(this.mName);
        this.mColorSwatch.setColor(this.mColor);
        if (savedInstanceState != null) {
            this.mNameEdit.setText(savedInstanceState.getString("name"));
            this.mColorSwatch.setColor(savedInstanceState.getInt("color"));
        }
        this.mNameEdit.setText(this.mName);
        this.mNameEdit.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == 6) {
                    TrackProperties.this.returnResult();
                    TrackProperties.this.mFragmentHolder.popCurrent();
                }
                return false;
            }
        });
        this.mColorSwatch.setColor(this.mColor);
        this.mColorSwatch.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ColorPickerDialog dialog = new ColorPickerDialog();
                dialog.setColors(MarkerStyle.DEFAULT_COLORS, TrackProperties.this.mColor);
                dialog.setArguments(R.string.color_picker_default_title, 4, 2);
                dialog.setOnColorSelectedListener(new OnColorSelectedListener() {
                    public void onColorSelected(int color) {
                        TrackProperties.this.mColorSwatch.setColor(color);
                        TrackProperties.this.mColor = color;
                    }
                });
                dialog.show(TrackProperties.this.getFragmentManager(), "ColorPickerDialog");
            }
        });
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnTrackPropertiesChangedListener) context;
            this.mFragmentHolder = (FragmentHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTrackPropertiesChangedListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mListener = null;
        this.mFragmentHolder = null;
    }

    public void onDestroyView() {
        super.onDestroyView();
        returnResult();
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("name", this.mNameEdit.getText().toString());
        outState.putInt("color", this.mColorSwatch.getColor());
    }

    private void returnResult() {
        String name = this.mNameEdit.getText().toString();
        int color = this.mColorSwatch.getColor();
        if (!name.equals(this.mName) || color != this.mColor) {
            this.mListener.onTrackPropertiesChanged(name, color);
            this.mName = name;
            this.mColor = color;
        }
    }
}
