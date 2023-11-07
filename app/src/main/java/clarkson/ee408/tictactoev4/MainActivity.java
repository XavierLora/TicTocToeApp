package clarkson.ee408.tictactoev4;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.os.Bundle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import socket.Response;
import socket.Request;
import client.SocketClient;
import client.AppExecutors;

public class MainActivity extends AppCompatActivity {
    private TicTacToe tttGame;
    private Button [][] buttons;
    private TextView status;
    private Gson gson;
    private Handler handler;
    private static final int REQUEST_INTERVAL = 1000;
    private boolean shouldRequestMove = false;
    private final AppExecutors appExecutors = AppExecutors.getInstance(); // Initialize the AppExecutors instance


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tttGame = new TicTacToe(2);
        gson = new GsonBuilder().serializeNulls().create();
        buildGuiByCode();
        handler = new Handler(Looper.getMainLooper());

        final AppExecutors appExecutors = AppExecutors.getInstance();

        // Start a background thread for socket connection and requesting moves
        appExecutors.networkIO().execute(new Runnable() {
            @Override
            public void run() {
                // Set the server's IP address and port number
                SocketClient.getInstance().setServer("192.168.68.104", 5650);

                // Connect to the server
                if (SocketClient.getInstance().connectToServer()) {
                    shouldRequestMove = true; // Connection successful, enable requestMove
                } else {
                    // Handle connection failure, e.g., show an error message
                    shouldRequestMove = false;
                }

                startPeriodicRequests();
            }
        });
    }


    public void buildGuiByCode( ) {
        // Get width of the screen
        Point size = new Point( );
        getWindowManager( ).getDefaultDisplay( ).getSize( size );
        int w = size.x / TicTacToe.SIDE;

        // Create the layout manager as a GridLayout
        GridLayout gridLayout = new GridLayout( this );
        gridLayout.setColumnCount( TicTacToe.SIDE );
        gridLayout.setRowCount( TicTacToe.SIDE + 2 );

        // Create the buttons and add them to gridLayout
        buttons = new Button[TicTacToe.SIDE][TicTacToe.SIDE];
        ButtonHandler bh = new ButtonHandler( );

//        GridLayout.LayoutParams bParams = new GridLayout.LayoutParams();
//        bParams.width = w - 10;
//        bParams.height = w -10;
//        bParams.bottomMargin = 15;
//        bParams.rightMargin = 15;

        gridLayout.setUseDefaultMargins(true);

        for( int row = 0; row < TicTacToe.SIDE; row++ ) {
            for( int col = 0; col < TicTacToe.SIDE; col++ ) {
                buttons[row][col] = new Button( this );
                buttons[row][col].setTextSize( ( int ) ( w * .2 ) );
                buttons[row][col].setOnClickListener( bh );
                GridLayout.LayoutParams bParams = new GridLayout.LayoutParams();
//                bParams.width = w - 10;
//                bParams.height = w -40;

                bParams.topMargin = 0;
                bParams.bottomMargin = 10;
                bParams.leftMargin = 0;
                bParams.rightMargin = 10;
                bParams.width=w-10;
                bParams.height=w-10;
                buttons[row][col].setLayoutParams(bParams);
                gridLayout.addView( buttons[row][col]);
//                gridLayout.addView( buttons[row][col], bParams );
            }
        }

        // set up layout parameters of 4th row of gridLayout
        status = new TextView( this );
        GridLayout.Spec rowSpec = GridLayout.spec( TicTacToe.SIDE, 2 );
        GridLayout.Spec columnSpec = GridLayout.spec( 0, TicTacToe.SIDE );
        GridLayout.LayoutParams lpStatus
                = new GridLayout.LayoutParams( rowSpec, columnSpec );
        status.setLayoutParams( lpStatus );

        // set up status' characteristics
        status.setWidth( TicTacToe.SIDE * w );
        status.setHeight( w );
        status.setGravity( Gravity.CENTER );
        status.setBackgroundColor( Color.GREEN );
        status.setTextSize( ( int ) ( w * .15 ) );
        status.setText( tttGame.result( ) );

        gridLayout.addView( status );

        // Set gridLayout as the View of this Activity
        setContentView( gridLayout );
    }

    public void update( int row, int col ) {
        int play = tttGame.play( row, col );
        if( play == 1 )
            buttons[row][col].setText( "X" );
        else if( play == 2 )
            buttons[row][col].setText( "O" );
        if( tttGame.isGameOver( ) ) {
            status.setBackgroundColor( Color.RED );
            enableButtons( false );
            status.setText( tttGame.result( ) );
            showNewGameDialog( );	// offer to play again
        }
    }

    public void enableButtons( boolean enabled ) {
        for( int row = 0; row < TicTacToe.SIDE; row++ )
            for( int col = 0; col < TicTacToe.SIDE; col++ )
                buttons[row][col].setEnabled( enabled );
    }

    public void resetButtons( ) {
        for( int row = 0; row < TicTacToe.SIDE; row++ )
            for( int col = 0; col < TicTacToe.SIDE; col++ )
                buttons[row][col].setText( "" );
    }

    private class ButtonHandler implements View.OnClickListener {
        public void onClick( View v ) {
            Log.d("button clicked", "button clicked");

            for( int row = 0; row < TicTacToe.SIDE; row ++ ) {
                for (int column = 0; column < TicTacToe.SIDE; column++) {
                    if (v == buttons[row][column]) {
                        int move = row * TicTacToe.SIDE + column;
                        sendMove(move);
                        update(row, column);
                    }
                }
            }
        }
    }

    public void showNewGameDialog() {
        if (!isFinishing()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(tttGame.result());
            alert.setMessage("Do you want to play again?");
            PlayDialog playAgain = new PlayDialog();
            alert.setPositiveButton("YES", playAgain);
            alert.setNegativeButton("NO", playAgain);
            alert.show();
        }
    }

    private class PlayDialog implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int id) {
            if (!isFinishing()) {
                if (id == DialogInterface.BUTTON_POSITIVE) {
                    // Switch the player's turn for the next game
                    tttGame.switchPlayer();

                    tttGame.resetGame();
                    enableButtons(true);
                    resetButtons();
                    status.setBackgroundColor(Color.GREEN);
                    status.setText("Your Turn"); // Set the initial status
                } else if (id == DialogInterface.BUTTON_NEGATIVE) {
                    MainActivity.this.finish();
                }
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close the socket connection and release resources when the app is destroyed
        SocketClient.getInstance().close();
    }
    private void startPeriodicRequests() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (shouldRequestMove) {
                    // Call the method to send a request to the server (e.g., requestMove())
                    appExecutors.networkIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            requestMove();
                        }
                    });
                }

                // Schedule the next request after the defined interval
                handler.postDelayed(this, REQUEST_INTERVAL);
            }
        }, REQUEST_INTERVAL);
    }
    private void requestMove() {
        // Create a Request with type REQUEST_MOVE
        Request request = new Request(Request.RequestType.REQUEST_MOVE, "");

        // Send the request using the SocketClient and receive a response
        Response response = SocketClient.getInstance().sendRequest(request, Response.class);

        if (response != null && response.getData() != null) {
            // Check if the response is successful and contains a valid move
            if (response.getStatus() == Response.ResponseStatus.SUCCESS) {
                Integer moveInteger = gson.fromJson(response.getData(), Integer.class);

                if (moveInteger != null) {
                    int move = moveInteger.intValue();

                    // Update the game board based on the received move
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            update(move / TicTacToe.SIDE, move % TicTacToe.SIDE);
                        }
                    });
                }
            }
        }
    }


    private void sendMove(int move) {
        final int finalMove = move; // Store the move as a final variable

        // Execute the network-related code on a background thread using networkIO executor
        AppExecutors appExecutors = AppExecutors.getInstance();
        appExecutors.networkIO().execute(new Runnable() {
            @Override
            public void run() {
                // Create a Request with type SEND_MOVE and set the data attribute with the move
                Request request = new Request(Request.RequestType.SEND_MOVE, gson.toJson(finalMove));

                // Send the request using the SocketClient
                Response response = SocketClient.getInstance().sendRequest(request, Response.class);

                if (response != null && response.getStatus() == Response.ResponseStatus.SUCCESS) {
                    // Update the game board locally
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            update(finalMove / TicTacToe.SIDE, finalMove % TicTacToe.SIDE);
                        }
                    });
                }
            }
        });
    }
}