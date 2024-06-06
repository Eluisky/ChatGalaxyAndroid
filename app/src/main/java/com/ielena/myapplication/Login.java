package com.ielena.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.sql.SQLException;

public class Login extends AppCompatActivity {

    CheckBox rememberMe;
    EditText emailOrPhoneNumberOrUsername;
    EditText loginPassword;
    static boolean logged = false;
    static String username;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Colorear ActionBar
        getSupportActionBar().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background));
        setContentView(R.layout.login_view);


        Button signIn = findViewById(R.id.signIn);
        Button signUp = findViewById(R.id.register);
        emailOrPhoneNumberOrUsername = findViewById(R.id.emailOrPhoneNumberOrUsername);
        loginPassword = findViewById(R.id.password);
        rememberMe = findViewById(R.id.rememberMe);

        signIn.setOnClickListener(v -> tryToLogin());
        signUp.setOnClickListener(v -> launchRegister());

        //Comprobar si ha marcado la casilla de recordarme
        Mediator.remember(emailOrPhoneNumberOrUsername,loginPassword,rememberMe,this);
    }

    public void tryToLogin(){
        Toast toast = Mediator.tryToLogin(emailOrPhoneNumberOrUsername,loginPassword,rememberMe,this);
        //Muestra el mensaje correspondiente al usuario
        toast.show();
        //Si se ha iniciado sesión correctamente
        if (logged){
            //Preparar al usuario
            try {
                Intent intent = new Intent();
                intent = Mediator.prepareUser(emailOrPhoneNumberOrUsername,loginPassword, this);
                if (intent != null) {
                    startActivity(intent);
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void launchRegister(){
        Intent intent = Mediator.launchRegister(this);
        startActivity(intent);
    }
}