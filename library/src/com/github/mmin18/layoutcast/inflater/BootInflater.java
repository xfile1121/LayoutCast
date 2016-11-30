package com.github.mmin18.layoutcast.inflater;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;

import com.github.mmin18.layoutcast.context.OverrideContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Used to replace the application service, provide Activity's layout inflater
 * by cloneInContext()
 * 
 * @author mmin18
 */
public class BootInflater extends BaseInflater {

	/**
	 * The original LayoutInflater in Application Service
	 */
	public static LayoutInflater systemInflater;

	public BootInflater(Context context) {
		super(context);
	}

	@Override
	public LayoutInflater cloneInContext(Context newContext) {
		if (newContext instanceof ContextWrapper) {
			try {
				OverrideContext.overrideDefault((ContextWrapper) newContext);
			} catch (Exception e) {
				Log.e("lcast", "fail to override resource in context "
						+ newContext, e);
			}
		}
		return super.cloneInContext(newContext);
	}

	public static void initApplication(Application app) {
		LayoutInflater inflater = (LayoutInflater) app
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (inflater instanceof BootInflater) {
			// already inited
			return;
		}
		systemInflater = inflater;
		if(Build.VERSION.SDK_INT <= 22) {
			Class<?> cCtxImpl = app.getBaseContext().getClass();
			if ("android.app.ContextImpl".equals(cCtxImpl.getName())) {
				try {
					ClassLoader cl = cCtxImpl.getClassLoader();
					Class<?> cStaticFetcher = cl
							.loadClass("android.app.ContextImpl$StaticServiceFetcher");
					Class<?> cFetcherContainer = null;
					for (int i = 1; i < 50; i++) {
						String cn = "android.app.ContextImpl$" + i;
						try {
							Class<?> c = cl.loadClass(cn);
							if (cStaticFetcher.isAssignableFrom(c)) {
								cFetcherContainer = c;
								break;
							}
						} catch (Exception e) {
						}
					}
					Constructor<?> cFetcherConstructor = cFetcherContainer
							.getDeclaredConstructor();
					cFetcherConstructor.setAccessible(true);
					Object fetcher = cFetcherConstructor.newInstance();
					Field f = cStaticFetcher.getDeclaredField("mCachedInstance");
					f.setAccessible(true);
					f.set(fetcher, new BootInflater(app));
					f = cCtxImpl.getDeclaredField("SYSTEM_SERVICE_MAP");
					f.setAccessible(true);
					HashMap<String, Object> map = (HashMap<String, Object>) f
							.get(null);
					map.put(Context.LAYOUT_INFLATER_SERVICE, fetcher);
				} catch (Exception e) {
					throw new RuntimeException(
							"unable to initialize application for BootInflater");
				}
			} else {
				throw new RuntimeException("application base context class "
						+ cCtxImpl.getName() + " is not expected");
			}
		}
		else {
			Class<?> cCtxImpl = app.getBaseContext().getClass();
			try {
				ClassLoader cl = cCtxImpl.getClassLoader();
				Class<?> cSystemService = cl.loadClass("android.app.SystemServiceRegistry");
				Class<?> cStaticFetcher = cl.loadClass("android.app.SystemServiceRegistry$StaticServiceFetcher");
				Class<?> cFetcherContainer = null;
				for (int i = 1; i < 50; i++) {
					String cn = "android.app.SystemServiceRegistry$" + i;
					try {
						Class<?> c = cl.loadClass(cn);
						if (cStaticFetcher.isAssignableFrom(c)) {
							cFetcherContainer = c;
							break;
						}
					} catch (Exception e) {
					}
				}
				Constructor<?> cFetcherConstructor = cFetcherContainer
						.getDeclaredConstructor();
				cFetcherConstructor.setAccessible(true);
				Object fetcher = cFetcherConstructor.newInstance();
				Field f = cStaticFetcher.getDeclaredField("mCachedInstance");
				f.setAccessible(true);
				f.set(fetcher, new BootInflater(app));
				f = cSystemService.getDeclaredField("SYSTEM_SERVICE_FETCHERS");
				f.setAccessible(true);
				HashMap<String, Object> map = (HashMap<String, Object>) f
						.get(null);
				map.put(Context.LAYOUT_INFLATER_SERVICE, fetcher);
			}
			catch (Exception e) {
				throw new RuntimeException(
						"unable to initialize application for BootInflater");
			}
		}

		if (!(app.getSystemService(Context.LAYOUT_INFLATER_SERVICE) instanceof BootInflater)) {
			throw new RuntimeException(
					"unable to initialize application for BootInflater");
		}
	}
}
