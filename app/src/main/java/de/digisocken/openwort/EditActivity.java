package de.digisocken.openwort;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class EditActivity extends Activity {
    private Button sendButton;
    private EditText editMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_activity);
        sendButton = (Button) findViewById(R.id.btnOk);
        editMsg = (EditText) findViewById(R.id.editMsg);
        Intent intent = getIntent();
        if (intent!=null) {
            editMsg.setText(intent.getStringExtra("msg"));
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}