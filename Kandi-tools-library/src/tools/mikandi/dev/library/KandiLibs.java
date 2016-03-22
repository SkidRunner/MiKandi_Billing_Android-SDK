package tools.mikandi.dev.library;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.saguarodigital.returnable.defaultimpl.JSONResponse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import tools.mikandi.dev.ads.FullScreenAd;
import tools.mikandi.dev.ads.OnFullScreenAdDisplayedListener;
import tools.mikandi.dev.inapp.AccountBalancePoint;
import tools.mikandi.dev.inapp.OnAuthorizeInAppListener;
import tools.mikandi.dev.inapp.OnValidationListener;
import tools.mikandi.dev.inapp.onBuyGoldReturnListener;
import tools.mikandi.dev.inapp.onPurchaseHistoryListener;
import tools.mikandi.dev.inapp.onUserVerificationListener;
import tools.mikandi.dev.login.AAppReturnable;
import tools.mikandi.dev.login.LoginActivity;
import tools.mikandi.dev.login.LibraryLoginResult;
import tools.mikandi.dev.login.LoginStorageUtils;
import tools.mikandi.dev.passreset.DefaultJSONAsyncTask;
import tools.mikandi.dev.passreset.OnJSONResponseLoadedListener;
import tools.mikandi.dev.purchase.AuthorizePurchaseReturnable;
import tools.mikandi.dev.purchase.ListPurchasesReturnable;
import tools.mikandi.dev.utils.InstallerCheck;
import tools.mikandi.dev.utils.UserInfoObject;
import tools.mikandi.dev.validation.ValidatePurchaseReturnable;
import tools.mikandi.dev.validation.ValidateUserReturnable;

/**
 * The main KandiLibs class. All API calls use a method from this class.
 * Feel free to go look about the source code, but it gets complicated quickly.
 * You <b> will regret </b> autoformatting this page! 
 */
public class KandiLibs {

	public static final boolean debug = true;
	public static final String sAppId = "appid";
	public static final String sSecret = "secretkey";
	public static final String sPublisherId = "publisherid"; 
	public static boolean ownedBoolean = false;
	public static boolean balanceloading = false;
	private static onPurchaseHistoryListener sPurchaseHistory = null;
	private static OnValidationListener sValidate = null;
	private static onUserVerificationListener sOnUserVerification = null;
	private static OnAuthorizeInAppListener sAuthInAppListener = null;	
	private static OnFullScreenAdDisplayedListener sOnFullScreenAdDisplayedListener = null;
	private static onBuyGoldReturnListener sOnBuyGoldReturn = null;

	/**
	 * These are the login activity static ints. 
	 * The ID is used as the the request code 
	 *	The RESULT int's are returned in the login int. 
	 *
	 *	Developers should be sure to handle these, and be sure to handle results. 
	 *  - Unsucessful generally means login credentials are correct. 
	 *  - Login Failed means that their is a networking issue or the device is unable to connect to our mikandi servers. 
	 *  
	 */
	public static int LIBRARY_LOGIN_ID = 0x0001; 
	public static int LIBRARY_LOGIN_RESULT_SUCCESSFUL = 0x0002;
	public static int LIBRARY_LOGIN_RESULT_UNSUCESSFUL = 0x0003;
	public static int LIBRARY_LOGIN_RESULT_FAILED = 0x0004;

	public static int LIBRARY_BUYGOLD_ID = 0x0011;
	public static int LIBRARY_BUYGOLD_RESULT_UNSUCESSFUL = 0x0012;
	public static int LIBRARY_BUYGOLD_RESULT_FAILED = 0x0013;
	public static int LIBRARY_BUYGOLD_RESULT_SUCESSFUL = 0x0014;
	public static int LIBRARY_BUYGOLD_RESULT_ABANDOND = 0x0015;
	
	public static int LIBRARY_AD_ID = 0x0031; 
	public static int LIBRARY_AD_RESULT_CLICKED = 0x0032;
	public static int LIBRARY_AD_RESULT_CLOSED = 0x0033;
	public static int LIBRARY_AD_SEEN = 0x0034; 
	
	/**
	 * 
	 * Welcome to Kandilibs!
	 * This is the api/interface/intermediate between you (the developer) and
	 * the Mikandi apis. We have built in functionality, that allows you to log
	 * in users to Mikandi, and verify users while the user is using your app
	 * 
	 * Validating Mikandi user's allows developers to not worry too much about
	 * DRM, and people illegally using you're work, but allows you to focus on
	 * improvement and delievering high quality content. We hope to create an
	 * easy to use implementation of our billing service that allows you the
	 * developer to get the most out of our service, create the best content and
	 * make you the most money possible!
	 * 
	 * Dev Notes: - In-app buying gold, is currently not yet implemented. I have
	 * demonstrated an easy way to redirect the user to the buy gold activity
	 * within the mikandi client. This is currently a work in progress however
	 * and in later versions we hope to be able to support within-app buy gold.
	 *  
	 * We have extensive notes on the implementation, design and use of this
	 * library and that material can be found at:
	 * 
	 *  http://tools.mikandi.com/confluence/display/KANDI/KandiLibs+Documentation
	 * 
	 * and i (Mike, the library developer) can be reached at: kandilibs@mikandi.com
	 * or mike@mikandi.com
	 * 
	 * I encourage you too reach out if you have any trouble and more importantly 
	 * if you could recommend any improvements, or any features that as developers, would
	 * help you out.
	 * 
	 */

	// --------------------------------------------------Buy Gold Activity-----------------------------------------------------------------------
	/**
	 * Starts the buy gold activity, opens in Mikandi, unless Mikandi store not present in which the user 
	 * will be prompted to open the buy gold page in a url 
	 * 
	 * @param act - Reference to the activity in which this has been called. 
	 */
	public static final void requestBuyGold(final Activity act) {
		String url = "https://mikandi.com/buygold";
		Intent mIntent = new Intent(Intent.ACTION_VIEW);
		mIntent.setData(Uri.parse(url));
		act.startActivityForResult(mIntent, LIBRARY_BUYGOLD_ID); 
	}

	// -------------------------------------------------------- Buy Gold Activity End  --------------------------------------------------------------

	public static String adListener = "fullScreenadlistener";
	public static final void requestFullScreenAd(Activity a, OnFullScreenAdDisplayedListener l) { 
		// if connected to the internet.
		Intent mIntent = new Intent(a, FullScreenAd.class);
		a.startActivityForResult(mIntent, LIBRARY_AD_ID);
	}
	
	// ------------------------------------------------------Login Activity----------------------------------------------------------------------------------
	/**
	 * 
	 * This starts the login, 
	 * 
	 * @param act
	 * @param uio
	 * @param loginListener
	 */
	public static final void requestLogin(final Activity act, UserInfoObject uio) {

		if (debug) Log.i("DEBUGGING XML ERROR: ", "Request Login in library");
		String mSecret = uio.getSecretKey();
		String mAppId = uio.getAppId();
		if (debug) Log.i("DEBUGGING XML ERROR: ", "Secret key  " + mSecret	+ " and appid " + mAppId);
		Intent mIntent = new Intent(act, LoginActivity.class);
		if (debug) Log.i("DEBUGGING XML ERROR: ", "Login Intent created");
		mIntent.putExtra(sSecret, mSecret);
		mIntent.putExtra(sAppId, mAppId);
		if (debug) Log.i("DEBUGGING XML ERROR: ", "starting login activity");
		act.startActivityForResult(mIntent, LIBRARY_LOGIN_ID);
	
	}
	// ---------------------------------------------- Login Activity End----------------------------------------------------------------------------
	
	// --------------------------------------------------- Retreiving balance 
	
	
	
	
	// ----------------------------------------- End balance  -----------------------------------------
	//----------------------------------------- Logout --------------------------------------------------------------------------------------
	/** Function call to log user out of Mikandi and wipe the loginresults 
	 *  from the UserInfoObject.
	 *  @param uio  
	 */
	public static final void logOutUser(UserInfoObject uio) { 
			Context mContext = uio.getContext();
			LoginStorageUtils.clear(mContext);
		}
	// ---------------------------------------------- End Log out user ----------------------------------------------------------------------
	public static final boolean isLoggedIn(UserInfoObject uio) { 
		Context mContext = uio.getContext();
		boolean isLoggedIn = LoginStorageUtils.isLoggedIn(mContext);
		return isLoggedIn;
	}
	// ----------------------------------------------Validate purchase-----------------------------------------------------------------------------
	
	/**
	 * This function is to be called after the AuthInAppPurchase call, as an additional 
	 * validation, this provides a positive response if the purchase has been validated 
	 * on the servers and inform the developer that the purchase was completed properly. 
	 * 
	 * @param uio - UserInfoObject
	 * @param validateListener - Interface for call back s
	 * @param mToken	 - String purchaseId
	 * @param mDescription - A string description of the app (THESE SHOULD NEVER CHANGE) 
	 * @param mAmount - A String representation of the amount of Mikandi Gold
	 */
	public static void requestValidateInApp(UserInfoObject uio,
			final OnValidationListener validateListener, String mToken) {

		sValidate = validateListener;
		LibraryLoginResult lr = uio.getLoginResult();
		Context context = uio.getContext();

		if (!isConnectedToNetwork(context)) { 
			sValidate.FailedValidation();
			return;
		}
		
		if (lr == null) {
			if (debug)
				Log.e("Request Validate In App ", "login Result is null!");
			Toast.makeText(uio.getContext(),
					"Need to login before you do this! ", Toast.LENGTH_SHORT)
					.show();
			return;
			}
		try {
		
			HashMap<String, String> args = new HashMap<String, String>();
			args.put(AAppReturnable.APP_ID, uio.getAppId());
			args.put(AAppReturnable.APP_SECRET, uio.getSecretKey());
			args.put(AAppReturnable.USER_ID, lr.getUserIdString());
			args.put(AAppReturnable.AUTH_HASH, lr.getUserAuthHash());
			args.put(AAppReturnable.TOKEN, mToken);
			args.put(AAppReturnable.AUTH_EXPIRES, lr.getUserAuthExpires());
			new DefaultJSONAsyncTask<ValidatePurchaseReturnable>(
					ValidatePurchaseReturnable.class,
					context,
					new OnJSONResponseLoadedListener<ValidatePurchaseReturnable>() {
						@Override
						public void onJSONLoaded(
								JSONResponse<ValidatePurchaseReturnable> jsonResponse) {
							
							if (jsonResponse == null) { 
								validateResponseHandler(null);
								return;
							}
							//json response may be null here: 
							validateResponseHandler(jsonResponse);
							
						}
					}, args).execute();
		} catch (Exception E) {
			E.printStackTrace();
		}
	}

	protected static void validateResponseHandler(JSONResponse<ValidatePurchaseReturnable> jsonResponse) {
		if (jsonResponse == null) {
			sValidate.FailedValidation();
			return; 
		}
		ValidatePurchaseReturnable vpr = jsonResponse.getOne(); 
		if (debug) Log.e("VALIDATEPURCHASE RESPONSE IS", "" + jsonResponse);
		if (jsonResponse.getCode() != 200) {
			if (debug) Log.e("Kandilibs/Networking error", jsonResponse.getCode() + " returned");
			sValidate.FailedValidation();
		} else {
			if (vpr.isPurchased()) { 
				sValidate.PositiveValidation();
			} else { 
				sValidate.NegativeValidation();
			}
			if (debug) Log.i("Validated Response : ","" + jsonResponse.getCode());
		}
	}
	// ------------------------------------------Validate PurchaseEnd--------------------------------------------------------------------------------
	// ------------------------------------- Auth in app purchase Authorization -------------------------------- ---------------------------------------
	/**
	 * AuthInAppPurchase is the function called by the developer to authorize a in-app 
	 * purchase of some content within the application. The AuthInAppPurchase function is normally 
	 * called before the ValidateInAppPurchase function. 
	 * 
	 * AuthInApp takes in the UserInfoObject, and 3 string params and runs idempotent, so 
	 * repeated requests don't nessesarily mean repeated transaction.  
	 * 
	 * mToken is the purchase ID, constant across your app, each different available transaction 
	 * will have a different purchaseId. 
	 * 
	 * mDescription is a cleartext description of the transaction. Some examples would be : 
	 * 	ex1: "content pack - Asian" 
	 *  ex2: "Mikandi - Fun pack 1" 
	 *  ex3: "in-app purchase 2: Unlock membership"
	 * 
	 * mAmount is the amount of Mikandi Gold that the transaction will cost the user.
	 * this amount has to have been entered into the developer panel so our services
	 * can authorize payment accordingly this should just be placed into the function as a
	 * String 
	 * 
	 * @param uio - UserInfo Object 
	 * @param mToken - purchase ID 
	 * @param mDescription - String description of purchase
	 * @param mAmount - amount of Gold (as a string)
	 * @param authInAppListener - interface used as way of implementing callbacks
	 */
	public static void AuthInAppPuchase(UserInfoObject uio ,String mToken, String mDescription, String mAmount, 
			 OnAuthorizeInAppListener authInAppListener){
		sAuthInAppListener = authInAppListener;
		
		if (!isConnectedToNetwork(uio.getContext())) { 
			sAuthInAppListener.Failed(-1);
			return; 
		}
		
		Context context = uio.getContext();
		LibraryLoginResult lr = uio.getLoginResult();
		
		if (lr == null) {
			if (debug) Log.e("authInAppPurchase " , "error! login result is null");
			if (debug) Log.e("In-App purchase Error" , "User not logged in ! ");
			Toast.makeText(uio.getContext(), "Need to login first! ", Toast.LENGTH_LONG).show();
			return;
		}

		try {
		
		// need to start the task here
		HashMap<String, String> args = new HashMap<String, String>();
		args.put(AAppReturnable.APP_ID, uio.getAppId());
		args.put(AAppReturnable.APP_SECRET, uio.getSecretKey());
		args.put(AAppReturnable.USER_ID, lr.getUserIdString());
		args.put(AAppReturnable.AUTH_HASH, lr.getUserAuthHash());
		args.put(AAppReturnable.AUTH_EXPIRES, lr.getUserAuthExpires());
		args.put(AAppReturnable.DESCRIPTION	,mDescription.trim().toLowerCase(Locale.getDefault()));
		args.put(AAppReturnable.TOKEN, mToken);
		args.put(AAppReturnable.AMOUNT, mAmount.trim());
		
		new DefaultJSONAsyncTask<AuthorizePurchaseReturnable>(
			AuthorizePurchaseReturnable.class,
			context,
			new OnJSONResponseLoadedListener<AuthorizePurchaseReturnable>() {
				@Override
				public void onJSONLoaded(
					JSONResponse<AuthorizePurchaseReturnable> jsonResponse) {
								
					if (jsonResponse != null) {
					
					int code = jsonResponse.getCode();
					inappAuthorizationHandler(code);
					
						} else {
					if (debug) Log.e("Authorizing in app", "json is null");
						return;
									}
								}
							}, args).execute();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
	
	/**
	 *  This is a callback handler for the AuthInApp function
	 *  @param code - this is the return code
	 */
	protected static void inappAuthorizationHandler(int code) {
		if (code == 200) { 
			sAuthInAppListener.Sucess();
		} else{ 
			sAuthInAppListener.Failed(code);
		}
	}
	// ------------------------------------ ENd in app Purchase Authorization ------------------------------------------------------

	// ------------------------------------------ Purchase History --------------------------------------------------------------------------------------
	
	/**
	 * This is a function that returns a boolean value representing wheather or not the given token has been purchased by the current 
	 * user. This is the easier and quickest way to check to see if a user has purchased user content! 
	 * 
	 * This checks the stored login details , then checks the network and retreives the list. 
	 * 
	 * 
	 * @param uio	= UserInfoObject, an instance should be collected previously and passed into this function! 
	 * @param token = String Id of the purchase
	 * @return
	 */
	public static boolean checkPurchase(final UserInfoObject uio, final String token){
		//have to set owned because boolean can be changes in the onPurchaseHistoryListener()
		setOwned(false);
		
		if (uio.getLoginResult() == null){
			if (debug) Log.e("CheckPurchase library call" , "User needs to be logged in.");
			Toast.makeText(uio.getContext(), "You need to log in first!" , Toast.LENGTH_LONG).show();
			return false;
		}
		if (uio.getLoginResult() != null){
			LibraryLoginResult lr = uio.getLoginResult();
			
			if (lr.getArrayListTokens() != null && lr.getArrayListTokens().contains(token)){
				setOwned(true);
				return getOwned();
		}
		else {
			if (debug) Log.i("checking token", "none from lr");
			}
		}
		try {
			
			if (!isConnectedToNetwork(uio.getContext())) { 
				
				return getOwned();
			}
			
		requestPurchaseHistory(uio, new onPurchaseHistoryListener() {
			
			@Override
			public void onSucessfulHistoryRetrieved(List<String> mTokens) {
				setOwned(mTokens.contains(token));		
				if (debug) Log.i("Retreiving Purchase History " , "Tokens Sucessfully retreivied from server"); 
				if (debug) Log.i("Checking token from purchases "  , " Token was " + ((mTokens.contains(token) == true) ? " found " : " not found - token not purchased by user ")); 
			}
			
			@Override
			public void onFailedHistoryRetrieved() {
				setOwned(false);
				if (debug) Log.e("Retreiving Purchase History " , "Trouble Retreiving list... Try again later");
			}

			@Override
			public void onNoPurchases() {
				setOwned(false);
				if (debug) Log.e("Retreiving Purchase History " , "No purchases found");
			}
		});
		
		if (debug) Log.i("Token Checking "  , "Token :" + token + " is : " + ((getOwned() == true) ? " purchased " : " not purchased ")); 
		
		} catch (Exception E) { 
			
		}
		
		return getOwned();
	}
	
	/**
	 * This returns the purchase History of the User, this allows the developer
	 * to determine what in-app purchases or access to premium content the user
	 * has purchased.
	 * 
	 * There are two ways this can be used the first is that by modifying the
	 * which_action variable, one use of this function is to retreive tokens
	 * from the stored login result on the device, one use of function is to
	 * retreive the list from the server.
	 * 
	 * I would recommend that this from device is done on start up of the app,
	 * or even near the beging of the app life cycle. By doing this, you can
	 * determine if the user has purchased all you're premium content you offer,
	 * and if so, just not have the app-check in with the server at all.
	 * 
	 * 
	 * @param UserInfoObject uio - Stores all nessesary information needed for API
	 * @param purchaseHistoryListener - Interface for overall function
	 */
	public static void requestPurchaseHistory(final UserInfoObject uio ,final onPurchaseHistoryListener purchaseHistoryListener) {
		
		if (debug) Log.i("KandiLibs - RequestPurchaseHistory","method called");
		
		final Context context = uio.getContext();
		LibraryLoginResult lr = uio.getLoginResult();
		sPurchaseHistory = purchaseHistoryListener;
		
		if (lr == null) {
			if (debug) Log.e("RequestPurchase History " , "error! login result is null");
			Toast.makeText(uio.getContext(), "Request purchase History - lr is null", Toast.LENGTH_LONG).show();
			return;
		}
		else {
			if (!isConnectedToNetwork(context)) { 
				sPurchaseHistory.onFailedHistoryRetrieved();
				return;
			}
			
			if (debug) Log.i("KandiLibs - RequestPurchaseHistory","Instantiating variables ");
				HashMap<String, String> args = new HashMap<String, String>();
				args.put(AAppReturnable.APP_ID, uio.getAppId());
				args.put(AAppReturnable.APP_SECRET, uio.getSecretKey());
				args.put(AAppReturnable.USER_ID, lr.getUserIdString());
				args.put(AAppReturnable.AUTH_HASH, lr.getUserAuthHash());
				args.put(AAppReturnable.AUTH_EXPIRES, lr.getUserAuthExpires());
	
		try {
			new DefaultJSONAsyncTask<ListPurchasesReturnable>(
			ListPurchasesReturnable.class,
			context,
			new OnJSONResponseLoadedListener<ListPurchasesReturnable>() {
		
		@Override
		public void onJSONLoaded(
			JSONResponse<ListPurchasesReturnable> jsonResponse) {
			List<String> mTokens = null;
									
			if (jsonResponse != null) {
			
				if (debug) Log.i("Server response from get list" , "" + jsonResponse.getCode());
		
			if (jsonResponse.getCode() == 401) { 
				sPurchaseHistory.onNoPurchases();
				return;
			}

			ListPurchasesReturnable mList = (ListPurchasesReturnable) jsonResponse.getOne();
			mTokens = mList.getArrayListTokens();
			
			if (mTokens != null) 
			{ 
				String[] updateTokens = new String[mTokens.size()];
				updateTokens = mTokens.toArray(updateTokens);
				LoginStorageUtils.refreshToken(uio.getContext(), updateTokens);
			}
				purchaseHistoryHandler(context , mTokens);
			} else {
				if (debug) Log.e("ListPurchaseTask", "json is null");
				sPurchaseHistory.onFailedHistoryRetrieved();
			}
		}
					}, args).execute();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	
	
	/**
	 * This is the call back handler for the RequestPurchaseHistory application 
	 * tokens passed through to the on sucessful callback, void onSucessful included
	 * 
	 * @param mTokens
	 */
	protected static void purchaseHistoryHandler(Context ctx, List<String> mTokens) {
		if (mTokens != null) {
			if (debug) Log.i("printing out tokens" , "tokens :" + mTokens.toString());
			sPurchaseHistory.onSucessfulHistoryRetrieved(mTokens);
			LoginStorageUtils.refreshToken(ctx, mTokens.toArray(new String[mTokens.size()]));
		} else {
			sPurchaseHistory.onFailedHistoryRetrieved();
		}
	}
	// --------------------------------------- End Purchase history--------------------------------------------------------------------------------------

	// ------------------------------------------------ Gold Retreival ----------------------------------------------------
	/**
	 * This function returns -1 if the balance hasn't been retreived correctly. 
	 * @param l
	 * @param u
	 */
	public static void getBalance(final onBuyGoldReturnListener l , UserInfoObject u) { 
		sOnBuyGoldReturn = l; 
		final Context c = u.getContext(); 
		LibraryLoginResult lr = u.getLoginResult();
		
		if (!isConnectedToNetwork(c)) { 
			l.retreivedGold(-2);
			return; 
		}
		
		if (lr == null) { 
			Toast.makeText(c, "Please log in first" , Toast.LENGTH_SHORT).show();
			l.retreivedGold(-2);
			return;
		}
		
		HashMap<String, String> args = new HashMap<String, String>();
		args.put(AAppReturnable.APP_ID, u.getAppId());
		args.put(AAppReturnable.USER_ID, lr.getUserId() + "");
		args.put(AAppReturnable.AUTH_HASH, lr.getUserAuthHash());
		args.put(AAppReturnable.AUTH_EXPIRES, lr.getUserAuthExpires());
		
		try
		{
			new DefaultJSONAsyncTask<AccountBalancePoint>
				(AccountBalancePoint.class, c ,new OnJSONResponseLoadedListener<AccountBalancePoint>(){
					@Override
					public void onJSONLoaded(
							JSONResponse<AccountBalancePoint> jsonResponse) {
						if (jsonResponse == null) {
							l.retreivedGold(-2);
						}
						if (jsonResponse.getCode() != 200) { 
							l.retreivedGold(-1);
						} else { 
							AccountBalancePoint a = jsonResponse.getOne(); 
							l.retreivedGold(a.getBalance());
						}
						
					}}, args).execute();
		}
		catch (Exception e)
		{
			if (debug) Log.e("Gold balance Retreiveal " , " ", e);
		}	
	}
	
	// ----------------------------------------------- End  Gold Retreival --------------------------------------------------------
	
	
	
	// ------------------------------------------------------ Verify User ---------------------------------------------------
	/**
	 * This will tell you whether or not the user has purchased the application from the MiKandi app store. 
	 * Becareful not to lock out the users who purchase your app! 
	 * 
	 * @param UserInfoObject uio 
	 * @param onUserVerificationListener userValidListener 
	 */
	public static void requestUserVerify(UserInfoObject uio, onUserVerificationListener userValidListener) {
			sOnUserVerification = userValidListener;
		
		try {
			if (uio.getLoginResult() == null) {
				Log.i("Request user verfiy" , "login is null");
				Toast.makeText(uio.getContext(), "Need to login First! " , Toast.LENGTH_LONG).show();
				return; 
			}
		} catch (Exception E) { 
			if (debug) Log.e("error thrown in retreiving userverify , when not logged in" , "printing error: " + E);
		}
			
		Context ctx = uio.getContext();
		
		if (!isConnectedToNetwork(ctx)) { 
			sOnUserVerification.userVerifyFailed(-1);
			return;
		}
		
		
		LibraryLoginResult lr = uio.getLoginResult();
		if (debug) Log.i("Printing LoginResult", "" + lr);
		final HashMap<String, String> hm = new HashMap<String, String>();
		hm.put(AAppReturnable.USER_ID, lr.getUserIdString());
		hm.put(AAppReturnable.APP_ID, uio.getAppId());
		hm.put(AAppReturnable.AUTH_HASH, lr.getUserAuthHash());
		hm.put(AAppReturnable.APP_SECRET, uio.getSecretKey());

		try {
			new DefaultJSONAsyncTask<ValidateUserReturnable>(
					ValidateUserReturnable.class, ctx,
					new OnJSONResponseLoadedListener<ValidateUserReturnable>() {

						public void onJSONLoaded(
								JSONResponse<ValidateUserReturnable> jsonResponse) {
							try {
								if (jsonResponse != null) { 
								ValidateUserReturnable mResponse = (ValidateUserReturnable) jsonResponse.getOne();
								if (debug) Log.e("Code from request User verify : " , "" + jsonResponse.getCode());
								
								if (mResponse.isValidated()) 
									isValid(true, jsonResponse.getCode());
								 else 
									isValid(false, jsonResponse.getCode());
								}
								else { // if json response is null
									if (debug) Log.e("ValidateUserReturnable", "jsonResponse is null");
								}
					} catch (Exception E) {
						if (debug) Log.e("error thrown in request User verification",	"Error is : "+ E);
					}
						}
					}, hm).execute();

		} catch (Exception E) {
			if (debug) Log.i("Error thrown in request userVerfication",
					"creating the task constructor");
		}
	}
	/**
	 * isValid is the callback handler for the RequestUserVerify function the isValid 
	 * method is called regardless of the result of the RequestUserVerify
	 * 
	 * @param sucessfulVerification this is a boolean value that determines which interface callback 
	 * is used 
	 * @param code this is the request Return code that is returned along with the failed 
	 * 	 */
	protected static void isValid(boolean sucessfulVerification, int code) {
		if (sucessfulVerification) {
			sOnUserVerification.userVerifiedSuccessfully();
		} else {
			sOnUserVerification.userVerifyFailed(code);
		}
	}
	// ------------------------------------------------------ End Verify User--------------------------------------------------------
	/**
	 * This checks the package installer and will return true if the package 
	 * was installed by MiKandi. (non-network based DRM)
	 * @param ctx
	 * @return
	 */
	public static boolean checkInstaller(UserInfoObject uio) {	
		return InstallerCheck.checkInstaller(uio.getContext());
	}
	
	// -------------------------------------------- UserValueRetreival ----------------
	
	
	// -------------------------------------------------- Random functions -----------------------------------------------------------------
	// setters are used when we need to handle a owned variable inside a listener and can't reference a class var without setters.
	private static void setOwned(boolean var) { 
		ownedBoolean = var;
	}
	private static  boolean getOwned() { 
		return ownedBoolean;
	}
	
	public static boolean isConnectedToNetwork(Context context) { 
			boolean ret = false;
			
			ConnectivityManager cm =
			        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			 
			NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
			ret = activeNetwork != null &&
			                      activeNetwork.isConnectedOrConnecting();
			
			return ret;
	}
	// -------------------------------------------------End Random Functions ---------------------------		
}
