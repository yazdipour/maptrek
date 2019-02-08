package mobi.maptrek.fragments;

import android.annotation.SuppressLint;
import android.app.ListFragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.MenuRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import mobi.maptrek.R;
import mobi.maptrek.util.XmlUtils;
import mobi.maptrek.view.PanelMenu;
import mobi.maptrek.view.PanelMenu.OnPrepareMenuListener;

public class PanelMenuFragment extends ListFragment implements PanelMenu {
    private MenuListAdapter mAdapter;
    private FragmentHolder mFragmentHolder;
    private HashMap<Integer, PanelMenuItem> mItemsMap;
    private int mMenuId;
    private ArrayList<PanelMenuItem> mMenuItems;
    private OnPrepareMenuListener mOnPrepareMenuListener;
    private boolean mPopulating;

    private static class MenuItemHolder {
        ViewGroup action;
        Switch check;
        ImageView icon;
        TextView title;

        private MenuItemHolder() {
        }
    }

    public class MenuListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        MenuListAdapter(Context context) {
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        }

        public PanelMenuItem getItem(int position) {
            return (PanelMenuItem) PanelMenuFragment.this.mMenuItems.get(position);
        }

        public long getItemId(int position) {
            return (long) ((PanelMenuItem) PanelMenuFragment.this.mMenuItems.get(position)).getItemId();
        }

        public int getCount() {
            return PanelMenuFragment.this.mMenuItems.size();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            MenuItemHolder itemHolder;
            int i;
            final int i2;
            final PanelMenuItem item = getItem(position);
            View actionView = item.getActionView();
            if (actionView != null) {
                if (actionView.getTag() == null) {
                    itemHolder = new MenuItemHolder();
                    convertView = this.mInflater.inflate(R.layout.menu_item, parent, false);
                    itemHolder.title = (TextView) convertView.findViewById(R.id.title);
                    itemHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    itemHolder.check = (Switch) convertView.findViewById(R.id.check);
                    itemHolder.action = (ViewGroup) convertView.findViewById(R.id.actionViewContainer);
                    itemHolder.action.addView(actionView, itemHolder.action.getLayoutParams());
                    itemHolder.action.setVisibility(0);
                    actionView.setTag(convertView);
                    actionView.setTag(R.id.itemHolder, itemHolder);
                } else {
                    convertView = (View) actionView.getTag();
                    itemHolder = (MenuItemHolder) actionView.getTag(R.id.itemHolder);
                }
            } else if (convertView == null || convertView.getTag() == null) {
                itemHolder = new MenuItemHolder();
                convertView = this.mInflater.inflate(R.layout.menu_item, parent, false);
                itemHolder.title = (TextView) convertView.findViewById(R.id.title);
                itemHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                itemHolder.check = (Switch) convertView.findViewById(R.id.check);
                itemHolder.action = (ViewGroup) convertView.findViewById(R.id.actionViewContainer);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (MenuItemHolder) convertView.getTag();
            }
            itemHolder.title.setText(item.getTitle());
            itemHolder.icon.setImageDrawable(item.getIcon());
            Switch switchR = itemHolder.check;
            if (item.isCheckable()) {
                i = 0;
            } else {
                i = 8;
            }
            switchR.setVisibility(i);
            final View view = convertView;
            final ListView listView = PanelMenuFragment.this.getListView();
            if (item.isCheckable()) {
                itemHolder.check.setOnCheckedChangeListener(null);
                itemHolder.check.setChecked(item.isChecked());
                itemHolder.check.setVisibility(0);
                i2 = position;
                itemHolder.check.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        item.setChecked(itemHolder.check.isChecked());
                        PanelMenuFragment.this.onListItemClick(listView, view, i2, MenuListAdapter.this.getItemId(i2));
                    }
                });
            } else {
                itemHolder.check.setVisibility(8);
                itemHolder.check.setOnCheckedChangeListener(null);
            }
            if (actionView == null) {
                itemHolder.action.setVisibility(8);
            }
            i2 = position;
            convertView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    item.setChecked(!itemHolder.check.isChecked());
                    PanelMenuFragment.this.onListItemClick(listView, view, i2, MenuListAdapter.this.getItemId(i2));
                }
            });
            return convertView;
        }

        public boolean hasStableIds() {
            return true;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.menu_list_with_empty_view, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        populateMenu();
        this.mAdapter = new MenuListAdapter(getActivity());
        setListAdapter(this.mAdapter);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.mFragmentHolder = (FragmentHolder) context;
    }

    public void onDetach() {
        super.onDetach();
        this.mFragmentHolder = null;
    }

    public void onListItemClick(ListView lv, View v, int position, long id) {
        OnMenuItemClickListener listener = (OnMenuItemClickListener) getActivity();
        this.mFragmentHolder.popCurrent();
        listener.onMenuItemClick((MenuItem) this.mMenuItems.get(position));
    }

    public void setMenu(@MenuRes int menuId, OnPrepareMenuListener onPrepareMenuListener) {
        this.mMenuId = menuId;
        this.mOnPrepareMenuListener = onPrepareMenuListener;
        if (isVisible()) {
            populateMenu();
            this.mAdapter.notifyDataSetChanged();
        }
    }

    @Nullable
    public PanelMenuItem findItem(@IdRes int id) {
        return (PanelMenuItem) this.mItemsMap.get(Integer.valueOf(id));
    }

    public MenuItem add(@IdRes int id, int order, CharSequence title) {
        PanelMenuItem item = new PanelMenuItem(getContext());
        if (id == -1) {
            id = View.generateViewId();
        }
        item.setItemId(id);
        item.setTitle(title);
        this.mMenuItems.add(order, item);
        this.mItemsMap.put(Integer.valueOf(id), item);
        if (isVisible() && !this.mPopulating) {
            this.mAdapter.notifyDataSetChanged();
        }
        return item;
    }

    public void removeItem(@IdRes int id) {
        this.mMenuItems.remove(this.mItemsMap.remove(Integer.valueOf(id)));
        if (isVisible() && !this.mPopulating) {
            this.mAdapter.notifyDataSetChanged();
        }
    }

    @SuppressLint({"UseSparseArrays"})
    private void populateMenu() {
        this.mPopulating = true;
        this.mMenuItems = new ArrayList();
        this.mItemsMap = new HashMap();
        loadHeadersFromResource(this.mMenuId, this.mMenuItems);
        Iterator it = this.mMenuItems.iterator();
        while (it.hasNext()) {
            PanelMenuItem item = (PanelMenuItem) it.next();
            this.mItemsMap.put(Integer.valueOf(item.getItemId()), item);
        }
        if (this.mOnPrepareMenuListener != null) {
            this.mOnPrepareMenuListener.onPrepareMenu(this);
        }
        this.mPopulating = false;
    }

    private void loadHeadersFromResource(@MenuRes int resId, List<PanelMenuItem> target) {
        Exception e;
        XmlResourceParser parser = null;
        try {
            int type;
            parser = getResources().getXml(resId);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            do {
                type = parser.next();
                if (type == 1) {
                    break;
                }
            } while (type != 2);
            String nodeName = parser.getName();
            if ("menu".equals(nodeName)) {
                int outerDepth = parser.getDepth();
                while (true) {
                    type = parser.next();
                    if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                        if (parser != null) {
                            parser.close();
                            return;
                        }
                        return;
                    } else if (!(type == 3 || type == 4)) {
                        if ("item".equals(parser.getName())) {
                            PanelMenuItem item = new PanelMenuItem(getContext());
                            TypedArray sa = getContext().obtainStyledAttributes(attrs, new int[]{16842960, 16843233, 16842754, 16843237, 16843515});
                            item.setItemId(sa.getResourceId(0, -1));
                            TypedValue tv = sa.peekValue(1);
                            if (tv != null && tv.type == 3) {
                                if (tv.resourceId != 0) {
                                    item.setTitle(tv.resourceId);
                                } else {
                                    item.setTitle(tv.string);
                                }
                            }
                            int iconRes = sa.getResourceId(2, 0);
                            if (iconRes != 0) {
                                item.setIcon(iconRes);
                            }
                            item.setCheckable(sa.getBoolean(3, false));
                            int actionRes = sa.getResourceId(4, 0);
                            if (actionRes != 0) {
                                item.setActionView(actionRes);
                            }
                            sa.recycle();
                            target.add(item);
                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
                if (parser != null) {
                    parser.close();
                    return;
                }
                return;
            }
            throw new RuntimeException("XML document must start with <menu> tag; found" + nodeName + " at " + parser.getPositionDescription());
        } catch (Exception e2) {
            e = e2;
            try {
                throw new RuntimeException("Error parsing headers", e);
            } catch (Throwable th) {
                if (parser != null) {
                    parser.close();
                }
            }
        } catch (Exception e22) {
            e = e22;
            throw new RuntimeException("Error parsing headers", e);
        }
    }
}
