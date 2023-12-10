package clarkson.ee408.tictactoev4;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import socket.Request;
import socket.Response;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.util.List;
import client.SocketClient;
import model.Event;
import model.User;
import socket.PairingResponse;

public class PairingActivity extends AppCompatActivity {

    private final String TAG = "PAIRING";

    private static Gson gson;

    private TextView noAvailableUsersText;
    private RecyclerView recyclerView;
    private AvailableUsersAdapter adapter;

    private Handler handler;
    private Runnable refresh;
    private AlertDialog invitationDialog;

    private boolean shouldUpdatePairing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        Log.e(TAG, "App is now created");
        // TODO: setup Gson with null serialization option
        gson = new Gson();

        //Setting the username text
        TextView usernameText = findViewById(R.id.text_username);
        // TODO: set the usernameText to the username passed from LoginActivity (i.e from Intent)
        String username = getIntent().getStringExtra("username");
        usernameText.setText(username);
        //Getting UI Elements
        noAvailableUsersText = findViewById(R.id.text_no_available_users);
        recyclerView = findViewById(R.id.recycler_view_available_users);

        //Setting up recycler view adapter
        adapter = new AvailableUsersAdapter(this, this::sendGameInvitation);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        updateAvailableUsers(null);

        handler = new Handler();
        refresh = () -> {
            // TODO: call getPairingUpdate if shouldUpdatePairing is true
            if(shouldUpdatePairing = true){
                getPairingUpdate();
            }
            handler.postDelayed(refresh, 3000);
        };
        handler.post(refresh);
    }

    /**
     * Send UPDATE_PAIRING request to the server
     */
    private void getPairingUpdate() {
        // TODO:  Send an UPDATE_PAIRING request to the server. If SUCCESS call handlePairingUpdate(). Else, Toast the error
        new UpdatePairingTask(this).execute();
    }
    private static class UpdatePairingTask extends AsyncTask<Void, Void, PairingResponse> {
        private WeakReference<PairingActivity> activityReference;

        UpdatePairingTask(PairingActivity activity) {
            activityReference = new WeakReference<>(activity);
        }

        @Override
        protected PairingResponse doInBackground(Void... voids) {
            PairingActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return null;
            }

            SocketClient socketClient = SocketClient.getInstance();
            return socketClient.sendRequest(new Request(Request.RequestType.UPDATE_PAIRING, null), PairingResponse.class);
        }

        @Override
        protected void onPostExecute(PairingResponse pairingResponse) {
            PairingActivity activity = activityReference.get();
            if (activity != null && pairingResponse != null &&
                    pairingResponse.getStatus() == Response.ResponseStatus.SUCCESS) {
                activity.handlePairingUpdate(pairingResponse);
            } else if (activity != null) {
                Toast.makeText(activity, "Failed to update pairing", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Handle the PairingResponse received form the server
     * @param response PairingResponse from the server
     */
    private void handlePairingUpdate(PairingResponse response) {
        if (response == null) {
            Log.e("PAIRING_RESPONSE", "Received null response");
            return;
        }

        // Handle availableUsers
        List<User> availableUsers = response.getAvailableUsers();
        if (availableUsers != null) {
            updateAvailableUsers(availableUsers);
        }

        // Handle invitationResponse
        Event invitationResponse = response.getInvitationResponse();
        if (invitationResponse != null) {
            new SendAcknowledgementTask().execute(invitationResponse);

            // Handle ACCEPTED and DECLINED responses
            if (invitationResponse.getStatus() == Event.EventStatus.ACCEPTED) {
                Toast.makeText(this, "Invitation accepted", Toast.LENGTH_SHORT).show();
                beginGame(response.getInvitation(), 1);
            } else if (invitationResponse.getStatus() == Event.EventStatus.DECLINED) {
                Toast.makeText(this, "Invitation declined", Toast.LENGTH_SHORT).show();
            }
        }

        // Handle invitation
        Event invitation = response.getInvitation();
        if (invitation != null) {
            createRespondAlertDialog(invitation);
        }
    }

    /**
     * Updates the list of available users
     * @param availableUsers list of users that are available for pairing
     */
    public void updateAvailableUsers(List<User> availableUsers) {
        adapter.setUsers(availableUsers);
        if (adapter.getItemCount() <= 0) {
            // TODO show noAvailableUsersText and hide recyclerView
            noAvailableUsersText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // TODO hide noAvailableUsersText and show recyclerView
            noAvailableUsersText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Sends game invitation to an
     * @param userOpponent the User to send invitation to
     */
    private void sendGameInvitation(User userOpponent) {
        // TODO: Send a SEND_INVITATION request to the server using AsyncTask
        new SendInvitationTask(this, userOpponent).execute();
    }

    private static class SendInvitationTask extends AsyncTask<Void, Void, Response> {
        private WeakReference<PairingActivity> activityReference;
        private User userOpponent;

        SendInvitationTask(PairingActivity activity, User userOpponent) {
            activityReference = new WeakReference<>(activity);
            this.userOpponent = userOpponent;
        }

        @Override
        protected Response doInBackground(Void... voids) {
            PairingActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return null;
            }

            String username = userOpponent.getUsername();
            SocketClient socketClient = SocketClient.getInstance();



            // Create a Request with the serialized JSON
            Request request = new Request(Request.RequestType.SEND_INVITATION,gson.toJson(username));

            // Send the request and handle the response
            return socketClient.sendRequest(request, Response.class);
        }
        @Override
        protected void onPostExecute(Response response) {
            PairingActivity activity = activityReference.get();
            if (activity != null) {
                if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                    Toast.makeText(activity, "Invitation sent successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Failed to send invitation", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    /**
     * Sends an ACKNOWLEDGE_RESPONSE request to the server
     * Tell server i have received accept or declined response from my opponent
      */
    private void sendAcknowledgement(Event invitationResponse) {
        new SendAcknowledgementTask().execute(invitationResponse);
    }

    private class SendAcknowledgementTask extends AsyncTask<Event, Void, Void> {

        @Override
        protected Void doInBackground(Event... events) {
            if (events.length > 0) {
                Event invitationResponse = events[0];

                // TODO: Implement the network operation in the background
                SocketClient socketClient = SocketClient.getInstance();
                Request request = new Request(Request.RequestType.ACKNOWLEDGE_RESPONSE,
                        gson.toJson(invitationResponse.getEventId()));
                socketClient.sendRequest(request, Response.class);

                // Any UI-related operations (like Toast) should be done in onPostExecute
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Handle UI updates or post-execution tasks here
            // For example, show a Toast message
            Toast.makeText(PairingActivity.this, "Acknowledgement sent", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create a dialog showing incoming invitation
     * @param invitation the Event of an invitation
     */
    private void createRespondAlertDialog(Event invitation) {
        // TODO: set shouldUpdatePairing to false
        shouldUpdatePairing = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Game Invitation");
        builder.setMessage(invitation.getSender() + " has Requested to Play with You");
        builder.setPositiveButton("Accept", (dialogInterface, i) -> acceptInvitation(invitation));
        builder.setNegativeButton("Decline", (dialogInterface, i) -> declineInvitation(invitation));
        invitationDialog = builder.create(); // Save the reference to the AlertDialog

        invitationDialog.show(); // Show the dialog

        // Dismiss the dialog when the user declines the invitation
        builder.setOnDismissListener(dialog -> {
            shouldUpdatePairing = true; // Make sure to update pairing if the invitation is declined
        });
    }

    private void acceptInvitation(Event invitation) {
        // TODO: Send an ACCEPT_INVITATION request to the server. If SUCCESS beginGame() as player 2. Else, Toast the error

        new AcceptInvitationTask(this, invitation).execute();
    }

    private static class AcceptInvitationTask extends AsyncTask<Void, Void, Response> {
        private WeakReference<PairingActivity> activityReference;
        private Event invitation;

        AcceptInvitationTask(PairingActivity activity, Event invitation) {
            activityReference = new WeakReference<>(activity);
            this.invitation = invitation;
        }

        @Override
        protected Response doInBackground(Void... voids) {
            PairingActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return null;
            }

            SocketClient socketClient = SocketClient.getInstance();
            Request request = new Request(Request.RequestType.ACCEPT_INVITATION, gson.toJson(invitation.getEventId()));
            return socketClient.sendRequest(request, Response.class);
        }

        @Override
        protected void onPostExecute(Response response) {
            PairingActivity activity = activityReference.get();
            if (activity != null) {
                if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                    activity.beginGame(invitation, 2);
                } else {
                    Toast.makeText(activity, "Failed to accept invitation", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Sends a DECLINE_INVITATION to the server
     *
     * @param invitation the Event invitation to decline
     */
    private void declineInvitation(Event invitation) {
        // TODO: Send a DECLINE_INVITATION request to the server. If SUCCESS response, Toast a message, else, Toast the error
        new DeclineInvitationTask(this, invitation).execute();
        if (invitationDialog != null && invitationDialog.isShowing()) {
            invitationDialog.dismiss();
        }
    }

    private static class DeclineInvitationTask extends AsyncTask<Void, Void, Response> {
        private WeakReference<PairingActivity> activityReference;
        private Event invitation;

        DeclineInvitationTask(PairingActivity activity, Event invitation) {
            activityReference = new WeakReference<>(activity);
            this.invitation = invitation;
        }

        @Override
        protected Response doInBackground(Void... voids) {
            PairingActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return null;
            }

            SocketClient socketClient = SocketClient.getInstance();
            Request request = new Request(Request.RequestType.DECLINE_INVITATION, gson.toJson(invitation.getEventId()));
            return socketClient.sendRequest(request, Response.class);
        }

        @Override
        protected void onPostExecute(Response response) {
            PairingActivity activity = activityReference.get();
            if (activity != null) {
                if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                    Toast.makeText(activity, "Invitation declined", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Failed to decline invitation", Toast.LENGTH_SHORT).show();
                }

                activity.shouldUpdatePairing = true;
            }
        }
    }

    /**
     *
     * @param pairing the Event of pairing
     * @param player either 1 or 2
     */
    private void beginGame(Event pairing, int player) {
        // TODO: set shouldUpdatePairing to false

        // TODO: start MainActivity and pass player as data
        shouldUpdatePairing = false;
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("PLAYER", player);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: set shouldUpdatePairing to true
        shouldUpdatePairing = true;
        getPairingUpdate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        // TODO: set shouldUpdatePairing to false
        shouldUpdatePairing = false;
        // TODO: logout by calling close() function of SocketClient


        SocketClient.getInstance().close();
    }

}