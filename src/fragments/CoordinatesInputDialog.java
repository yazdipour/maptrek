package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import mobi.maptrek.R;
import mobi.maptrek.util.JosmCoordinatesParser;
import mobi.maptrek.util.JosmCoordinatesParser.Result;

public class CoordinatesInputDialog extends DialogFragment {
    private CoordinatesInputDialogCallback mCallback;
    private int mColorDarkBlue;
    private int mColorRed;
    private int mColorTextPrimary;
    private AlertDialog mDialog;
    private String mLineSeparator = System.getProperty("line.separator");

    public static class Builder {
        private CoordinatesInputDialogCallback mCallbacks;
        private String mId;
        private String mTitle;

        public Builder setTitle(String title) {
            this.mTitle = title;
            return this;
        }

        public Builder setId(String id) {
            this.mId = id;
            return this;
        }

        public Builder setCallbacks(CoordinatesInputDialogCallback callbacks) {
            this.mCallbacks = callbacks;
            return this;
        }

        public CoordinatesInputDialog create() {
            CoordinatesInputDialog dialogFragment = new CoordinatesInputDialog();
            Bundle args = new Bundle();
            if (this.mTitle != null) {
                args.putString("title", this.mTitle);
            }
            if (this.mId != null) {
                args.putString("id", this.mId);
            }
            dialogFragment.setCallback(this.mCallbacks);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }

    public interface CoordinatesInputDialogCallback {
        void onTextInputNegativeClick(String str);

        void onTextInputPositiveClick(String str, String str2);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.mColorTextPrimary = context.getColor(R.color.textColorPrimary);
        this.mColorDarkBlue = context.getColor(R.color.darkBlue);
        this.mColorRed = context.getColor(R.color.red);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString("title", "");
        final String id = args.getString("id", null);
        Activity activity = getActivity();
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_coordinates_input, null);
        final EditText textEdit = (EditText) dialogView.findViewById(R.id.coordinatesEdit);
        textEdit.requestFocus();
        textEdit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                int i = 0;
                if (s.length() != 0) {
                    String[] lines = s.toString().split(CoordinatesInputDialog.this.mLineSeparator);
                    int offset = 0;
                    ForegroundColorSpan[] spans = (ForegroundColorSpan[]) s.getSpans(0, s.length(), ForegroundColorSpan.class);
                    if (spans != null && spans.length > 0) {
                        Log.e("CID", "L: " + spans.length);
                        for (ForegroundColorSpan span : spans) {
                            s.removeSpan(span);
                        }
                    }
                    int length = lines.length;
                    while (i < length) {
                        String line = lines[i];
                        try {
                            Result result = JosmCoordinatesParser.parseWithResult(line);
                            s.setSpan(new ForegroundColorSpan(CoordinatesInputDialog.this.mColorDarkBlue), offset, result.offset + offset, 33);
                            s.setSpan(new ForegroundColorSpan(CoordinatesInputDialog.this.mColorTextPrimary), result.offset + offset, s.length(), 33);
                        } catch (IllegalArgumentException e) {
                            s.setSpan(new ForegroundColorSpan(CoordinatesInputDialog.this.mColorRed), offset, s.length(), 33);
                        }
                        offset += line.length() + CoordinatesInputDialog.this.mLineSeparator.length();
                        i++;
                    }
                }
            }
        });
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(activity);
        dialogBuilder.setTitle(title);
        dialogBuilder.setPositiveButton(R.string.ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                CoordinatesInputDialog.this.mCallback.onTextInputPositiveClick(id, textEdit.getText().toString());
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                CoordinatesInputDialog.this.mCallback.onTextInputNegativeClick(id);
            }
        });
        dialogBuilder.setNeutralButton(R.string.explain, null);
        dialogBuilder.setView(dialogView);
        this.mDialog = dialogBuilder.create();
        this.mDialog.getWindow().setSoftInputMode(5);
        this.mDialog.setOnShowListener(new OnShowListener() {
            public void onShow(DialogInterface dialogInterface) {
                CoordinatesInputDialog.this.mDialog.getButton(-3).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ((InputMethodManager) CoordinatesInputDialog.this.getActivity().getSystemService("input_method")).hideSoftInputFromWindow(CoordinatesInputDialog.this.mDialog.getWindow().getDecorView().getWindowToken(), 0);
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(CoordinatesInputDialog.this.getActivity());
                        builder.setMessage(R.string.msgCoordinatesInputExplanation);
                        builder.setPositiveButton(R.string.ok, null);
                        builder.create().show();
                    }
                });
            }
        });
        return this.mDialog;
    }

    public void setCallback(CoordinatesInputDialogCallback callback) {
        this.mCallback = callback;
    }
}
