/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.gigya;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollInvocation;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;
import org.json.JSONException;
import org.json.JSONObject;

import com.gigya.socialize.GSObject;
import com.gigya.socialize.GSResponse;
import com.gigya.socialize.GSResponseListener;
import com.gigya.socialize.android.GSAPI;
import com.gigya.socialize.android.event.GSConnectUIListener;
import com.gigya.socialize.android.event.GSEventListener;
import com.gigya.socialize.android.event.GSLoginUIListener;

@Kroll.module(name="Gigya", id="ti.gigya")
public class GigyaModule extends KrollModule
{
	// Standard Debugging variables
	private static final String LCAT = "GigyaModule";

	public GigyaModule(TiContext tiContext) {
		super(tiContext);
	}

	// Accessor for Gigya API object. Allocated on first use.
	private static GSAPI _gsAPI = null;
	
	public GSAPI getGSAPI(KrollInvocation invocation) 
	{
		if (_gsAPI == null) {
			String apiKey = TiConvert.toString(getProperty("apiKey"));
			if (apiKey.length() == 0) {
				Log.e(LCAT, "[ERROR] apiKey property is  not set");
				return null;
			}
			
			_gsAPI = new GSAPI(apiKey, invocation.getActivity());
			_gsAPI.setEventListener(new GlobalEventListener());
		}
		return _gsAPI;
	}
	
/* ---------------------------------------------------------------------------------
   Utility methods
   --------------------------------------------------------------------------------- */
	
	private static GSObject GSObjectFromArgument(Object args)
	{
		GSObject gsObj = null;
		
	    // We support passing the parameters as a dictionary or a JSON string.
	    // Based on the class of the parameter we will then convert to a GSObject
	    // that can be used for the Gigya APIs.
	
		if (args != null) {
			if (args instanceof KrollDict) {
				try {
					gsObj = new GSObject(TiConvert.toJSONString((KrollDict)args));
				} catch (Exception e) {
					Log.e(LCAT, "Unable to convert dictionary to Gigya object");
				}
			} else if (args instanceof String) {
				try {
					gsObj = new GSObject((String)args);
				} catch (Exception e) {
					Log.e(LCAT, "Unable to convert string to Gigya object");
				}
			} else {
				throw new IllegalArgumentException("Expected dictionary or JSON string");
			}
		}
		
		return gsObj;
	}
	
	private static KrollDict dataFromGSObject(GSObject obj)
	{
		KrollDict data = null;
		
	    // A GSObject is returned from many of the Gigya APIs. We need to
	    // convert that object to an NSDictionary that can be passed back
	    // to the JavaScript event handler.
	    
	    if (obj != null) {
	    	try {
	    		data = new KrollDict(new JSONObject(obj.toJsonString()));
	    	} catch (JSONException e) {
	    		Log.e(LCAT, "Error converting JSON string to KrollDict");
	    	}
	    }
		
		return data;
	}

/* ---------------------------------------------------------------------------------
   showLoginUI method and delegates
   --------------------------------------------------------------------------------- */
	
	@Kroll.constant public static final String LOGINUI_DID_LOGIN = "loginui_did_login";
	@Kroll.constant public static final String LOGINUI_DID_CLOSE = "loginui_did_close";
	@Kroll.constant public static final String LOGINUI_DID_FAIL = "loginui_did_fail";
	@Kroll.constant public static final String LOGINUI_DID_LOAD = "loginui_did_load";
	
	@Kroll.method(runOnUiThread=true)
	public void showLoginUI(KrollInvocation invocation, Object args)
	{
		GSAPI gsAPI = getGSAPI(invocation);
		GSObject gsObj = GSObjectFromArgument(args);
		
		gsAPI.showLoginUI(gsObj, new LoginUIListener(), null);
	}
	
	class LoginUIListener implements GSLoginUIListener 
	{
		public void onLogin(String provider, GSObject user, Object context)
		{
			if (hasListeners(LOGINUI_DID_LOGIN)) {
				KrollDict event = new KrollDict();
				event.put("provider", provider);
				event.put("user", dataFromGSObject(user));
				
				fireEvent(LOGINUI_DID_LOGIN, event);
			}
		}
		
		public void onLoad(Object context)
		{
			if (hasListeners(LOGINUI_DID_LOAD)) {
				KrollDict event = new KrollDict();
				
				fireEvent(LOGINUI_DID_LOAD, event);
			}
		}
		
		public void onError(int errorCode, String errorMessage, String trace, Object context)
		{
			if (hasListeners(LOGINUI_DID_FAIL)) {
				KrollDict event = new KrollDict();
				event.put("code", errorCode);
				event.put("message", errorMessage);
				
				fireEvent(LOGINUI_DID_FAIL, event);
			}
		}
		
		public void onClose(boolean canceled, Object context)
		{
			if (hasListeners(LOGINUI_DID_CLOSE)) {
				KrollDict event = new KrollDict();
				event.put("canceled", canceled);

				fireEvent(LOGINUI_DID_CLOSE, event);
			}
		}
	}

/* ---------------------------------------------------------------------------------
   showAddConnectionsUI method and delegates
   --------------------------------------------------------------------------------- */

	@Kroll.constant public static final String ADDCONNECTIONSUI_DID_CONNECT = "addconnectionsui_did_connect";
	@Kroll.constant public static final String ADDCONNECTIONSUI_DID_CLOSE = "addconnectionsui_did_close";
	@Kroll.constant public static final String ADDCONNECTIONSUI_DID_FAIL = "addconnectionsui_did_fail";
	@Kroll.constant public static final String ADDCONNECTIONSUI_DID_LOAD = "addconnectionsui_did_load";

	@Kroll.method(runOnUiThread=true)
	public void showAddConnectionsUI(KrollInvocation invocation, Object args)
	{
		GSAPI gsAPI = getGSAPI(invocation);
		GSObject gsObj = GSObjectFromArgument(args);
		
		gsAPI.showAddConnectionsUI(gsObj, new ConnectUIListener(), null);
	}
	
	class ConnectUIListener implements GSConnectUIListener 
	{
		public void onConnectionAdded(String provider, GSObject user, Object context)
		{
			if (hasListeners(ADDCONNECTIONSUI_DID_CONNECT)) {
				KrollDict event = new KrollDict();
				event.put("provider", provider);
				event.put("user", dataFromGSObject(user));
				
				fireEvent(ADDCONNECTIONSUI_DID_CONNECT, event);
			}
		}
		
		public void onLoad(Object context)
		{
			if (hasListeners(ADDCONNECTIONSUI_DID_LOAD)) {
				KrollDict event = new KrollDict();
				
				fireEvent(ADDCONNECTIONSUI_DID_LOAD, event);
			}
		}
		
		public void onError(int errorCode, String errorMessage, String trace, Object context)
		{
			if (hasListeners(ADDCONNECTIONSUI_DID_FAIL)) {
				KrollDict event = new KrollDict();
				event.put("code", errorCode);
				event.put("message", errorMessage);
				
				fireEvent(ADDCONNECTIONSUI_DID_FAIL, event);
			}
		}
		
		public void onClose(boolean canceled, Object context)
		{
			if (hasListeners(ADDCONNECTIONSUI_DID_CLOSE)) {
				KrollDict event = new KrollDict();
				event.put("canceled", canceled);

				fireEvent(ADDCONNECTIONSUI_DID_CLOSE, event);
			}
		}
	}
	
/* ---------------------------------------------------------------------------------
   login / logout methods
   --------------------------------------------------------------------------------- */
	
	@Kroll.method(runOnUiThread=true)
	public void login(KrollInvocation invocation, Object args)
	{
		GSAPI gsAPI = getGSAPI(invocation);
		GSObject gsObj = GSObjectFromArgument(args);
		
		try {
			gsAPI.login(gsObj, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	@Kroll.method(runOnUiThread=true)
	public void logout(KrollInvocation invocation)
	{
		GSAPI gsAPI = getGSAPI(invocation);
		
		gsAPI.logout();
	}

/* ---------------------------------------------------------------------------------
   addConnection / removeConnection methods
   --------------------------------------------------------------------------------- */
	
	@Kroll.method
	public void AddConnection(KrollInvocation invocation, Object args)
	{
		GSAPI gsAPI = getGSAPI(invocation);
		GSObject gsObj = GSObjectFromArgument(args);
		
		try {
			gsAPI.addConnection(gsObj, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Kroll.method
	public void RemoveConnection(KrollInvocation invocation, Object args)
	{
		GSAPI gsAPI = getGSAPI(invocation);
		GSObject gsObj = GSObjectFromArgument(args);
		
		try {
			// NOTE: This method name is misspelled in the Gigya SDK
			gsAPI.removeConnetion(gsObj, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/* ---------------------------------------------------------------------------------
   sendRequest method and delegate
   --------------------------------------------------------------------------------- */

	@Kroll.constant public static final String RESPONSE = "response";

	@Kroll.method(runOnUiThread=true)
	public void sendRequest(KrollInvocation invocation, KrollDict args)
	{
	    // NOTE: This must be called on the UI thread, eventhough it doesn't perform any UI
		GSAPI gsAPI = getGSAPI(invocation);
	
		String method = args.getString("method");
		GSObject gsObj = GSObjectFromArgument(args.get("params"));
		boolean useHTTPS = args.optBoolean("useHTTPS", false);
		
		try {
			gsAPI.sendRequest(method, gsObj, useHTTPS, new RequestResponseListener(), null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class RequestResponseListener implements GSResponseListener
	{
		public void onGSResponse(String method, GSResponse response, Object context)
		{
			if (hasListeners(RESPONSE)) {
				try {
					KrollDict event = new KrollDict();
					event.put("method", method);
					event.put("errorCode", response.getErrorCode());
					event.put("data", dataFromGSObject(response.getData()));
					event.put("responseText", response.getResponseText());
					event.put("errorMessage", response.getErrorMessage());
					
					fireEvent(RESPONSE, event);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

/* ---------------------------------------------------------------------------------
   Event delegates
   --------------------------------------------------------------------------------- */

	@Kroll.constant public static final String DID_LOGIN = "did_login";
	@Kroll.constant public static final String DID_LOGOUT = "did_logout";
	@Kroll.constant public static final String DID_ADD_CONNECTION = "did_add_connection";
	@Kroll.constant public static final String DID_REMOVE_CONNECTION = "did_remove_connection";

	class GlobalEventListener implements GSEventListener
	{
		public void onLogin(String provider, GSObject user, Object context)
		{
			if (hasListeners(DID_LOGIN)) {
				KrollDict event = new KrollDict();
				event.put("provider", provider);
				event.put("user", dataFromGSObject(user));
				
				fireEvent (DID_LOGIN, event);
			}
		}
		
		public void onLogout(Object context)
		{
			if (hasListeners(DID_LOGOUT)) {
				KrollDict event = new KrollDict();
				
				fireEvent (DID_LOGOUT, event);
			}
		}
		
		public void onConnectionAdded(String provider, GSObject user, Object context) 
		{
			if (hasListeners(DID_ADD_CONNECTION)) {
				KrollDict event = new KrollDict();
				event.put("provider", provider);
				event.put("user", dataFromGSObject(user));
				
				fireEvent (DID_ADD_CONNECTION, event);
			}
		}
		
		public void onConnectionRemoved(String provider, Object context)
		{
			if (hasListeners(DID_REMOVE_CONNECTION)) {
				KrollDict event = new KrollDict();
				event.put("provider", provider);
				
				fireEvent (DID_REMOVE_CONNECTION, event);
			}
		}
	}
}