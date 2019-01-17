package com.zebra.windevmobiledatawedgewrapper;

// Imports
import com.zebra.datawedgeprofileintents.*;
import android.util.Log;
import android.text.TextUtils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


public class DatawedgeIntentWrapperFacade {

    public interface IAppelProcedureWL
    {
        boolean appelProcedureWL(Object... aParameters);
    }

    public interface IContextRetriever
    {
        Context getContext();
    }

    // Membres
    public static String TAG = "DatawedgeIntentWrapperFacade";

    // Paramètres d'intent pour la réception des codes scannés
    public static String mIntentAction = "com.symbol.windevdatawedgedemo.RECVR";
    public static String mIntentCategory = "android.intent.category.DEFAULT";

    public static boolean mbShowSpecialChars = false;

    // Callback utilisé pour la réception des codes scannés
    public static String msCallbackHandleScan = "";

    // Interface pour executer les procedures WL
    // Cet objet doit être implémenté dans la collection de procedures WL
    public static IAppelProcedureWL mAppelProcedureWL = null;

    // Interface pour récupérer le contexte de l'application
    // Cet objet doit être implémenté dans la collection de procédures WL
    public static IContextRetriever mContextRetriever = null;

    // Membres initialiser un profil
    private static DWProfileSetConfigSettings dwProfileSetConfigSettings = null;

    // Membres modifier les paramètres du scanner
    private static DWProfileSwitchBarcodeParamsSettings dwProfileSwitchBarcodeParamsSettings = null;

    private static BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            _DWTraiteDonneeScannee(intent);
        }
    };

    private static boolean appelProcedureWL(Object... arguments)
    {
        if(mAppelProcedureWL != null)
            return mAppelProcedureWL.appelProcedureWL(arguments);
        return false;
    }

    private static Context getContexteApplication()
    {
        if(mContextRetriever != null)
        {
            return mContextRetriever.getContext();
        }
        return null;
    }

    private static boolean _DWTraiteDonneeScannee(Intent i)
    {
        // check the intent action is for us
        if ( i.getAction().contentEquals(mIntentAction) ) {
            // get the source of the data
            String source = i.getStringExtra(DataWedgeConstants.SOURCE_TAG);
            // save it to use later
            if (source == null) source = "scanner";
            // get the data from the intent
            String data = i.getStringExtra(DataWedgeConstants.DATA_STRING_TAG);

            // let's define a variable for the data length
            Integer data_len = 0;
            // and set it to the length of the data
            if (data != null)
                data_len = data.length();

            String sLabelTypeSymbology = null;
            String sSymbology = null;
            String sLabelType = null;

            // check if there is anything in the data
            if (data != null && data.length() > 0) {
                // we have some data, so let's get it's symbology
                sLabelTypeSymbology = i.getStringExtra(DataWedgeConstants.LABEL_TYPE_TAG);
                // check if the string is empty
                if (sLabelTypeSymbology != null && sLabelTypeSymbology.length() > 0) {
                    // format of the label type string is LABEL-TYPE-SYMBOLOGY
                    // so let's skip the LABEL-TYPE- portion to get just the symbology
                    sSymbology = sLabelTypeSymbology.substring(11);
                    sLabelType = sLabelTypeSymbology.substring(0,11);
                }
                else {
                    // the string was empty so let's set it to "Unknown"
                    sLabelType = "Unknown";
                    sSymbology = "Unknown";
                }

                if(mbShowSpecialChars == true){
                    String transformedString="";
                    char[] dataChar = data.toCharArray();
                    for(char acar : dataChar)
                    {
                        if(Character.isLetterOrDigit(acar))
                        {
                            transformedString += acar;
                        }
                        else
                        {
                            transformedString += "["+(int)acar+"]";
                        }
                    }
                    data = transformedString;
                }
                if(msCallbackHandleScan != "")
                {
                    appelProcedureWL(msCallbackHandleScan, data, sSymbology);
                }
                return true;
            }
        }
        return false;
    }

    private static void _DataWedgeInitialise(){
        _DWReinitialiseValeurs();
    }

    private static void _DWVerifierSiLeProfilExiste(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
	/*
	The profile checker will check if the profile already exists
	*/
        DWProfileChecker checker = new DWProfileChecker(getContexteApplication());

        // Setup profile checker parameters
        DWProfileCheckerSettings profileCheckerSettings = new DWProfileCheckerSettings()
        {{
            mProfileName = fsNomDuProfil;
            mTimeOutMS = flTimeoutMs;
        }};

        // Execute the checker with the given parameters
        checker.execute(profileCheckerSettings, new DWProfileChecker.onProfileExistResult() {
            @Override
            public void result(String profileName, boolean exists)
            {
                // exists == true means that the profile already... exists..
                if(exists){
                    _DWLogMessage("Profile " + profileName + " found in DW profiles list.");
                    _DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName, true);
                    }
                }
                else
                {
                    _DWLogMessage("Profile " + profileName + " not found in DW profiles list.");
                    _DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName, false);
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeOut(String profileName){
                String sErreur = "Timeout lors de la vérification si le profil " + profileName + "existe.";
                _DWLogMessage(sErreur);
                _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    appelProcedureWL(fsCallbackError, sErreur, "");
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private static void _DWLogMessage(String message)
    {
        Log.d(TAG, message);
    }

    private static void _DWCreerUnProfil(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWProfileCreate profileCreate = new DWProfileCreate(getContexteApplication());

        DWProfileCreateSettings profileCreateSettings = new DWProfileCreateSettings()
        {{
            mProfileName = fsNomDuProfil;
            mTimeOutMS = flTimeoutMs;
        }};

        profileCreate.execute(profileCreateSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    _DWLogMessage("Profile: " + profileName + " created with success.");
                    _DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName);
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de la création du profil: " + profileName;
                    _DWLogMessage(sErreur);
                    _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        appelProcedureWL(fsCallbackError, sErreur, resultInfo);
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de la création du profil: " + profileName;
                _DWLogMessage(sErreur);
                _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    appelProcedureWL(fsCallbackError, sErreur,"");
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private static void _DWEffacerUnProfil(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWProfileDelete deleteProfile = new DWProfileDelete(getContexteApplication());

        DWProfileDeleteSettings profileDeleteSettings = new DWProfileDeleteSettings()
        {{
            mProfileName = fsNomDuProfil;
            mTimeOutMS = flTimeoutMs;
        }};

        deleteProfile.execute(profileDeleteSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    _DWLogMessage("Profile: " + profileName + " deleted with success.");
                    _DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName);
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de l'effacement du profil: " + profileName;
                    _DWLogMessage(sErreur);
                    _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        appelProcedureWL(fsCallbackError, sErreur, resultInfo);
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de l'effacement du profil: " + profileName;
                _DWLogMessage(sErreur);
                _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    appelProcedureWL(fsCallbackError, sErreur, "");
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private static void _DWCreerConfigurationInitialiserProfil_Java(String profilAsJsonString)
    {
        dwProfileSetConfigSettings = DWProfileSetConfigSettings.fromJson(profilAsJsonString);

        // Some parameters are forced in the plugin (ex: disable keystroke plugin, enable intent output, etc...)
        // Specific for windev
        dwProfileSetConfigSettings.MainBundle.PACKAGE_NAME = getContexteApplication().getPackageName();
        dwProfileSetConfigSettings.IntentPlugin.intent_action = mIntentAction;
        dwProfileSetConfigSettings.IntentPlugin.intent_category = mIntentCategory;
        dwProfileSetConfigSettings.IntentPlugin.intent_output_enabled = true;
        dwProfileSetConfigSettings.KeystrokePlugin.keystroke_output_enabled = false;

        // Profile is ready for action !!! :D
    }

    private static void _DWInitialiserUnProfil(String sCallback, String sCallbackError)
    {
        final String fsCallbackSucces = sCallback;
        final String fsCallbackError = sCallbackError;

        DWProfileSetConfig profileSetConfig = new DWProfileSetConfig(getContexteApplication());

        profileSetConfig.execute(dwProfileSetConfigSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    _DWLogMessage("Profile: " + profileName + " intialised with success.");
                    _DWLogMessage("Trying to call procedure: " + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName);
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de l'initialisation du profil: " + profileName;
                    _DWLogMessage(sErreur);
                    _DWLogMessage("Trying to call procedure: " + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        appelProcedureWL(fsCallbackError, sErreur, resultInfo);
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de l'initialisation du profil: " + profileName;
                _DWLogMessage(sErreur);
                _DWLogMessage("Trying to call procedure: " + fsCallbackError);
                if(fsCallbackError != "")
                {
                    appelProcedureWL(fsCallbackError, sErreur, "");
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private static void _DWReinitialiseValeurs()
    {
        dwProfileSetConfigSettings = null;
        dwProfileSwitchBarcodeParamsSettings = null;
        mIntentAction = getContexteApplication().getPackageName() + ".RECVR";
    }

    private static void _DWEffacerCallbackDeScan()
    {
        getContexteApplication().unregisterReceiver(mMessageReceiver);
    }

    private static void _DWEnregistrerCallbackDeScan(String sCallbackHandleScan)
    {
        msCallbackHandleScan = sCallbackHandleScan;
        IntentFilter myFilter = new IntentFilter();
        myFilter.addAction(mIntentAction);
        myFilter.addCategory(mIntentCategory);
        getContexteApplication().registerReceiver(mMessageReceiver, myFilter);
    }

    private static void _DWDemarrerUnScan(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWScannerStartScan dwstartscan = new DWScannerStartScan(getContexteApplication());

        DWProfileBaseSettings settings = new DWProfileCreateSettings()
        {{
            mProfileName = fsNomDuProfil;
            mTimeOutMS = flTimeoutMs;
        }};

        dwstartscan.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    _DWLogMessage("Scan on profile: " + profileName + " started with success.");
                    _DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName);
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors du demarrage du scan pour le profil: " + profileName;
                    _DWLogMessage(sErreur);
                    _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        appelProcedureWL(fsCallbackError, sErreur, resultInfo);
                    }
                }
                _DWReinitialiseValeurs();
            }


            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors du demarrage du scan pour le profil: " + profileName;
                _DWLogMessage(sErreur);
                _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    appelProcedureWL(fsCallbackError, sErreur,"");
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private static void _DWStopperUnScan(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWScannerStopScan dwstopscan = new DWScannerStopScan(getContexteApplication());

        DWProfileBaseSettings settings = new DWProfileCreateSettings()
        {{
            mProfileName = fsNomDuProfil;
            mTimeOutMS = flTimeoutMs;
        }};

        dwstopscan.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    _DWLogMessage("Scan on profile: " + profileName + " stopped with success.");
                    _DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName);
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de la demande d'arrêt du scan pour le profil: " + profileName;
                    _DWLogMessage(sErreur);
                    _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        appelProcedureWL(fsCallbackError, sErreur, resultInfo);
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de la demande d'arrêt du scan pour le profil: " + profileName;
                _DWLogMessage(sErreur);
                _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    appelProcedureWL(fsCallbackError, sErreur, "");
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private static void _DWModifierLesParametresDuScanner_Java(String sCallback, String sCallbackError)
    {
        final String fsCallbackSucces = sCallback;
        final String fsCallbackError = sCallbackError;

        DWProfileSwitchBarcodeParams switchContinuousMode = new DWProfileSwitchBarcodeParams(getContexteApplication());
        // TO UPDATE
        switchContinuousMode.execute(dwProfileSwitchBarcodeParamsSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    _DWLogMessage("Scanner params on profile: " + profileName + " modification succeeded.");
                    _DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName);
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de la modificationd des paramètres du scanner du profil: " + profileName;
                    _DWLogMessage(sErreur);
                    _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        appelProcedureWL(fsCallbackError, sErreur, resultInfo);
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de la modificationd des paramètres du scanner du profil: " + profileName;
                _DWLogMessage(sErreur);
                _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    appelProcedureWL(fsCallbackError, sErreur, "");
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private static void _DWCreerConfigurationModifierLesParametresDuScanner_Java(String settingsAsJSONString)
    {
        dwProfileSwitchBarcodeParamsSettings = DWProfileSwitchBarcodeParamsSettings.fromJson(settingsAsJSONString);
    }


    private static void _DWActiverDataWedge(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWScannerPluginEnable dwpluginenable = new DWScannerPluginEnable(getContexteApplication());

        DWProfileBaseSettings settings = new DWProfileCreateSettings()
        {{
            mProfileName = fsNomDuProfil;
            mTimeOutMS = flTimeoutMs;
        }};

        dwpluginenable.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    _DWLogMessage("Plugin on profile: " + profileName + " enabled with success.");
                    _DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName);
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de l'activation de DataWedge pour le profil: " + profileName;
                    _DWLogMessage(sErreur);
                    _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        appelProcedureWL(fsCallbackError, sErreur, resultInfo);
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de l'activation de DataWedge pour le profil: " + profileName;
                _DWLogMessage(sErreur);
                _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    appelProcedureWL(fsCallbackError, sErreur,"");
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private static void _DWDesactiverDataWedge(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWScannerPluginDisable dwplugindisable = new DWScannerPluginDisable(getContexteApplication());

        DWProfileBaseSettings settings = new DWProfileCreateSettings()
        {{
            mProfileName = fsNomDuProfil;
            mTimeOutMS = flTimeoutMs;
        }};

        dwplugindisable.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    _DWLogMessage("Plugin on profile: " + profileName + " disabled with success.");
                    _DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        appelProcedureWL(fsCallbackSucces, profileName);
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de la desactivation de DataWedge pour le profil: " + profileName;
                    _DWLogMessage(sErreur);
                    _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        appelProcedureWL(fsCallbackError, sErreur, resultInfo);
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de la desactivation de DataWedge pour le profil: " + profileName;
                _DWLogMessage(sErreur);
                _DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    appelProcedureWL(fsCallbackError, sErreur, "");
                }
                _DWReinitialiseValeurs();
            }
        });
    }
}
