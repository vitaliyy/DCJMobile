package org.diacalc.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.diacalc.android.manager.DatabaseManager;
import org.diacalc.android.products.ProductGroup;
import org.diacalc.android.products.ProductInMenu;
import org.diacalc.android.products.ProductInSearch;
import org.diacalc.android.products.ProductW;


import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;



public class Search extends ListActivity {
	private static final String DCJ_TAG = null;
	public static boolean on_search = false;
	private ProductProxyAdapter2 adapter;
	private ArrayList<ProductInMenu> menuProds;
	DatabaseManager mgr;
	SimpleCursorAdapter scAdapter;
	Cursor cursor;
	private DataPocket dtPkt;
	private int groupSelected = -1;
	private int prodSelected = -1;
	private ArrayList<ProductGroup> groups;
	private Comparator<ProductW> prodsComparator = new Comparator<ProductW>(){
		public int compare(ProductW p1, ProductW p2) {
			return p1.getName().compareTo(p2.getName());
		}
    	
    };
    
    @Override
    public void onBackPressed()
    {
      super.onDestroy();
      
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	onSearchRequested();	
    }
    
	@Override
public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		onSearchRequested();
		setContentView(R.layout.search);
		mgr = new DatabaseManager(this);
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) { 
			
			String query = intent.getStringExtra(SearchManager.QUERY);
		
			dtPkt = (DataPocket)this.getApplication();
			menuProds = dtPkt.getMenuProds(mgr);
			groups = dtPkt.getGroups(mgr);
			adapter = new ProductProxyAdapter2(this, mgr.fetchRecordsByQuery(query));
			groupSelected = 0;
	    	adapter.filter(groups.get(groupSelected).getId());
	    	getListView().setAdapter(adapter);
			 
		if (prodSelected>-1) getListView().setSelection(prodSelected);
			
			getListView().setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					ProductInSearch pr = (ProductInSearch)adapter.getItem(position);
					if (pr.isSelected()){
						for(int i=0;i<menuProds.size();i++){
							if (menuProds.get(i).getId()==pr.getId()){
								menuProds.remove(i);
								break;
							}
						}
					}else{
						menuProds.add(new ProductInMenu(pr));
						Collections.sort(menuProds, prodsComparator);
					}
					dtPkt.setMenuNeed2Save();
					
					pr.setSelected(!pr.isSelected());
					adapter.notifyDataSetChanged();
				}
			});
			registerForContextMenu(getListView());
		};

	}

public void onStarButtonClick(View v){
	Intent intent = new Intent();
    intent.setClass(getBaseContext(), MenuForm.class);
    startActivityForResult(intent, 0);
    finish();
}
@SuppressLint("UseValueOf")
public class ProductProxyAdapter2 extends BaseAdapter{
	private ArrayList<Integer> filter = null;
	private ArrayList<ProductInSearch> underlying;
	private LayoutInflater mInflater;
	
	public ProductProxyAdapter2(Context c,ArrayList<ProductInSearch> prods){
		// Cache the LayoutInflate to avoid asking for a new one each time.
        mInflater = LayoutInflater.from(c);
        underlying = prods;
    }
	public void changeProds(ArrayList<ProductInSearch> prods,int owner){
		underlying = prods;
		filter(owner);
	}
	private int getFilterOffset(int index) {
	    if(filter == null) {
	         return index;
	    }
	    if(filter.size() > index && index>=0) {
	    	return filter.get(index).intValue();
	    }
	    return -1;
	}
	@SuppressLint("UseValueOf")
	private int getUnderlyingOffset(int index) {
	    if(filter == null) {
	        return index;
	    }
	    return filter.indexOf(new Integer(index));
	}
	public void filter(int owner) {
	    filter = new ArrayList<Integer>();
	    for(int iter = 0 ; iter < underlying.size() ; iter++) {
	           underlying.get(iter);     
	               filter.add(new Integer(iter));
	    }
	    notifyDataSetChanged();
	}
	
	public int getCount() {
		// TODO Auto-generated method stub
		/*if (underlying!=null) return underlying.size();
		else return 0;*/
		if (filter!=null) return filter.size();
		else return 0;
	}

	public Object getItem(int index) {
		if (index<0) return null;
	    return underlying.get(getFilterOffset(index));
	  	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unneccessary calls
        // to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.product_item_search, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.textProductName);
            holder.descr = (TextView) convertView.findViewById(R.id.textProductDescription);
            
            holder.selected = (CheckBox)convertView.findViewById(R.id.checkProductSelected);
            
            convertView.setTag(holder);
        } else {
        	Log.i(DCJ_TAG, "on create-adap17!!!");
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        holder.name.setText( ((ProductInSearch)getItem(position)).getName() );
        holder.descr.setText( ""+
        		underlying.get(getFilterOffset(position)).getProt()+"-"+
        		underlying.get(getFilterOffset(position)).getFat()+"-"+
        		underlying.get(getFilterOffset(position)).getCarb()+"-"+
        		underlying.get(getFilterOffset(position)).getGi() );
        holder.selected.setChecked(underlying.get(getFilterOffset(position)).isSelected());
        return convertView;
    }
	class ViewHolder{
		TextView name;
		TextView descr;
		CheckBox selected;
	}
	
}

}

			