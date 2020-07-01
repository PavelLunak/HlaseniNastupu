package cz.stodva.hlaseninastupu.utils;

public interface AppConstants {

    String LOG_TAG = "log_tag";
    String LOG_TAG_SMS = "log_tag_sms";

    String MESSAGE_STRAT = "NASTUP";
    String MESSAGE_END = "KONEC";
    String PHONE_NUMBER = "+420736370433";

    int ALARM_REQUEST_CODE_START = 12345;
    int ALARM_REQUEST_CODE_END = 54321;
    int ALARM_REQUEST_ERROR = 64321;

    int SENT_REQUEST_START = 12346;
    int SENT_REQUEST_END = 54322;

    int DELIVERED_REQUEST_START = 12347;
    int DELIVERED_REQUEST_END = 54323;

    String FRAGMENT_MAIN_NAME = "FragmentMain";
    String FRAGMENT_TIMER_NAME = "FragmentTimer";
    String FRAGMENT_SETTINGS_NAME = "FragmentSettings";

    String FRAGMENT_TIMER = "Automat";
    String FRAGMENT_SETTINGS = "Nastavení";

    int MESSAGE_TYPE_START = 1;
    int MESSAGE_TYPE_END = 2;
    int MESSAGE_TYPE_BOTH = 3;

    int REPORT_TYPE_NEXT = 1;
    int REPORT_TYPE_LAST = 2;

    int ALARM_TYPE_NO_SENT = 1;
    int ALARM_TYPE_NO_DELIVERED = 2;

    int ERROR_TYPE_NO_SENT = 1;
    int ERROR_TYPE_NO_DELIVERED = 2;

    int SMS_PERMISSION_REQUEST = 1;

    int PREFS_DELETE_KEY = -123;    //Příznak, že danou hodnotu v prefs chci odstranit

    String ACTION_SMS_SENT = "action_sms_sent";
    String ACTION_SMS_DELIVERED = "action_sms_delivered";
}
