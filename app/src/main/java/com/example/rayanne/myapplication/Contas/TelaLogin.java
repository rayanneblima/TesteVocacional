package com.example.rayanne.myapplication.Contas;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.rayanne.myapplication.MainActivity;
import com.example.rayanne.myapplication.Menu.PagMenu;
import com.example.rayanne.myapplication.Others.SharedPref;
import com.example.rayanne.myapplication.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TelaLogin extends AppCompatActivity {
    //TODO: se for o primeiro login com o fb, deve ser pedido pra completar as informações do perfil. E-mail tbm devera ser vinculado.
    //TODO: melhorar os toast de mensagem
    //TODO: conferir o private (segurança) dos campos

    private EditText edtEmail, edtSenha;
    private Button entrarButton;

    //private static String URL_LOGIN = "http://192.168.2.4/teste/login.php";
   // private static String URL_LOGIN = "http://rolenoifapp.epizy.com/login.php";
    private static String URL_LOGIN = "https://rolenoifapp.000webhostapp.com/login.php"; //site no ar
    private CallbackManager callbackManager = CallbackManager.Factory.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_login);
        getSupportActionBar().hide();

        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        entrarButton = findViewById(R.id.entrarButton);

        loginFB();
        loginbtnEntrar();
        cadastroUsuario();

    }

    /*public void obterKeyHash(){
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.example.rayanne.myapplication", PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }*/

    public void loginFB(){

        //botão facebook
        Button loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginManager.getInstance().logInWithReadPermissions(TelaLogin.this,Arrays.asList("public_profile","email"));
            }
        });

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (Profile.getCurrentProfile() != null) {
                    AccessToken accessToken = loginResult.getAccessToken();
                    Toast.makeText(getApplicationContext(), "Aguarde.", Toast.LENGTH_SHORT).show();
                    get_profile(accessToken);
                    //Login OK, session = true
                    SharedPref.save(getApplicationContext(), "session", "true");
                    Profile profile = Profile.getCurrentProfile();
                    String firstName = profile.getName();
                    Toast.makeText(getApplicationContext(), "Bem-vindo(a), " + firstName + "!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(TelaLogin.this, PagMenu.class);
                    startActivity(intent);
                    finish();
                } else {
                    ProfileTracker profileTracker = new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                            stopTracking();
                            Profile.setCurrentProfile(currentProfile);
                            String firstName = currentProfile.getName();
                            Toast.makeText(getApplicationContext(), "Bem-vindo(a), " + firstName + "!", Toast.LENGTH_LONG).show();
                            SharedPref.save(getApplicationContext(), "session", "true");
                            Intent intent = new Intent(TelaLogin.this, PagMenu.class);
                            startActivity(intent);
                            finish();
                        }
                    };
                    profileTracker.startTracking();
                }
            }

            @Override
            public void onCancel() {
                Toast.makeText(getApplicationContext(), R.string.cancel_login, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getApplicationContext(), R.string.error_login, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void get_profile(AccessToken accessToken){
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken, new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        //Intent profileIntent = new Intent(MainActivity.this, Perfil.class);
                        try {
                            String userID = object.getString("id");
                            SharedPref.saveUserId(getApplicationContext(), "user id", userID);
                            String firstname;
                            String lastname;
                            String email;

                            if (object.has("first_name") && object.has("last_name")) {
                                firstname = object.getString("first_name");
                                lastname = object.getString("last_name");
                                SharedPref.saveUserName(getApplicationContext(), "user name", firstname + " " + lastname);
                            }
                            if (object.has("email")){
                                email = object.getString("email");
                                SharedPref.saveUserEmail(getApplicationContext(), "user email", email);
                            }
                            //startActivity(profileIntent);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id, first_name, last_name, email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    public void loginbtnEntrar(){
        //botão entrar
        entrarButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                String emailUser = edtEmail.getText().toString().trim();
                String senhaUser = edtSenha.getText().toString().trim();

                if (emailUser.equals("") || senhaUser.equals("")) {
                    Toast.makeText(getApplicationContext(), R.string.branco_cadastro, Toast.LENGTH_SHORT).show();
                } else {
                    logarUsuario(emailUser, senhaUser);
                }
            }
        });
    }

    private void logarUsuario(final String emailUser, final String senhaUser) {
        // StringRequest() começa aqui
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_LOGIN,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse( String response) {
                        //Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
                        try{
                            JSONObject jsonObject = new JSONObject(response);
                            String code = jsonObject.getString("code");
                            if (code.equals("sucess")){
                                Toast.makeText(getApplicationContext(), "Bem-vindo(a)!", Toast.LENGTH_LONG).show();
                                SharedPref.save(getApplicationContext(), "session", "true");
                                Intent intent = new Intent(TelaLogin.this, PagMenu.class);
                                startActivity(intent);
                                finish();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Confira os dados inseridos!", Toast.LENGTH_SHORT).show();
                            //Toast.makeText(getApplicationContext(), "ERROR: " + e.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Erro ao logar!", Toast.LENGTH_SHORT).show();
                        error.printStackTrace();
                    }
                }
        ){
            @Override
            protected Map<String, String> getParams() {
                // Cria um mapa p/ Criptografia
                Map<String, String> params = new HashMap<>();
                params.put("emailUser", emailUser);
                params.put("senhaUser", senhaUser);
                return params;
            }

        };
        // StringRequest() acaba aqui --------------------------------------------------------------------------
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    public void cadastroUsuario(){
        //botão cadastre-se
        Button btnCadastrar = findViewById(R.id.btnCadastrar);

        btnCadastrar.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setClass(TelaLogin.this, TelaCadastro.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}
