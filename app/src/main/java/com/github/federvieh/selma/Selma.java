
package com.github.federvieh.selma;

import android.app.Application;
import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * Created by frank on 7/12/15.
 */
@ReportsCrashes(
        mailTo = "frank.oltmanns+selmaAcra@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        customReportContent = {
                ReportField.USER_COMMENT,
                ReportField.ANDROID_VERSION,
                ReportField.APP_VERSION_NAME,
                ReportField.BRAND,
                ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.LOGCAT
        })
public class Selma extends Application{
    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
