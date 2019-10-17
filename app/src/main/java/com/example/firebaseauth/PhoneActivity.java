package com.example.firebaseauth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class PhoneActivity extends AppCompatActivity {

    private EditText editTextMobile,editTextMobileCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        editTextMobile = findViewById(R.id.editTextMobile);
        editTextMobileCode = findViewById(R.id.editTextMobileCode);

        findViewById(R.id.buttonContinue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String mobile = editTextMobile.getText().toString().trim();
                String code = editTextMobileCode.getText().toString().trim();

                if(mobile.isEmpty() || mobile.length() < 6){
                    editTextMobile.setError("Enter a valid mobile");
                    editTextMobile.requestFocus();
                    return;
                }

                Intent intent = new Intent(PhoneActivity.this, VerifyPhoneActivity.class);
                intent.putExtra("mobile", mobile);
                intent.putExtra("code", code);
                startActivity(intent);
            }
        });

    }
}
