package org.diacalc.android;

import java.util.ArrayList;

import org.diacalc.android.manager.DatabaseManager;
import org.diacalc.android.maths.User;
import org.diacalc.android.products.ProductGroup;
import org.diacalc.android.products.ProductInBase;
import org.diacalc.android.products.ProductInMenu;

import android.app.Application;
import android.util.Log;

public class DataPocket extends Application {
	private User user = null;
	private ArrayList<ProductInMenu> menuProds = null;
	private boolean need2saveMenu = false;
	
	private ArrayList<ProductGroup> groups = null;
	private ArrayList<ProductInBase> prods = null;
	
	private boolean need2saveProds = true;
	
	
	public DataPocket(){
		super();
	}
	
	public void setProdsNeed2Save(){
		need2saveProds = true;
	}
	
	public void setMenuNeed2Save(){
		need2saveMenu = true;
	}
	public boolean isNeed2SaveMenu(){
		return need2saveMenu;
	}
	public ArrayList<ProductInBase> getProducts(DatabaseManager mgr){
		if (prods==null){
			prods = mgr.getProducts();
		}
		return prods;
	}
	public ArrayList<ProductGroup> getGroups(DatabaseManager mgr){
		if (groups==null){
			groups = mgr.getGroups();
		}
		return groups;
	}
	public ArrayList<ProductInMenu> getMenuProds(DatabaseManager mgr){
		if (menuProds==null){
			menuProds = mgr.getMenuProducts();
		}
		return menuProds;
	}
	public void setAllPointers2Null(){
		menuProds = null;
		user = null;
	}
	public void setGroupProds2Null(){
		prods = null;
		groups = null;
	}
	public boolean isProductsNull(){
		return prods==null;
	}
	public void storeMenuProds(DatabaseManager mgr){
		Log.i("DataPocket","storing menu1 "+need2saveMenu);
		if (need2saveMenu){
			Log.i("DataPocket","storing menu");
			if (menuProds!=null) mgr.putMenuProds(menuProds);
		}
	}
	
	public User getUser(DatabaseManager mgr){
		if (user==null){
			Log.i("data pocket","get user");
			user = mgr.getUser();
		}
		return user;
	}
	public void storeUser(DatabaseManager mgr){
		if (user!=null) mgr.putUser(user);
	}
	
}
