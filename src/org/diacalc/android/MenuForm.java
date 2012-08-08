package org.diacalc.android;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.diacalc.android.components.FloatEditText;
import org.diacalc.android.internet.DoingPost;
import org.diacalc.android.manager.DatabaseManager;
import org.diacalc.android.maths.DPS;
import org.diacalc.android.maths.Dose;
import org.diacalc.android.maths.Factors;
import org.diacalc.android.maths.Sugar;
import org.diacalc.android.maths.User;
import org.diacalc.android.products.ProductInBase;
import org.diacalc.android.products.ProductInMenu;
import org.diacalc.android.products.ProductW;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import 	android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.widget.TextView.OnEditorActionListener;

import java.util.*;

public class MenuForm extends Activity {
	//private ProgressDialog m_ProgressDialog = null; 
    private ArrayList<RowHolder> rows = new ArrayList<RowHolder>();
    
    //private Runnable viewOrders;
    
    private DataPocket dtPkt;
    private User user;
    
    private static final int DIALOG_COEFS_ID = 100+0;
    private static final int DIALOG_DRS_ID = 100+1;
    
    private LinearLayout root; 
    
    private TextView textBD;
    private TextView textMD;
    private TextView textSumD;
    private TextView textInfo;
    
    private DecimalFormat df;
    private DecimalFormat df0;
    private DecimalFormat df00;
    
    private ProductW sum;//Что бы не дергать каждый раз меню для округления
    
    //для использования в отдельном потоке
    private ArrayList<ProductInMenu> i_prods = null;
    private ArrayList<ProductInMenu> i_snack = null;
    private User i_us = null;
    private String i_msg = "";
    private ProgressDialog i_progressDialog = null;
    private DatabaseManager mgr;
    
    /* Работа с диалогами
     */
    
    @Override
    public Dialog onCreateDialog(int id) {
    	switch(id){
    	case DIALOG_COEFS_ID:
    			return createCoefDlg();
    	case DIALOG_DRS_ID:
    			return createDRSDlg();
    	default: return null;	
    	}
    	
    }
    private Dialog createDRSDlg() {
    	final Dialog dialog = new Dialog(this);
    	dialog.setContentView(R.layout.menu_drs_dlg);
    	dialog.setTitle(dialog.getContext().getString(R.string.SugarDialogTitle));
    	
    	int zeros = 0;
    	if (user.isMmol()) zeros = 1;
    	
    	final FloatEditText fldS1 = (FloatEditText)dialog.
    			findViewById(R.id.editMenuDlgS1);
    	fldS1.setZeroes(zeros);
    	
    	final FloatEditText fldS2 = (FloatEditText)dialog.
			findViewById(R.id.editMenuDlgS2);
    	fldS2.setZeroes(zeros);
    	
    	final FloatEditText fldOUV = (FloatEditText)dialog.
			findViewById(R.id.editMenuDlgOUV);
    	fldOUV.setZeroes(zeros+1);
    	
    	dialog.setOnDismissListener(new OnDismissListener(){
			public void onDismiss(DialogInterface dialog) {
				// TODO Auto-generated method stub
				Sugar s = new Sugar();
				s.setSugar(fldOUV.getValue(), user.isMmol(), user.isPlasma());
				user.getFactors().setK3(s.getValue());
				
				s.setSugar(fldS1.getValue(), user.isMmol(), user.isPlasma());
				user.setS1(s.getValue());
				
				s.setSugar(fldS2.getValue(), user.isMmol(), user.isPlasma());
				user.setS2(s.getValue());
				
				//Где то тут надо проверить, считать дальше или 
				//предупредить о быстром снижении сахаров
				if ( (user.getS1()-user.getS2())>5f ){
					final float s2 = user.getS1()-5f;
					DecimalFormat d = user.isMmol()?df0:df;
					
					AlertDialog.Builder builder = new AlertDialog.Builder(MenuForm.this);
					builder.setMessage(getString(R.string.sugarFastAlert)+" "+
							d.format(new Sugar(s2).getSugar(user.isMmol(), user.isPlasma()))
							)
					       .setCancelable(false)
					       .setPositiveButton(MenuForm.this.getString(R.string.btnOk), 
					    		   	new DialogInterface.OnClickListener() {
					           			public void onClick(DialogInterface dialog, int id){
					           				//Нажали да
					           				user.setS2(s2);
					           				fillSugarsButton();
					           				calcMenu();
					           			}
					       			})
					       .setNegativeButton(MenuForm.this.getString(R.string.btnNo),
					    		   null);//Кнопку нет не надо слушать
					AlertDialog alert = builder.create();
					alert.show();
				}
				
				calcMenu();
				
				//Надо еще значения в кнопки записать
    			fillSugarsButton();
    		}
    	});
    	Button btnOk = (Button)dialog.findViewById(R.id.btnMenuDRSDlgOk);
    	btnOk.setOnClickListener(new OnClickListener(){
    		public void onClick(View v){
    			dialog.dismiss();
    		}
    	});
    	
    	return dialog;
	}
    
    private Dialog createCoefDlg() {
    	final Dialog dialog = new Dialog(this);
    	dialog.setContentView(R.layout.menu_coefs_dlg);
    	dialog.setTitle(getString(R.string.SugarDialogTitle));
    	
    	final FloatEditText fldK1 = (FloatEditText)dialog.
    			findViewById(R.id.editMenuDlgK1);
    	fldK1.setZeroes(2);
    	
    	final FloatEditText fldK2 = (FloatEditText)dialog.
			findViewById(R.id.editMenuDlgK2);
    	fldK2.setZeroes(2);
    	
    	final FloatEditText fldBE = (FloatEditText)dialog.
			findViewById(R.id.editMenuDlgBE);
    	fldBE.setZeroes(0);
    	
    	dialog.setOnDismissListener(new OnDismissListener(){
			public void onDismiss(DialogInterface dialog) {
				// TODO Auto-generated method stub
				user.getFactors().setK1XE(fldK1.getValue(), fldBE.getValue(), 
						Factors.DIRECT);
				user.getFactors().setK2(fldK2.getValue());
				
    			//пересчитываем
				calcMenu();
				
				//Надо еще значения в кнопки записать
				fillButtonCoef();
			}
    	});
    	Button btnOk = (Button)dialog.findViewById(R.id.btnMenuCoefDlgOk);
    	btnOk.setOnClickListener(new OnClickListener(){
    		public void onClick(View v){
    			dialog.dismiss();
    		}
    	});
    	
    	return dialog;
	}
    
    //Подготавливаем диалоги
    @Override
	public void onPrepareDialog(int id, Dialog dialog){
		switch (id){
			case DIALOG_COEFS_ID:
				FloatEditText fldK1 = (FloatEditText)dialog.
					findViewById(R.id.editMenuDlgK1);
				fldK1.setValue(user.getFactors().getK1(Factors.DIRECT));
				
				FloatEditText fldK2 = (FloatEditText)dialog.
					findViewById(R.id.editMenuDlgK2);
				fldK2.setValue(user.getFactors().getK2());
				
				FloatEditText fldBE = (FloatEditText)dialog.
					findViewById(R.id.editMenuDlgBE);
				fldBE.setValue(user.getFactors().getBE(Factors.DIRECT));
				break;
			case DIALOG_DRS_ID:
				FloatEditText fldS1 = (FloatEditText)dialog.
					findViewById(R.id.editMenuDlgS1);
				fldS1.setValue(new Sugar(user.getS1()).getSugar(user.isMmol(),
						user.isPlasma()));
			
				FloatEditText fldS2 = (FloatEditText)dialog.
					findViewById(R.id.editMenuDlgS2);
				fldS2.setValue(new Sugar(user.getS2()).getSugar(user.isMmol(),
					user.isPlasma()));
			
				FloatEditText fldOUV = (FloatEditText)dialog.
					findViewById(R.id.editMenuDlgOUV);
				fldOUV.setValue(new Sugar(user.getFactors().getK3())
					.getSugar(user.isMmol(),
							user.isPlasma()));
				break;
			default:
		}
	}
    
    //Показываем диалоги
    public void onClickButtonMenuCoef(View v){
    	validyRowsValue();
    	showDialog(DIALOG_COEFS_ID);
    }
    public void onClickButtonMenuDRS(View v){
    	validyRowsValue();
    	showDialog(DIALOG_DRS_ID);
    }
    private void validyRowsValue(){
    	//Проверяем, что пользователь видит то же значение, что и установлено 
    	//в весе продукта
    	for(int i=0;i<rows.size();i++){
    		if ( Math.abs(rows.get(i).weight.getValue()-rows.get(i).prod.getWeight())>0.001  ){
    			rows.get(i).prod.setWeight(rows.get(i).weight.getValue());
    		}
    	}
    }
    ///Строим основное окно
    @Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.menu);
	  
	  root = (LinearLayout)findViewById(R.id.listPaneMenu);
	  
	  mgr = new DatabaseManager(this); 
	  
	  dtPkt = (DataPocket)this.getApplication();
      user = dtPkt.getUser(mgr);
      
      NumberFormat f = NumberFormat.getInstance(Locale.US);
      switch (user.getRound())
      {
      case User.ROUND_005: 
    	  if (f instanceof DecimalFormat) {
  			df0 = (DecimalFormat)f;
  			df0.applyPattern("0.00");
        };
		break;	
		
      default: if (f instanceof DecimalFormat) {
			df0 = (DecimalFormat)f;
			df0.applyPattern("0.0");
    };
      }
      
      f = NumberFormat.getInstance(Locale.US);
      if (f instanceof DecimalFormat) {
			df = (DecimalFormat)f;
			df.applyPattern("0");
      }
      f = NumberFormat.getInstance(Locale.US);
      if (f instanceof DecimalFormat) {
			df00 = (DecimalFormat)f;
			df00.applyPattern("0.00");
      }
      
      for (int i=0;i<dtPkt.getMenuProds(mgr).size();i++){
		addRow(dtPkt.getMenuProds(mgr).get(i));
      }
    
      textBD = (TextView)findViewById(R.id.textMenuBD);
      textMD = (TextView)findViewById(R.id.textMenuMD);
      textSumD = (TextView)findViewById(R.id.textMenuSum);
      textInfo = (TextView)findViewById(R.id.textMenuInfo);
      
      
      fillButtonCoef();
      fillSugarsButton();
      
      calcMenu();
	}
    
    @Override
	public void onPause(){//Тут сохраняем данные
    	super.onPause();
    	if (dtPkt.isNeed2SaveMenu()){
    		Log.i("Menu","need to save");
    		dtPkt.getMenuProds(mgr).clear();
    		Log.i("Menu",""+dtPkt.getMenuProds(mgr).size());
    		for(int i=0;i<rows.size();i++){
    			dtPkt.getMenuProds(mgr).add(rows.get(i).prod);
    		}
    		Log.i("Menu",""+dtPkt.getMenuProds(mgr).size());
    	}
    }
    public void onStarMenuButtonClick(View v){
    	Intent intent = new Intent();
        intent.setClass(getBaseContext(), ProdForm.class);
        startActivity(intent);
        finish();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Обработка меню
        switch (item.getItemId()) {
        case R.id.deleteOldMenu:
            deleteOldFromMenu();
            return true;
        case R.id.clearMenu:
            clearMenu();
            return true;
       /* case R.id.storeMenu:
        	//storeMenu
        	return true;*/
        case R.id.loadMenu:
        	downloadMenu();
        	return true;
        case R.id.uploadMenu:
        	uploadMenu();
        	return true;
        case R.id.createProdMenu:
        	return true;
        case R.id.deleteRowMenuSub:
        	deleteSelectedRow();
        	return true;
        case R.id.roundDoseMenuSub:
        	roundDose();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    private void uploadMenu(){
    	Thread uploadingMenu = new Thread(){
    		public void run(){
    			if (rows.isEmpty()) return;
    			i_prods = new ArrayList<ProductInMenu>();
    			for(int i=0;i<rows.size();i++){
    				i_prods.add(rows.get(i).prod);
    			}
    			i_msg = "";
    			HashMap<String,Object> i_answer = 
        			new DoingPost(user).sendMenu(i_prods);
    			if (i_answer.get(DoingPost.ERROR)!=null){
    	    		//Значит ошибка
    	    		i_msg = (String)i_answer.get(DoingPost.ERROR);
    	    	}
    			runOnUiThread(new Runnable(){
    	    		public void run(){
    	    			i_progressDialog.dismiss();
    	    			AlertDialog.Builder builder = 
    	    	    		new AlertDialog.Builder(MenuForm.this);
    	    			if (i_msg.length()>0){//Выводим сообщение о ошибке
    	    	    		builder.setTitle(getString(R.string.errorTitle))
    	    	    			.setMessage(getString(R.string.errorMsgMenuUpload)+"\n"+i_msg)
    	    	    			.setNeutralButton(getString(R.string.btnOk),new DialogInterface.OnClickListener(){
    	    	        			public void onClick(DialogInterface dialog, int id) {
    	    	    					dialog.cancel();
    	    	        			}
    	    	    	        });
    	    	    		
    	    	        }else{
    	    	        	builder.setTitle(getString(R.string.menuUploadedTitle))
	    	    			.setMessage(getString(R.string.menuUploadedMsg))
	    	    			.setPositiveButton(getString(R.string.btnOk),null);
    	    	        }
    	    			AlertDialog alert = builder.create();
    	    	    	alert.show();
    	    		}
    			});
    		}
    	};
    	i_progressDialog = ProgressDialog.show(this,    
                getString(R.string.pleaseWait), 
                getString(R.string.menuUploading), true);
    	
    	uploadingMenu.start();
    }
    private void downloadMenu(){
    	Thread loadingMenu = new Thread(){
        	public void run(){
        		i_prods = null;
        		i_snack = null;
        		i_us = null;
        		i_msg = "";
        		
        		HashMap<String,Object> i_answer = 
        			new DoingPost(user).requestMenu();
    	    	if (i_answer.get(DoingPost.ERROR)!=null){
    	    		//Значит ошибка
    	    		i_msg = (String)i_answer.get(DoingPost.ERROR);
    	    	}else{
    	    		i_prods = (ArrayList<ProductInMenu>)i_answer
    	    				.get(DoingPost.MENU_MENU);
    	    		i_snack = (ArrayList<ProductInMenu>)i_answer
    	    				.get(DoingPost.MENU_SNACK);
    	    		i_us = (User)i_answer.get(DoingPost.MENU_USER);
    	    	}
    	    	
    	    	runOnUiThread(new Runnable(){
    	    		public void run(){
    	    			i_progressDialog.dismiss();
    	    			AlertDialog.Builder builder = 
    	    	    		new AlertDialog.Builder(MenuForm.this);
    	    	    	if (i_msg.length()>0){//Выводим сообщение о ошибке
    	    	    		builder.setTitle(getString(R.string.errorTitle))
    	    	    			.setMessage(getString(R.string.errorMsgMenuLoad)+"\n"+i_msg)
    	    	    			.setNeutralButton(getString(R.string.btnOk),new DialogInterface.OnClickListener(){
    	    	        			public void onClick(DialogInterface dialog, int id) {
    	    	    					dialog.cancel();
    	    	        			}
    	    	    	        });
    	    	    		
    	    	        }else{
    	    	        	builder.setTitle(getString(R.string.menuLoadedTitle))
    		    			.setMessage(getString(R.string.menuLoadedMsg)+
    		    					(i_snack.size()>0?"\n"+getString(R.string.snackLoadedMsg):"") )
    		    			.setPositiveButton(getString(R.string.btnOk),new DialogInterface.OnClickListener(){
    		        			public void onClick(DialogInterface dialog, int id) {
    		    					//Тут заменяем меню новым
    		        				clearMenu();
    		        				user = i_us;
    		        				for(int i=0;i<i_prods.size();i++){
    		        					addRow(i_prods.get(i));
    		        				}
    		        				for(int i=0;i<i_snack.size();i++){
    		        					addRow(i_snack.get(i));
    		        				}
    		        				fillButtonCoef();
    		        				fillSugarsButton();
    		        				calcMenu();
    		        			}
    		    	        })
    		    	        .setNegativeButton(getString(R.string.btnNo),new DialogInterface.OnClickListener(){
    		        			public void onClick(DialogInterface dialog, int id) {
    		    					//А тут ничего не делаем
    		        				dialog.cancel();
    		        			}
    		    	        });
    	    	        }
    	    	    	AlertDialog alert = builder.create();
    	    	    	alert.show();
    	    		}
    	    	});
    	    }
        };
        
        i_progressDialog = ProgressDialog.show(this,    
                getString(R.string.pleaseWait), 
                getString(R.string.menuLoading), true);
        
        loadingMenu.start();
    }
    private void deleteOldFromMenu(){
    	boolean renew = false;
    	for(int i=rows.size();i>0;i--){
    		if (rows.get(i-1).prod.getId()==-1){
    			root.removeView(rows.get(i-1).layout);
    			rows.remove(i-1);
    			renew = true;
    		}
    	}
    	if (renew){
    		calcMenu();
    		dtPkt.setMenuNeed2Save();
    	}
    }
    private void clearMenu(){
    	if (rows.isEmpty()) return;
    	
    	dtPkt.setMenuNeed2Save();
    	if (!dtPkt.isProductsNull()){
    		for(ProductInBase it:dtPkt.getProducts(mgr)){
    			if (it.isSelected()) it.setSelected(false);
    		}
    	}
    	
    	rows.clear();
    	root.removeAllViews();
    	calcMenu();
	}
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            		ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu_contxmenu, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
      //AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      switch (item.getItemId()) {
      case R.id.deleteRowMenu:
        //Удаляем выделенный ряд
    	  deleteSelectedRow();
        return true;
      case R.id.roundDoseMenu:
        //Округляем дозу
    	  roundDose();
        return true;
      default:
        return super.onContextItemSelected(item);
      }
    }
    private int getSelectedRow(){
    	for(int i=0;i<root.getChildCount();i++){
			if (root.getChildAt(i).hasFocus()){
				return i;
			}
		}
    	return -1;
    }
    private void deleteSelectedRow(){
    	int i = getSelectedRow();
    	Log.i("menu",""+rows.get(i).prod.getId()+" "+
    			rows.get(i).prod.getName()+" "+dtPkt.isProductsNull());
    	if (i>-1){
    		if (rows.get(i).prod.getId()>-1 && !dtPkt.isProductsNull()){
    			for (ProductInBase it:dtPkt.getProducts(mgr)){
    				if (it.getId()==rows.get(i).prod.getId()){
    					it.setSelected(false);
    					break;
    				}
    			}
    		}
    		rows.remove(i);
			root.removeViewAt(i);
			if (root.getChildCount()>0){
				root.getChildAt(i>0?i-1:0).requestFocus();
			}
			calcMenu();
			dtPkt.setMenuNeed2Save();
    	}
    }
    private void roundDose(){
    	final int pos = getSelectedRow();
    	if (pos<0) return;
    	//Сначала все вычисляем
    	final ProductInMenu prod = rows.get(pos).prod;
    	DPS dps = new DPS(
                new Sugar(user.getS1()),
                new Sugar(user.getS2()),
                user.getFactors()
                );
        Dose ds_now = new Dose(sum,user.getFactors(),
                dps);
        ProductW prod100 = new ProductW(prod);
        prod100.setWeight(prod.getWeight()+100f);
        
        float dose_diff = new Dose(prod100,user.getFactors(),
                dps).getWholeDose() - //тут величина ДПС не влияет
                    new Dose(prod,user.getFactors(),dps).getWholeDose();

        float frac = ds_now.getWholeDose() - 
                (float)Math.floor(ds_now.getWholeDose());
        float step;
         switch (user.getRound()){
            case User.ROUND_1: step = 1f;
                                break;
            case User.ROUND_05: step = 0.5f;
                                break;
            case User.ROUND_01: step = 0.1f;
            					break;
            case User.ROUND_005: step = 0.05f;
			break;	
			
                /*case 2: step = 0.25;
                        break;
                case 3: step = 1/6;//NovoPen 3
                        break;
                case 4: step = 0.5/6;//NovoPen Demi
                        break;
                case 5: step = 0.05;
                        break;*/
            default: step = 1;
         }
        float i = 0;
        while (i<frac)
                i += step;
        float upDiff = (float)Math.floor( ds_now.getWholeDose() ) + i -
                 (float)ds_now.getWholeDose();
        float downDiff = ds_now.getWholeDose() -
                 (float)Math.floor( ds_now.getWholeDose() ) - (i-step);

        final float wUp = upDiff * 100 / dose_diff;
        final float wDown = downDiff * 100 / dose_diff;
        
    	//Потом показываем диалог с выбором
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(getString(R.string.RoundDialogTitle))
    			.setMessage(prod.getName()+"\n"+
    					getString(R.string.Weight)+
    					":"+df.format(prod.getWeight())
    					+" "+getString(R.string.gramm)+
    					"\n"+getString(R.string.roundTo)+" "+step)
    	        .setPositiveButton("+" + df0.format(wUp) +" "+getString(R.string.gramm), new DialogInterface.OnClickListener(){
    	        	public void onClick(DialogInterface dialog, int id) {
    	        		//прибавляем вес
    	        		prod.setWeight(prod.getWeight()+wUp);
    	        		rows.get(pos).weight.setValue(prod.getWeight());
    	        		calcMenu();
    	        		dtPkt.setMenuNeed2Save();
    	        	}
    	        })
    	        .setNeutralButton(getString(R.string.Cancel),new DialogInterface.OnClickListener(){
        			public void onClick(DialogInterface dialog, int id) {
    					dialog.cancel();
        			}
    	        }) 
    	        .setNegativeButton("-" + df0.format(wDown) +" "+getString(R.string.gramm),new DialogInterface.OnClickListener(){
    	        	public void onClick(DialogInterface dialog, int id) {
    	        		//Убавляем вес
    	        		prod.setWeight(prod.getWeight()-wDown);
    	        		rows.get(pos).weight.setValue(prod.getWeight());
    	        		calcMenu();
    	        		dtPkt.setMenuNeed2Save();
    	        	}
    	        });
    	AlertDialog alert = builder.create();
    	alert.show();
    	
    }
    private String getDescription(ProductInMenu p){
    
    	switch (user.getMenuInfo()){
    	case User.PFC_INFO:   return df0.format(p.getAllProt())+
        						"-" + df0.format(p.getAllFat())+
        						"-"+df0.format(p.getAllCarb())+
        						"-"+p.getGi();
    	case User.BE_INFO: 	  return df0.format(p.getAllCarb()/
    							user.getFactors().getBE(Factors.DIRECT));
    	case User.CALOR_INFO: return df.format(p.getCalories())+" "+getString(R.string.calor);
    	
    	case User.DOSE_INFO:  return df0.format(
    							new Dose(
    								p,
    								user.getFactors(),
    								new DPS()).getWholeDose());

    	default: return "===";
    	}
    }
    
    private void addRow(final ProductInMenu prod){
    	LinearLayout right = new LinearLayout(this);
		right.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams ltP = new LinearLayout.LayoutParams(
			     LinearLayout.LayoutParams.FILL_PARENT, 
			     LinearLayout.LayoutParams.WRAP_CONTENT);
		ltP.leftMargin = 5;
		right.setLayoutParams(ltP);
		
		///Наименование продукта
		ltP = new LinearLayout.LayoutParams(
		     LinearLayout.LayoutParams.FILL_PARENT, 
		     LinearLayout.LayoutParams.WRAP_CONTENT);
		
		TextView name = new TextView(this);
		name.setText(prod.getName());
		name.setTextSize(18);
		
		right.addView(name, ltP);
		
		///Описание продукта
		ltP = new LinearLayout.LayoutParams(
			     LinearLayout.LayoutParams.FILL_PARENT, 
			     LinearLayout.LayoutParams.WRAP_CONTENT);
		
		TextView descr = new TextView(this);
		descr.setText(getDescription(prod));
		descr.setTextSize(12);
		
		right.addView(descr, ltP);
		//Закончили правую сторону
		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ltP = new LinearLayout.LayoutParams(
			     LinearLayout.LayoutParams.FILL_PARENT, 
			     LinearLayout.LayoutParams.WRAP_CONTENT);
		ltP.bottomMargin = 5;
		ll.setLayoutParams(ltP);
		ll.setBackgroundColor(0x30FFFFFF);
		ll.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				for (int i=0;i<root.getChildCount();i++){
					if (v==root.getChildAt(i)){
						root.getChildAt(i).requestFocus();
					}
				}
			}
			
		});
		ll.setOnLongClickListener(new OnLongClickListener(){
			public boolean onLongClick(View v) {
				for (int i=0;i<root.getChildCount();i++){
					if (v==root.getChildAt(i)){
						root.getChildAt(i).requestFocus();
					}
				}
				return false;
			}
		});
		registerForContextMenu(ll);
		
		///Поле ввода веса
		ltP = new LinearLayout.LayoutParams(
		     LinearLayout.LayoutParams.WRAP_CONTENT, 
		     LinearLayout.LayoutParams.WRAP_CONTENT);
		
		final FloatEditText edit = new FloatEditText(this);
		edit.setZeroes(0);
		
		edit.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_CLASS_NUMBER);
		edit.setWidth(85);
		edit.setTextSize(18);
		edit.setValue(prod.getWeight());
		edit.setHint(getString(R.string.weightGr));
		edit.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				prod.setWeight(
						((FloatEditText)v).getValue()
						);
				calcMenu();
				dtPkt.setMenuNeed2Save();
				return false;
			}
		});
		edit.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus){
					if ( Math.abs(prod.getWeight()-edit.getValue())>0.001f){
						prod.setWeight(edit.getValue());
						calcMenu();
						dtPkt.setMenuNeed2Save();
					}
				}
				
			}
		});
		
		
		ll.addView(edit, ltP);
		ll.addView(right);
		
		RowHolder row = new RowHolder();
		row.name = name;
		row.descr = descr;
		row.weight = edit;
		row.layout = ll;
		row.prod = prod;
		
		rows.add(row);
		
		root.addView(row.layout);
    }
    //Рутинные задачи
    private void fillSugarsButton(){
    	Button btn = (Button)findViewById(R.id.btnMenuDPS);
    	
    	DecimalFormat d = user.isMmol()?df0:df;
    	
    	btn.setText(
    			getString(R.string.sugar1)+"=" +
    	    	 d.format(new Sugar(user.getS1()).getSugar(user.isMmol(), user.isPlasma()))+
    	    	 " "+getString(R.string.sugar2)+"="+
    	    	 d.format(new Sugar(user.getS2()).getSugar(user.isMmol(), user.isPlasma()))+
    	    	 "\n"+getString(R.string.k3)+"="+
    	    	 d.format(new Sugar(user.getFactors().getK3()).getSugar(user.isMmol(), user.isPlasma()))
    	);
    }
    
    private void fillButtonCoef(){
    	Button btn = (Button)findViewById(R.id.btnMenuCoef);
    	btn.setText(getString(R.string.k1)+"="+df00.format(user.getFactors().getK1(Factors.DIRECT))+
    			" "+getString(R.string.k2)+"="+df00.format(user.getFactors().getK2())+
    			"\n"+getString(R.string.BE)+"="+df.format(user.getFactors().getBE(Factors.DIRECT)));
    }
    
	private void calcMenu(){
		sum = new ProductW();
		for(int i=0;i<rows.size();i++){
			sum.plusProd(rows.get(i).prod);
			rows.get(i).descr.setText(getDescription(rows.get(i).prod));
		}
				
		Sugar s1 = new Sugar(user.getS1());
        Sugar s2 = new Sugar(user.getS2());
        DPS dps = new DPS(s1,s2,user.getFactors());
		Dose ds = new Dose(sum,user.getFactors(),dps);
        
		//тут заносим БД, МД и т.д.
		textBD.setText( df0.format( ds.getCarbFastDose()+ds.getDPSDose() ) );
		textMD.setText( df0.format( ds.getCarbSlowDose()+ds.getSlowDose() ) );
		textSumD.setText( df0.format( ds.getWholeDose() ) );

		switch (user.getMenuInfo()){
            case User.PFC_INFO: textInfo.setText(
                    ""+	df0.format(sum.getAllProt())+"-"+
                    	df0.format(sum.getAllFat())+"-"+
                    	df0.format(sum.getAllCarb())+"-"+sum.getGi());
                    break;
            case User.BE_INFO: textInfo.setText(
                    ""+df0.format(sum.getAllCarb()/
                       user.getFactors().getBE(Factors.DIRECT)));
                    break;
            case User.DOSE_INFO:
            case User.CALOR_INFO: textInfo.setText(
                    ""+df.format(sum.getCalories())+" "+getString(R.string.calor) );
                    break;
        }
        //Тут меняем заголовок
		/*String name = getName();
        if (fieldempty){
            name += " *";
        }
        if (Math.abs(ds.getDPSDose())>0){
            name += " Д";
        }
        if (master.getUser().isTimeSense()){
            name += " t";
        }
        form.setTitle( name );*/
	}
	
	/*private void getProds(){
        try{
            m_prods = new ArrayList<ProductInMenu>();
            m_prods.add(new ProductInMenu("Первый",2.5f,3.6f,7.8f,35,75f,101));
    		m_prods.add(new ProductInMenu("Второй",3.5f,4.6f,17.8f,65,35f,102));
    		m_prods.add(new ProductInMenu("Третий",4.5f,5.6f,27.8f,55,80f,103));
    		m_prods.add(new ProductInMenu("Четвертый",4.5f,5.6f,27.8f,55,80f,103));
    		m_prods.add(new ProductInMenu("Пятый",4.5f,5.6f,27.8f,55,80f,103));
    		m_prods.add(new ProductInMenu("Шестой",4.5f,5.6f,27.8f,55,80f,103));
    		m_prods.add(new ProductInMenu("Седьмой",4.5f,5.6f,27.8f,55,80f,103));
    		m_prods.add(new ProductInMenu("Восьмой",4.5f,5.6f,27.8f,55,80f,103));
            //Thread.sleep(100);
            Log.i("ARRAY", ""+ m_prods.size());
          } catch (Exception e) { 
            Log.e("BACKGROUND_PROC", e.getMessage());
          }
          runOnUiThread(returnRes);
	}
	private Runnable returnRes = new Runnable() {
		@Override
        public void run() {
            if(m_prods != null && m_prods.size() > 0){
                m_adapter.notifyDataSetChanged();
                for(int i=0;i<m_prods.size();i++)
                	m_adapter.add(m_prods.get(i));
            }
            m_ProgressDialog.dismiss();
            m_adapter.notifyDataSetChanged();
        }
   };*/

	//Класс содержащий строку в меню
	class RowHolder{
		private FloatEditText weight;
		private TextView descr;
		private TextView name;
		private LinearLayout layout;
		private ProductInMenu prod;
	}	
}
