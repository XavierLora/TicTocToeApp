package clarkson.ee408.tictactoev4;

import static socket.Request.RequestType.LOGIN;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.lang.ref.WeakReference;

import client.SocketClient;
import model.User;
import socket.Request;
import socket.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameField;
    private EditText passwordField;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Getting UI elements
        Button loginButton = findViewById(R.id.buttonLogin);
        Button registerButton = findViewById(R.id.buttonRegister);
        usernameField = findViewById(R.id.editTextUsername);
        passwordField = findViewById(R.id.editTextPassword);

        // TODO: Initialize Gson with null serialization option
        gson = new Gson();

        //Adding Handlers
        loginButton.setOnClickListener(view -> handleLogin());
        registerButton.setOnClickListener(view -> gotoRegister());
    }

    public void handleLogin() {
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();

        // TODO: verify that all fields are not empty before proceeding. Toast with the error message
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Username and password cannot be empty", Toast.LENGTH_SHORT).show();
        } else {
            User user = new User(username, password, username, true);

            new LoginTask(this).execute(user);
        }

        // TODO: Create User object with username and password and call submitLogin()
    }

    /**
     * Sends a LOGIN request to the server
     *
     *
     */
    private static class LoginTask extends AsyncTask<User, Void, Response> {
        private WeakReference<LoginActivity> activityReference;

        LoginTask(LoginActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Response doInBackground(User... users) {
            User user = users[0];
            user.setOnline(true);
            SocketClient socketClient = SocketClient.getInstance();
            Request loginRequest = new Request(Request.RequestType.LOGIN, new Gson().toJson(user));
            return socketClient.sendRequest(loginRequest, Response.class);
        }

        @Override
        protected void onPostExecute(Response response) {
            LoginActivity activity = activityReference.get();
            if (activity != null) {
                // Handle the response here
                if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                    activity.gotoPairing(response.getMessage());
                } else {
                    Toast.makeText(activity, "Login failed: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Sends a LOGIN request to the server
     *
     * @param user User object to login
     */
    public void submitLogin(User user) {
        SocketClient socketClient = SocketClient.getInstance();

        // Create a request object with the user credentials
        Request loginRequest = new Request(LOGIN, gson.toJson(user));

        // Send the request and get the response
        Response response = socketClient.sendRequest(loginRequest, Response.class);

        // Check the response status
        if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
            gotoPairing(user.getUsername());
        } else {
            Toast.makeText(this, "Login failed: " + response.getMessage(), Toast.LENGTH_SHORT).show();
        }// TODO: Send a LOGIN request, If SUCCESS response, call gotoPairing(), else, Toast the error message from sever
    }

    /**
     * Switch the page to {@link PairingActivity}
     *
     * @param username the data to send
     */
    public void gotoPairing(String username) {
        // TODO: start PairingActivity and pass the username
        Intent intent = new Intent(this, PairingActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    /**
     * Switch the page to {@link RegisterActivity}
     */
    public void gotoRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}