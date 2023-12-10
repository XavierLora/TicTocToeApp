package clarkson.ee408.tictactoev4;

import static socket.Request.RequestType.LOGIN;
import static socket.Request.RequestType.REGISTER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.lang.ref.WeakReference;

import client.AppExecutors;
import client.SocketClient;
import model.User;
import socket.Request;
import socket.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameField;
    private EditText passwordField;
    private EditText confirmPasswordField;
    private EditText displayNameField;
    private Gson gson;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Getting Inputs
        Button registerButton = findViewById(R.id.buttonRegister);
        Button loginButton = findViewById(R.id.buttonLogin);
        usernameField = findViewById(R.id.editTextUsername);
        passwordField = findViewById(R.id.editTextPassword);
        confirmPasswordField = findViewById(R.id.editTextConfirmPassword);
        displayNameField = findViewById(R.id.editTextDisplayName);

        // TODO: Initialize Gson with null serialization option
        gson = new Gson();

        // Display a toast indicating the connection status


        //Adding Handlers
        //TODO: set an onclick listener to registerButton to call handleRegister()
        registerButton.setOnClickListener(view -> handleRegister());
        //TODO: set an onclick listener to loginButton to call goBackLogin()
        loginButton.setOnClickListener(view -> goBackLogin());
    }

    /**
     * Process registration input and pass it to {@link #submitRegistration(User)}
     */
    public void handleRegister() {
        // TODO: declare local variables for username, password, confirmPassword and displayName. Initialize their values with their corresponding EditText
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();
        String confirmPassword = confirmPasswordField.getText().toString();
        String displayName = displayNameField.getText().toString();
        // TODO: verify that all fields are not empty before proceeding. Toast with the error message
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(confirmPassword) || TextUtils.isEmpty(displayName)) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: verify that password is the same as confirm password. Toast with the error message
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Create User object with username, display name, and password and call submitRegistration()
        User user = new User(username, password, displayName, false);
        submitRegistration(user);
        // TODO: verify that password is the same af confirm password. Toast with the error message

        // TODO: Create User object with username, display name and password and call submitRegistration()
    }

    /**
     * Sends REGISTER request to the server
     * @param user the User to register
     */
    void submitRegistration(User user) {
        new RegistrationTask(this).execute(user);
    }
    /**
     * Change the activity to LoginActivity
     */
    private static class RegistrationTask extends AsyncTask<User, Void, Response> {
        private WeakReference<RegisterActivity> activityReference;

        RegistrationTask(RegisterActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Response doInBackground(User... users) {
            User user = users[0];
            SocketClient socketClient = SocketClient.getInstance();
            Request registerRequest = new Request(REGISTER, new Gson().toJson(user));
            return socketClient.sendRequest(registerRequest, Response.class);
        }

        @Override
        protected void onPostExecute(Response response) {
            RegisterActivity activity = activityReference.get();
            if (activity != null) {
                // Handle the response here
                if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                    activity.goBackLogin();
                } else {
                    // Handle failure
                }
            }
        }
    }
    private void goBackLogin() {
        //TODO: Close this activity by calling finish(), it will automatically go back to its parent (i.e,. LoginActivity)
        finish();
    }





}