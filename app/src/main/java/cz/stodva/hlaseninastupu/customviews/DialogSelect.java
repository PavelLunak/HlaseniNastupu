package cz.stodva.hlaseninastupu.customviews;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import cz.stodva.hlaseninastupu.R;

public class DialogSelect extends Dialog {

    LinearLayout layoutItems;
    TextView labelTitle, labelMessage, btnClose;

    Context context;
    private String title, message;
    private String[] items;
    OnDialogSelectItemSelectedListener listener;

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (listener != null) listener.onDialogSelectItemSelected(((TextView) v).getText().toString());
            dismiss();
        }
    };

    public DialogSelect(Context context) {
        super(context);
        this.context = context;
    }

    public static DialogSelect createDialog(Context context) {
        return new DialogSelect(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_select);

        labelTitle = findViewById(R.id.labelTitle);
        labelMessage = findViewById(R.id.labelMessage);
        layoutItems = findViewById(R.id.layoutItems);
        btnClose = findViewById(R.id.btnClose);

        labelTitle.setText(title);
        labelMessage.setText(message);

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        addItems();

        if(message == null) labelMessage.setVisibility(View.GONE);
    }

    private void addItems() {
        if (items == null) return;
        TextView newLabel;

        for (int i = 0; i < items.length; i ++) {
            newLabel = new TextView(context);
            newLabel.setText(items[i]);
            newLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            newLabel.setPadding(15, 15, 15, 15);
            newLabel.setTextColor(Color.BLACK/*context.getResources().getColor(R.color.colorPrimary)*/);
            newLabel.setAllCaps(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 10, 0, 10);
            newLabel.setLayoutParams(lp);
            newLabel.setGravity(Gravity.CENTER_HORIZONTAL);
            newLabel.setBackgroundResource(R.drawable.bg_dialog_select_item);
            newLabel.setOnClickListener(clickListener);

            layoutItems.addView(newLabel);
        }
    }

    public DialogSelect setTitle(String title) {
        this.title = title;
        return this;
    }

    public DialogSelect setMessage(String message) {
        this.message = message;
        return this;
    }

    public DialogSelect setItems(String[] items) {
        this.items = items;
        return this;
    }

    public DialogSelect setListener(OnDialogSelectItemSelectedListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnDialogSelectItemSelectedListener {
        void onDialogSelectItemSelected(String selectedItem);
    }
}
