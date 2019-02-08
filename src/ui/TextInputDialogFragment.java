package mobi.maptrek.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import mobi.maptrek.R;

public class TextInputDialogFragment extends DialogFragment implements OnPrimaryClipChangedListener {
    private TextInputDialogCallback mCallback;
    private ClipboardManager mClipboard;
    private TextView mDescription;
    private ImageButton mPasteButton;
    private boolean mShowPasteButton;

    public static class Builder {
        private TextInputDialogCallback mCallbacks;
        private String mHint;
        private String mId;
        private Integer mInputType;
        private String mOldValue;
        private Boolean mSelectAllOnFocus;
        private Boolean mShowPasteButton;
        private String mTitle;

        public Builder setTitle(String title) {
            this.mTitle = title;
            return this;
        }

        public Builder setOldValue(String oldValue) {
            this.mOldValue = oldValue;
            return this;
        }

        public Builder setHint(String hint) {
            this.mHint = hint;
            return this;
        }

        public Builder setSelectAllOnFocus(boolean selectAllOnFocus) {
            this.mSelectAllOnFocus = Boolean.valueOf(selectAllOnFocus);
            return this;
        }

        public Builder setShowPasteButton(boolean showPasteButton) {
            this.mShowPasteButton = Boolean.valueOf(showPasteButton);
            return this;
        }

        public Builder setInputType(int inputType) {
            this.mInputType = Integer.valueOf(inputType);
            return this;
        }

        public Builder setId(String id) {
            this.mId = id;
            return this;
        }

        public Builder setCallbacks(TextInputDialogCallback callbacks) {
            this.mCallbacks = callbacks;
            return this;
        }

        public TextInputDialogFragment create() {
            TextInputDialogFragment dialogFragment = new TextInputDialogFragment();
            Bundle args = new Bundle();
            if (this.mTitle != null) {
                args.putString("title", this.mTitle);
            }
            if (this.mOldValue != null) {
                args.putString("oldValue", this.mOldValue);
            }
            if (this.mHint != null) {
                args.putString("hint", this.mHint);
            }
            if (this.mSelectAllOnFocus != null) {
                args.putBoolean("selectAllOnFocus", this.mSelectAllOnFocus.booleanValue());
            }
            if (this.mInputType != null) {
                args.putInt("inputType", this.mInputType.intValue());
            }
            if (this.mShowPasteButton != null) {
                args.putBoolean("showPasteButton", this.mShowPasteButton.booleanValue());
            }
            if (this.mId != null) {
                args.putString("id", this.mId);
            }
            dialogFragment.setCallback(this.mCallbacks);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }

    public interface TextInputDialogCallback {
        void afterTextChanged(Editable editable);

        void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3);

        void onTextChanged(CharSequence charSequence, int i, int i2, int i3);

        void onTextInputNegativeClick(String str);

        void onTextInputPositiveClick(String str, String str2);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.mClipboard = (ClipboardManager) context.getSystemService("clipboard");
    }

    public void onDetach() {
        super.onDetach();
        this.mClipboard = null;
    }

    public void onResume() {
        super.onResume();
        onPrimaryClipChanged();
        this.mClipboard.addPrimaryClipChangedListener(this);
    }

    public void onPause() {
        super.onPause();
        this.mClipboard.removePrimaryClipChangedListener(this);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString("title", "");
        String oldValue = args.getString("oldValue", "");
        boolean selectAllOnFocus = args.getBoolean("selectAllOnFocus", false);
        this.mShowPasteButton = args.getBoolean("showPasteButton", false);
        int inputType = args.getInt("inputType", 1);
        String hint = args.getString("hint", null);
        final String id = args.getString("id", null);
        Activity activity = getActivity();
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_text_input, null);
        final EditText textEdit = (EditText) dialogView.findViewById(R.id.textEdit);
        textEdit.setInputType(inputType);
        if (!"".equals(oldValue)) {
            textEdit.setText(oldValue);
        }
        textEdit.setSelectAllOnFocus(selectAllOnFocus);
        textEdit.requestFocus();
        textEdit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (TextInputDialogFragment.this.mCallback != null) {
                    TextInputDialogFragment.this.mCallback.beforeTextChanged(s, start, count, after);
                }
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextInputDialogFragment.this.mCallback != null) {
                    TextInputDialogFragment.this.mCallback.onTextChanged(s, start, before, count);
                }
            }

            public void afterTextChanged(Editable s) {
                if (TextInputDialogFragment.this.mCallback != null) {
                    TextInputDialogFragment.this.mCallback.afterTextChanged(s);
                }
            }
        });
        if (hint != null) {
            ((TextInputLayout) dialogView.findViewById(R.id.textWrapper)).setHint(hint);
        }
        if (this.mShowPasteButton) {
            this.mPasteButton = (ImageButton) dialogView.findViewById(R.id.pasteButton);
            this.mPasteButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (TextInputDialogFragment.this.mClipboard != null) {
                        CharSequence pasteData = TextInputDialogFragment.this.mClipboard.getPrimaryClip().getItemAt(0).getText();
                        if (pasteData != null) {
                            textEdit.setText(pasteData);
                        }
                    }
                }
            });
            onPrimaryClipChanged();
        }
        this.mDescription = (TextView) dialogView.findViewById(R.id.description);
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(activity);
        dialogBuilder.setTitle(title);
        dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                TextInputDialogFragment.this.mCallback.onTextInputPositiveClick(id, textEdit.getText().toString());
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                TextInputDialogFragment.this.mCallback.onTextInputNegativeClick(id);
            }
        });
        dialogBuilder.setView(dialogView);
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.getWindow().setSoftInputMode(5);
        return alertDialog;
    }

    public void onPrimaryClipChanged() {
        if (this.mShowPasteButton) {
            int visibility = 8;
            if (this.mClipboard.hasPrimaryClip() && this.mClipboard.getPrimaryClipDescription().hasMimeType("text/plain")) {
                visibility = 0;
            }
            this.mPasteButton.setVisibility(visibility);
        }
    }

    public void setDescription(@NonNull CharSequence text) {
        this.mDescription.setVisibility(text.length() > 0 ? 0 : 8);
        this.mDescription.setText(text);
    }

    public void setCallback(TextInputDialogCallback callback) {
        this.mCallback = callback;
    }
}
