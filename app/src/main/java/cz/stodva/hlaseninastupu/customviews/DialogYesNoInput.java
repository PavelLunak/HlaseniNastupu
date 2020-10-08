package cz.stodva.hlaseninastupu.customviews;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.listeners.YesNoInputSelectedListener;


public class DialogYesNoInput extends Dialog {

    private String title, message, desc;
    private YesNoInputSelectedListener listener;

    @ColorRes
    private int colorHeader;
    private int colorMessage;

    public DialogYesNoInput(Context context) {
        super(context);
    }

    public DialogYesNoInput setTitle(String title) {
        this.title = title;
        return this;
    }

    public DialogYesNoInput setMessage(String message) {
        this.message = message;
        return this;
    }

    public DialogYesNoInput setDesc(String desc) {
        this.desc = desc;
        return this;
    }

    public DialogYesNoInput setListener(YesNoInputSelectedListener listener) {
        this.listener = listener;
        return this;
    }

    public DialogYesNoInput setHeaderColor(@ColorRes int color) {
        colorHeader = color;
        return this;
    }

    public DialogYesNoInput setMessageColor(@ColorRes int color) {
        colorMessage = color;
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_yes_no_input);

        ((TextView) findViewById(R.id.label_title)).setText(title);
        ((TextView) findViewById(R.id.label_message)).setText(message);
        ((TextView) findViewById(R.id.label_message)).setTextColor(colorMessage);

        findViewById(R.id.layout_head).setBackgroundResource(colorHeader);
        findViewById(R.id.btnNo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.noSelected(((TextView) findViewById(R.id.etDesc)).getText().toString());
                dismiss();
            }
        });

        findViewById(R.id.btnYes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.yesSelected(((TextView) findViewById(R.id.etDesc)).getText().toString());
                dismiss();
            }
        });
    }

    public void updateMessage(String newMsg) {
        this.message = newMsg;
        ((TextView) findViewById(R.id.label_message)).setText(newMsg);
    }

    public static DialogYesNoInput createDialog(Context context) {
        return new DialogYesNoInput(context)
                .setHeaderColor(R.color.colorPrimary)
                .setMessageColor(R.color.colorPrimary);
    }
}
