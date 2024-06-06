package com.ielena.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class Register extends AppCompatActivity {

   static boolean registred = false;
   static int defaultImage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_view);
        //Colorear ActionBar
        getSupportActionBar().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.background));
        Button register = findViewById(R.id.register);
        //Pasamos el context al mediador para que segun en que ventana se esté, tenga su ventana de cargando
        Mediator.context = this;
        //Guardar la imagen por defecto
        defaultImage = getApplicationContext().getResources().getIdentifier("emptyuser","drawable",getApplicationContext().getPackageName());
        //Registrar cuando se pulse el botón de registro
        register.setOnClickListener(v-> {
            register();
        });
    }

    public void register(){
        EditText username = findViewById(R.id.username);
        EditText mail = findViewById(R.id.mail);
        EditText telephone = findViewById(R.id.telephone);
        EditText password = findViewById(R.id.password);
        //Registrar a un usuario comprobando los parametros pasados
        try {
            Toast toast = Mediator.register(username.getText().toString(), mail.getText().toString(), telephone.getText().toString(),password.getText().toString(),this);
            //Mostrar el mensaje que devuelva el método
            toast.show();
            if (registred){
                //Volver al Login
                registred = false;
               Intent intent = Mediator.returnToLogin(this);
               startActivity(intent);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
