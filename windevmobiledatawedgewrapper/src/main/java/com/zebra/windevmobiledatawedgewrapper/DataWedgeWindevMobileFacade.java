package com.zebra.windevmobiledatawedgewrapper;

// Imports
import com.zebra.datawedgeprofileintents.*;

import android.app.Activity;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


public class DataWedgeWindevMobileFacade {

    public interface IAppelProcedureWL
    {
        void appelProcedureWLSS(String param1, String param2);
        void appelProcedureWLSSB(String param1, String param2, boolean param3);
        void appelProcedureWLSSS(String param1, String param2, String param3);
        void appelProcedureWLSSSS(String param1, String param2, String param3, String param4);
    }

    public interface IActivityRetriever
    {
        Activity getActivity();
    }

    // Membres
    private String TAG = "DataWedgeWindevMobileFacade";

    // Paramètres d'intent pour la réception des codes scannés
    private String mIntentAction = "com.symbol.windevdatawedgedemo.RECVR";
    private String mIntentCategory = "android.intent.category.DEFAULT";

    public boolean mbShowSpecialChars = false;

    // Callback utilisé pour la réception des codes scannés
    private String msCallbackHandleScan = "";

    // Interface pour executer les procedures WL
    // Cet objet doit être implémenté dans la collection de procedures WL
    private IAppelProcedureWL mAppelProcedureWL = null;

    // Interface pour récupérer l'activité courante de l'application
    // Cet objet doit être implémenté dans la collection de procédures WL
    private IActivityRetriever mActivityRetriever = null;

    // Membres initialiser un profil
    private DWProfileSetConfigSettings dwProfileSetConfigSettings = null;

    // Membres modifier les paramètres du scanner
    private DWProfileSwitchBarcodeParamsSettings dwProfileSwitchBarcodeParamsSettings = null;

    private BroadcastReceiver mMessageReceiver = null;
    // On doit garder l'activité qui a permis d'enregistrer le receiver

    private BroadcastReceiver mMessageReceiverActivity = null;

    public DataWedgeWindevMobileFacade(IAppelProcedureWL aAppelProcedureWLInterface, IActivityRetriever aActivityRetrieverInterface)
    {
        mAppelProcedureWL = aAppelProcedureWLInterface;
        mActivityRetriever = aActivityRetrieverInterface;
    }

    private Activity getActivity()
    {
        if(mActivityRetriever != null)
        {
            return mActivityRetriever.getActivity();
        }
        return null;
    }

    private boolean _DWTraiteDonneeScannee(Intent i)
    {
        // check the intent action is for us
        if ( i.getAction().contentEquals(mIntentAction) && msCallbackHandleScan != "" && mAppelProcedureWL != null) {
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

                DWLogMessage("Scan: " + data + " " + sSymbology);
                mAppelProcedureWL.appelProcedureWLSSS(msCallbackHandleScan, data, sSymbology);

                return true;
            }
        }
        return false;
    }

    public void DataWedgeInitialise(){
        _DWReinitialiseValeurs();
    }

    public void DWVerifierSiLeProfilExiste(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
	/*
	The profile checker will check if the profile already exists
	*/
        DWProfileChecker checker = new DWProfileChecker(getActivity());

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
                    DWLogMessage("Profile " + profileName + (exists == false ? " not" : "") + " found in DW profiles list.");
                    DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSB(fsCallbackSucces, profileName, exists);
                        }
                    }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeOut(String profileName){
                String sErreur = "Timeout lors de la vérification si le profil " + profileName + "existe.";
                DWLogMessage(sErreur);
                DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, "");
                    }
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private void DWLogMessage(String message)
    {
        Log.d(TAG, message);
    }

    public void DWCreerUnProfil(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWProfileCreate profileCreate = new DWProfileCreate(getActivity());

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
                    DWLogMessage("Profile: " + profileName + " created with success.");
                    DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, profileName);
                        }
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de la création du profil: " + profileName;
                    DWLogMessage(sErreur);
                    DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, resultInfo);
                        }
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de la création du profil: " + profileName;
                DWLogMessage(sErreur);
                DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, "");
                    }
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    public void DWEffacerUnProfil(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWProfileDelete deleteProfile = new DWProfileDelete(getActivity());

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
                    DWLogMessage("Profile: " + profileName + " deleted with success.");
                    DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, profileName);
                        }
                        //appelProcedureWL(fsCallbackSucces, profileName);
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de l'effacement du profil: " + profileName;
                    DWLogMessage(sErreur);
                    DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, resultInfo);
                        }
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de l'effacement du profil: " + profileName;
                DWLogMessage(sErreur);
                DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, "");
                    }
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    public void DWCreerConfigurationInitialiserProfil_Java(String profilAsJsonString)
    {
        dwProfileSetConfigSettings = DWProfileSetConfigSettings.fromJson(profilAsJsonString);

        // Some parameters are forced in the plugin (ex: disable keystroke plugin, enable intent output, etc...)
        // Specific for windev
        dwProfileSetConfigSettings.MainBundle.PACKAGE_NAME = getActivity().getPackageName();
        dwProfileSetConfigSettings.IntentPlugin.intent_action = mIntentAction;
        dwProfileSetConfigSettings.IntentPlugin.intent_category = mIntentCategory;
        dwProfileSetConfigSettings.IntentPlugin.intent_output_enabled = true;
        dwProfileSetConfigSettings.KeystrokePlugin.keystroke_output_enabled = false;

        // Profile is ready for action !!! :D
    }

    public void DWInitialiserUnProfil(String sCallback, String sCallbackError)
    {
        final String fsCallbackSucces = sCallback;
        final String fsCallbackError = sCallbackError;

        DWProfileSetConfig profileSetConfig = new DWProfileSetConfig(getActivity());

        profileSetConfig.execute(dwProfileSetConfigSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    DWLogMessage("Profile: " + profileName + " intialised with success.");
                    DWLogMessage("Trying to call procedure: " + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, profileName);
                        }
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de l'initialisation du profil: " + profileName;
                    DWLogMessage(sErreur);
                    DWLogMessage("Trying to call procedure: " + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, resultInfo);
                        }
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de l'initialisation du profil: " + profileName;
                DWLogMessage(sErreur);
                DWLogMessage("Trying to call procedure: " + fsCallbackError);
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, "");
                    }
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    private void _DWReinitialiseValeurs()
    {
        dwProfileSetConfigSettings = null;
        dwProfileSwitchBarcodeParamsSettings = null;
    }

    public void DWEffacerCallbackDeScan(boolean effacerReceiver, final String fsCallbackSucces, final String fsCallbackError)
    {
        String tempCallbackHandleScanString = msCallbackHandleScan;
        // Remove reference to the Windev callback called when a scan occurs
        msCallbackHandleScan = "";
        Log.d(TAG, "Removing scan callback: " + tempCallbackHandleScanString + " succeeded");

        if(effacerReceiver && mMessageReceiver != null) {
            mIntentAction = "";
            try {
                getActivity().getApplicationContext().unregisterReceiver(mMessageReceiver);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSS(fsCallbackError, tempCallbackHandleScanString, e.getMessage());
                    }
                }
                return;
            }
            mMessageReceiver = null;
            Log.d(TAG, "Removing and unregistering message receiver:" + mIntentAction + "succeeded");
        }
        if(fsCallbackSucces != "")
        {
            if(mAppelProcedureWL != null) {

                mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, tempCallbackHandleScanString);
            }
        }
    }

    public void DWEnregistrerCallbackDeScan(String sCallbackHandleScan, final String fsCallbackSucces, final String fsCallbackError)
    {
        msCallbackHandleScan = sCallbackHandleScan;
        Log.d(TAG, "Registering scan callback: " + sCallbackHandleScan + " succedeed");

        if(mMessageReceiver == null) {
            mIntentAction = getActivity().getPackageName() + ".RECVR";
            try {
                mMessageReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        _DWTraiteDonneeScannee(intent);
                    }
                };
                IntentFilter myFilter = new IntentFilter();
                myFilter.addAction(mIntentAction);
                myFilter.addCategory(mIntentCategory);
                getActivity().getApplicationContext().registerReceiver(mMessageReceiver, myFilter);
            }
            catch(Exception e)
            {
                Log.d(TAG, e.getMessage());
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSS(fsCallbackError,msCallbackHandleScan, e.getMessage());
                    }
                }
                return;
            }
            Log.d(TAG, "Creating and registering message receiver: " + mIntentAction + " succedeed");
        }
        if(fsCallbackSucces != "")
        {
            if(mAppelProcedureWL != null) {

                mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, msCallbackHandleScan);
            }
        }
    }

    public void DWDemarrerUnScan(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWScannerStartScan dwstartscan = new DWScannerStartScan(getActivity());

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
                    DWLogMessage("Scan on profile: " + profileName + " started with success.");
                    DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, profileName);
                        }
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors du demarrage du scan pour le profil: " + profileName;
                    DWLogMessage(sErreur);
                    DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, resultInfo);
                        }
                    }
                }
                _DWReinitialiseValeurs();
            }


            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors du demarrage du scan pour le profil: " + profileName;
                DWLogMessage(sErreur);
                DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, "");
                    }
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    public void DWStopperUnScan(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWScannerStopScan dwstopscan = new DWScannerStopScan(getActivity());

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
                    DWLogMessage("Scan on profile: " + profileName + " stopped with success.");
                    DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, profileName);
                        }
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de la demande d'arrêt du scan pour le profil: " + profileName;
                    DWLogMessage(sErreur);
                    DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, resultInfo);
                        }
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de la demande d'arrêt du scan pour le profil: " + profileName;
                DWLogMessage(sErreur);
                DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, "");
                    }
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    public void DWModifierLesParametresDuScanner_Java(String sCallback, String sCallbackError)
    {
        final String fsCallbackSucces = sCallback;
        final String fsCallbackError = sCallbackError;

        DWProfileSwitchBarcodeParams switchContinuousMode = new DWProfileSwitchBarcodeParams(getActivity());
        // TO UPDATE
        switchContinuousMode.execute(dwProfileSwitchBarcodeParamsSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier)
            {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    DWLogMessage("Scanner params on profile: " + profileName + " modification succeeded.");
                    DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, profileName);
                        }
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de la modificationd des paramètres du scanner du profil: " + profileName;
                    DWLogMessage(sErreur);
                    DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, resultInfo);
                        }
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de la modificationd des paramètres du scanner du profil: " + profileName;
                DWLogMessage(sErreur);
                DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, "");
                    }
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    public void DWCreerConfigurationModifierLesParametresDuScanner_Java(String settingsAsJSONString)
    {
        dwProfileSwitchBarcodeParamsSettings = DWProfileSwitchBarcodeParamsSettings.fromJson(settingsAsJSONString);
    }


    public void DWActiverDataWedge(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWScannerPluginEnable dwpluginenable = new DWScannerPluginEnable(getActivity());

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
                    DWLogMessage("Plugin on profile: " + profileName + " enabled with success.");
                    DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, profileName);
                        }
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de l'activation de DataWedge pour le profil: " + profileName;
                    DWLogMessage(sErreur);
                    DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, resultInfo);
                        }
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de l'activation de DataWedge pour le profil: " + profileName;
                DWLogMessage(sErreur);
                DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, "");
                    }
                }
                _DWReinitialiseValeurs();
            }
        });
    }

    public void DWDesactiverDataWedge(final String fsNomDuProfil, final long flTimeoutMs, final String fsCallbackSucces, final String fsCallbackError)
    {
        DWScannerPluginDisable dwplugindisable = new DWScannerPluginDisable(getActivity());

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
                    DWLogMessage("Plugin on profile: " + profileName + " disabled with success.");
                    DWLogMessage("Trying to call procedure:" + fsCallbackSucces);
                    if(fsCallbackSucces != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSS(fsCallbackSucces, profileName);
                        }
                    }
                }
                else
                {
                    String sErreur = "Une erreur s'est produite lors de la desactivation de DataWedge pour le profil: " + profileName;
                    DWLogMessage(sErreur);
                    DWLogMessage("Trying to call procedure:" + fsCallbackError);
                    if(fsCallbackError != "")
                    {
                        if(mAppelProcedureWL != null) {

                            mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, resultInfo);
                        }
                    }
                }
                _DWReinitialiseValeurs();
            }

            @Override
            public void timeout(String profileName) {
                String sErreur = "Timeout lors de la desactivation de DataWedge pour le profil: " + profileName;
                DWLogMessage(sErreur);
                DWLogMessage("Trying to call procedure:" + fsCallbackError);
                if(fsCallbackError != "")
                {
                    if(mAppelProcedureWL != null) {

                        mAppelProcedureWL.appelProcedureWLSSSS(fsCallbackError, profileName, sErreur, "");
                    }
                }
                _DWReinitialiseValeurs();
            }
        });
    }
}
