package mobi.maptrek.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.ActionProvider;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnActionExpandListener;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;

public class PanelMenuItem implements MenuItem {
    public static final int HEADER_ID_UNDEFINED = -1;
    private View actionView;
    private Boolean checked;
    private Drawable icon;
    private int id = -1;
    private Intent intent;
    private Context mContext;
    private CharSequence title;

    public PanelMenuItem(Context context) {
        this.mContext = context;
    }

    public PanelMenuItem setItemId(int id) {
        this.id = id;
        return this;
    }

    public int getItemId() {
        return this.id;
    }

    public int getGroupId() {
        return 0;
    }

    public int getOrder() {
        return 0;
    }

    public PanelMenuItem setTitle(CharSequence title) {
        this.title = title;
        return this;
    }

    public PanelMenuItem setTitle(@StringRes int title) {
        this.title = this.mContext.getString(title);
        return this;
    }

    public CharSequence getTitle() {
        return this.title;
    }

    public PanelMenuItem setTitleCondensed(CharSequence title) {
        return this;
    }

    public CharSequence getTitleCondensed() {
        return null;
    }

    public PanelMenuItem setIcon(Drawable icon) {
        this.icon = icon;
        return this;
    }

    public PanelMenuItem setIcon(@DrawableRes int iconRes) {
        this.icon = this.mContext.getDrawable(iconRes);
        return this;
    }

    public Drawable getIcon() {
        return this.icon;
    }

    public PanelMenuItem setIntent(Intent intent) {
        this.intent = intent;
        return this;
    }

    public Intent getIntent() {
        return this.intent;
    }

    public PanelMenuItem setShortcut(char numericChar, char alphaChar) {
        return this;
    }

    public PanelMenuItem setNumericShortcut(char numericChar) {
        return this;
    }

    public char getNumericShortcut() {
        return '\u0000';
    }

    public PanelMenuItem setAlphabeticShortcut(char alphaChar) {
        return this;
    }

    public char getAlphabeticShortcut() {
        return '\u0000';
    }

    public PanelMenuItem setCheckable(boolean checkable) {
        if (checkable) {
            this.checked = Boolean.FALSE;
        }
        return this;
    }

    public boolean isCheckable() {
        return this.checked != null;
    }

    public PanelMenuItem setChecked(boolean checked) {
        this.checked = Boolean.valueOf(checked);
        return this;
    }

    public boolean isChecked() {
        return this.checked != null && this.checked.booleanValue();
    }

    public PanelMenuItem setVisible(boolean visible) {
        return this;
    }

    public boolean isVisible() {
        return true;
    }

    public PanelMenuItem setEnabled(boolean enabled) {
        return this;
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean hasSubMenu() {
        return false;
    }

    public SubMenu getSubMenu() {
        return null;
    }

    public PanelMenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        return this;
    }

    public ContextMenuInfo getMenuInfo() {
        return null;
    }

    public void setShowAsAction(int actionEnum) {
    }

    public PanelMenuItem setShowAsActionFlags(int actionEnum) {
        return this;
    }

    public PanelMenuItem setActionView(View view) {
        this.actionView = view;
        return this;
    }

    public PanelMenuItem setActionView(int resId) {
        this.actionView = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(resId, null);
        return this;
    }

    public View getActionView() {
        return this.actionView;
    }

    public PanelMenuItem setActionProvider(ActionProvider actionProvider) {
        return this;
    }

    public ActionProvider getActionProvider() {
        return null;
    }

    public boolean expandActionView() {
        return false;
    }

    public boolean collapseActionView() {
        return false;
    }

    public boolean isActionViewExpanded() {
        return false;
    }

    public PanelMenuItem setOnActionExpandListener(OnActionExpandListener listener) {
        return this;
    }
}
