package com.example.classroomwithfacedetection.UI.LOGIN;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;


import com.example.classroomwithfacedetection.R;
import com.example.classroomwithfacedetection.UI.HOME.Home;
import com.example.classroomwithfacedetection.UI.ResetPassWord.ResetPassWord;
import com.example.classroomwithfacedetection.UI.SIGNUP.Signup;
import com.example.classroomwithfacedetection.Untils.Constants;
import com.example.classroomwithfacedetection.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;

public class Login extends AppCompatActivity {

    private ActivityLoginBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.edtTaikhoan.getText().toString().trim();
                String pass = binding.edtPassword.getText().toString().trim();
                if (Constants.validate(email)){
                    Constants.AUTH.signInWithEmailAndPassword(email,pass)
                            .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                @Override
                                public void onSuccess(AuthResult authResult) {
                                    Toast.makeText(Login.this, getString(R.string.successful), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(Login.this, Home.class));
                                    finishAffinity();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }else {
                    binding.edtTaikhoan.setError(getString(R.string.email_fomat_error));
                }
            }
        });
        binding.btSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this, Signup.class));
            }
        });
        binding.forgetPass.setOnClickListener(v -> {
            startActivity(new Intent(this, ResetPassWord.class));
        });

    }

}