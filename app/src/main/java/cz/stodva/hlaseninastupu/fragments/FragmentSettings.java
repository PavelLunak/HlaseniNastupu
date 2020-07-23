package cz.stodva.hlaseninastupu.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.customviews.DialogYesNo;
import cz.stodva.hlaseninastupu.listeners.YesNoSelectedListener;
import cz.stodva.hlaseninastupu.utils.Animators;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;

public class FragmentSettings extends Fragment implements AppConstants {

    EditText etSap, etPhone, etMessageStart, etMessageEnd;
    TextView labelInvalidPhone, labelWarning, labelVersion, labelContactName;
    ImageView imgPerson;
    Button btnSave, btnCancel;

    MainActivity activity;

    String sap = "";
    String phone = "";
    String startMsg = "";
    String endMsg = "";
    String contactName = "";

    boolean backPressed;

    TextWatcher textWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            labelContactName.setText("");
            labelContactName.setVisibility(View.GONE);
            checkData();
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof MainActivity) activity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        etSap = view.findViewById(R.id.etSap);
        etPhone = view.findViewById(R.id.etPhone);
        etMessageStart = view.findViewById(R.id.etMessageStart);
        etMessageEnd = view.findViewById(R.id.etMessageEnd);

        labelInvalidPhone = view.findViewById(R.id.labelInvalidPhone);
        labelWarning = view.findViewById(R.id.labelWarning);
        labelVersion = view.findViewById(R.id.labelVersion);
        labelContactName = view.findViewById(R.id.labelContactName);

        imgPerson = view.findViewById(R.id.imgPerson);

        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity.getAppSettings();

        sap = activity.getAppSettings().getSap();
        phone = activity.getAppSettings().getPhoneNumber();
        startMsg = activity.getAppSettings().getStartMessage();
        endMsg = activity.getAppSettings().getEndMessage();
        contactName = activity.getAppSettings().getContactName();

        etSap.setText(sap);
        etPhone.setText(phone);
        etMessageStart.setText(startMsg);
        etMessageEnd.setText(endMsg);

        etSap.addTextChangedListener(textWatcher);
        etPhone.addTextChangedListener(textWatcher);
        etMessageStart.addTextChangedListener(textWatcher);
        etMessageEnd.addTextChangedListener(textWatcher);

        labelVersion.setText("Verze: " + activity.getAppVersion());

        if (contactName.equals("")) {
            labelContactName.setVisibility(View.GONE);
        } else {
            labelContactName.setVisibility(View.VISIBLE);
            labelContactName.setText(contactName);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
                activity.onBackPressed();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hasUpdatesAfterRequestClose();
                backPressed = true;
            }
        });

        imgPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(imgPerson, true);
                if (activity.checkReadContactsPermissionGranted()) {
                    activity.selectContact();
                }
            }
        });

        checkData();
    }

    private void checkData() {
        etPhone.setHint(PHONE_NUMBER);

        if (!sap.equals("")) {
            etMessageStart.setHint(MESSAGE_STRAT + " " + sap);
            etMessageEnd.setHint(MESSAGE_END + " " + sap);
        } else {
            if (etMessageStart.getHint().toString().contains("NASTUP")) etMessageStart.setHint("Musí být vyplněno");
            if (etMessageEnd.getHint().toString().contains("KONEC")) etMessageEnd.setHint("Musí být vyplněno");
        }

        if ((startMsg.equals("") || endMsg.equals("")) && sap.equals("")) labelWarning.setVisibility(View.VISIBLE);
        else labelWarning.setVisibility(View.GONE);

        if (validatePhoneNumber(etPhone.getText())) {
            labelInvalidPhone.setVisibility(View.GONE);
        } else {
            labelInvalidPhone.setVisibility(View.VISIBLE);
        }
    }

    public void onBackPressed() {
        backPressed = true;
        hasUpdatesAfterRequestClose();
    }

    public boolean hasUpdatesAfterRequestClose() {

        Log.d("safawc", "hasUpdatesAfterRequestClose()");

        Log.d("safawc", "etSap: " + etSap.getText().toString());
        Log.d("safawc", "etPhone: " + etPhone.getText().toString());
        Log.d("safawc", "etMessageStart: " + etMessageStart.getText().toString());
        Log.d("safawc", "etMessageEnd: " + etMessageEnd.getText().toString());

        Log.d("safawc", "sap: " + sap);
        Log.d("safawc", "phone: " + phone);
        Log.d("safawc", "startMsg: " + startMsg);
        Log.d("safawc", "endMsg: " + endMsg);

        boolean result = false;

        if (!checkInput(etSap).equals(sap.trim())) result = true;
        if (!checkInput(etPhone).equals(phone.trim())) result = true;
        if (!checkInput(etMessageStart).equals(startMsg.trim())) result = true;
        if (!checkInput(etMessageEnd).equals(endMsg.trim())) result = true;

        if (result) {
            DialogYesNo.createDialog(activity)
                    .setTitle("Upozornění")
                    .setMessage("Uložit provedené změny?")
                    .setListener(new YesNoSelectedListener() {
                        @Override
                        public void yesSelected() {
                            saveData();
                            activity.onBackPressed();
                        }

                        @Override public void noSelected() {
                            activity.onBackPressed();
                        }
                    }).show();
        } else {
            activity.onBackPressed();
        }

        return result;
    }

    private String checkInput(EditText et) {
        if (et == null) return "";
        if (et.getText() == null) return "";

        return et.getText().toString().trim();
    }

    public void saveData() {
        sap = checkInput(etSap);
        phone = checkInput(etPhone);
        startMsg = checkInput(etMessageStart);
        endMsg = checkInput(etMessageEnd);

        if (labelContactName.getText() != null) {
            if (labelContactName.getText().toString() != null) {
                contactName = labelContactName.getText().toString();
                if (contactName.equals("")) contactName = null;
            } else {
                contactName = "";
            }
        } else {
            contactName = "";
        }

        if (phone.equals("")) {
            phone = PHONE_NUMBER;
        } else {
            if (!validatePhoneNumber(phone)) {
                phone = "";
            }
        }

        if (!sap.equals("") && startMsg.equals("")) startMsg = MESSAGE_STRAT + " " + sap;
        if (!sap.equals("") && endMsg.equals("")) endMsg = MESSAGE_END + " " + sap;

        PrefsUtils.saveAppSettings(
                activity,
                sap,
                phone,
                contactName,
                startMsg,
                endMsg);

        activity.updateAppSettings(activity);
    }

    private boolean validatePhoneNumber(Object object) {
        if (object == null) return true;
        if (object.toString() == null) return true;
        if (object.toString().isEmpty()) return true;

        String inputPhone = "";

        if (object instanceof Editable) {
            inputPhone = object.toString();
        } else if (object instanceof String) {
            inputPhone = (String) object;
        } else {
            return true;
        }

        String pattern = "^((\\+|00){1}\\d{3})?( |-)?[1-9][0-9]{2}( |-)?[0-9]{3}( |-)?[0-9]{3}$";

        return inputPhone.trim().matches(pattern);

        /*
        ((\+|00){1}\d{3})? Jednou "+" nebo "00" a k tomu tři čísla. A celé tam být může a nemusí - ()?
        ( |-)? může a nemusí být mezera nebo pomlčka
        [1-9][0-9]{2} jedno číslo 1-9 a k tomu dvě čísla 0-9
        ( |-)? může a nemusí být mezera nebo pomlčka
        [0-9]{3} tři čísla 0-9
        ( |-)? může a nemusí být mezera nebo pomlčka
        [0-9]{3} tři čísla 0-9
        $ na konci řetězce

        PŘÍKLADY SPRÁVNÝCH FORMÁTŮ:
        123456789
        123 456 789, 123456 789, 123 456789
        123-456-789, 123-456789, 123456-789, 123-456 789, 123 456-789
        +420123456789
        +420-123-456-789, +420 123 456 789, +420-123-456 789
        00420123456789, 00420-123-456-789, 00420 123 456 789, 00420-123 456-789
        */
    }

    public void setContact(String phone, String name) {
        etPhone.setText(phone);
        labelContactName.setVisibility(View.VISIBLE);
        labelContactName.setText(name);
    }

    public boolean isBackPressed() {
        return backPressed;
    }
}
