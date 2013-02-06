package com.mohammadag.extendedpowermenu;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.view.WindowManager;
import java.lang.reflect.Constructor;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XC_MethodHook;

public class ExtendedPowerMenu implements IXposedHookZygoteInit, IXposedHookLoadPackage {

	private PowerManager pm = null;
	private Context mContext = null;
	
	
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName != "android")
			return;
		
		XC_MethodReplacement methodreplacer = new XC_MethodReplacement() {
			protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam paramAnonymousMethodHookParam) throws Throwable {
				String[] items = new String[3];
				items[0] = "Reboot";
				items[1] = "Download";
				items[2] = "Recovery";
				
				if (mContext == null) {
					XposedBridge.log("mContext is null, we're gonna crash...");
				}
				
				new AlertDialog.Builder(mContext)
		        .setSingleChoiceItems(items, 0, null)
		        .setTitle("Reboot Menu")
		        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int whichButton) {
		                dialog.dismiss();
		            }
		        })
		        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int whichButton) {
		                dialog.dismiss();
		                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
		                if (selectedPosition == 0) {
		                	pm.reboot(null);
		                } else if (selectedPosition == 1) {
		                	pm.reboot("download");
		                } else if (selectedPosition == 2) {
		                	pm.reboot("recovery");
		                }
		            }
		        }
		        
		        ).show();
				
				return null;
			}
		};
		
		XposedHelpers.findAndHookMethod("com.android.internal.policy.impl.GlobalActions$5", ExtendedPowerMenu.class.getClassLoader(),
				"onPress", methodreplacer);
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		try {
			final Class<?> clsPMS = findClass("com.android.server.pm.PackageManagerService", ExtendedPowerMenu.class.getClassLoader());
			
			// Listen for broadcasts from the Settings part of the mod, so it's applied immediately
			findAndHookMethod(clsPMS, "systemReady", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {			
					mContext = (Context) getObjectField(param.thisObject, "mContext");
					pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
				}
			});
			
		} catch (Throwable t) {
			XposedBridge.log(t);
		}
		
		try {
			Class<?> globalActionsClass = XposedHelpers.findClass("com.android.internal.policy.impl.GlobalActions", ExtendedPowerMenu.class.getClassLoader());
			Constructor<?> constructor = globalActionsClass.getConstructor(XposedHelpers.findClass("com.android.internal.policy.impl.GlobalActions", ExtendedPowerMenu.class.getClassLoader()));
			XposedBridge.hookMethod(constructor, new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mContext = (Context) getObjectField(param.thisObject, "mContext");
				}
			});
		} catch (Throwable t) {
			XposedBridge.log(t);
		}
	}

}
