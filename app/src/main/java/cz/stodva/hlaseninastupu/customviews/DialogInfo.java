package cz.stodva.hlaseninastupu.customviews;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import cz.stodva.hlaseninastupu.R;


public class DialogInfo extends Dialog {

    private String title, message;

    private OnDialogClosedListener listener;

    @ColorRes
    private int colorHeader;
    private int colorMessage;

    public DialogInfo(Context context) {
        super(context);
    }

    public DialogInfo setTitle(String title) {
        this.title = title;
        return this;
    }

    public DialogInfo setMessage(String message) {
        this.message = message;
        return this;
    }

    public DialogInfo setListener(OnDialogClosedListener listener) {
        this.listener = listener;
        return this;
    }

    public DialogInfo setHeaderColor(@ColorRes int color) {
        colorHeader = color;
        return this;
    }

    public DialogInfo setMessageColor(@ColorRes int color) {
        colorMessage = color;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_info);

        ((TextView) findViewById(R.id.label_title)).setText(title);
        ((TextView) findViewById(R.id.label_message)).setText(message);
        ((TextView) findViewById(R.id.label_message)).setTextColor(colorMessage);

        findViewById(R.id.layout_head).setBackgroundResource(colorHeader);
        findViewById(R.id.btnClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener != null) {
                    listener.onDialogClosed();
                }
            }
        });
    }

    public static DialogInfo createDialog(Context context) {
        return new DialogInfo(context)
                .setHeaderColor(R.color.colorPrimary)
                .setMessageColor(R.color.colorPrimary);
    }

    public interface OnDialogClosedListener {
        public void onDialogClosed();
    }
}
