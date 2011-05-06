package aexp.jsoncalc;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import org.apache.http.HttpResponse;  
import org.apache.http.HttpStatus;    
import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.*;
import java.io.DataInputStream;
import java.io.IOException;

public class JsonCalc extends ListActivity
{

// New entry
    private static final int MENU_ENTRY = 1;    
// Process list
    private static final int MENU_PROCESS = 2;    
// Clear list
    private static final int MENU_CLEAR = 3;    

    private static final int ACTIVITY_ENTRY = 1;

    static final String LOG_TAG = "JSONCALC";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        adapter = new ArrayAdapter<OpEntry>( this, R.layout.main_item );
        adapter.setNotifyOnChange( true );
        setListAdapter( adapter );
        uiHandler = new Handler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add( Menu.NONE, MENU_ENTRY, Menu.NONE, R.string.menu_entry );
        menu.add( Menu.NONE, MENU_PROCESS, Menu.NONE, R.string.menu_process );
        menu.add( Menu.NONE, MENU_CLEAR, Menu.NONE, R.string.menu_clear );
        return result;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch ( item.getItemId() ) {
            case MENU_ENTRY: {
                    Intent i = new Intent();
                    i.setClassName( 
                        "aexp.jsoncalc",
                        "aexp.jsoncalc.Entry" );
                    startActivityForResult(i, ACTIVITY_ENTRY );
                    return true;
                }

            case MENU_CLEAR:
                adapter.clear();
                return true;

            case MENU_PROCESS:
                setProgressBarIndeterminateVisibility( true );
                ProcessList p = new ProcessList();
                p.start();
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult( int requestCode,
                                        int resultCode, 
                                        Intent extras ) {
        super.onActivityResult( requestCode, resultCode, extras);
        Log.d( LOG_TAG, "onActivityResult: requestCode: "+
                    requestCode+
                    "; resultCode: "+resultCode+
                    "; extras: "+extras );
        switch(requestCode) {
            case ACTIVITY_ENTRY: {
                    if( resultCode == RESULT_OK ) {
                        double v1 = extras.getDoubleExtra( "v1",0.0 );
                        double v2 = extras.getDoubleExtra( "v2",0.0 );
                        int op = extras.getIntExtra( "op", 0 );
                        OpEntry oe = new OpEntry( v1,v2,op );
                        adapter.add( oe );
                    }
                }
                break;
        }
    }


    private String process() {
        String errMsg = null;
        JSONArray elements = new JSONArray();
        for( int i = 0 ; i < adapter.getCount() ; ++i ) {
            try {
                elements.put( adapter.getItem( i ).toJSON() );
            } catch( JSONException ex ) {
                Log.e( LOG_TAG, "JSONException", ex );
            }
        }
        String request = elements.toString();
        Log.d( LOG_TAG, "request: "+request );
        String response = null;
        try {
            response = sendToServer( request );
            Log.d( LOG_TAG, "response: "+response );
            JSONTokener tokener = new JSONTokener( response );
            Object o = tokener.nextValue();
            if( o instanceof JSONArray ) {
                JSONArray array = (JSONArray)o; 
                for( int i = 0 ; i < array.length() ; ++i ) {
                    JSONObject object = array.getJSONObject( i );
                    int id = object.getInt( OpEntry.ID_KEY );
                    double result = object.getDouble( OpEntry.RESULT_KEY );
                    OpEntry op = getOpEntry( id );
                    if( op != null )
                        op.setResult( result );
                }
            } else
                throw new JSONException( "Top element is not a JSONArray" );
        } catch( IOException ex ) {
            errMsg = "Connection problem";
            Log.e( LOG_TAG, "IOException", ex );
        } catch( JSONException ex ) {
            errMsg = "Malformed response";
            Log.e( LOG_TAG, "Malformed JSON response: "+response, ex );
        }
        return errMsg;
    }

    private OpEntry getOpEntry( int id ) {
        for( int i = 0 ; i < adapter.getCount() ; ++i ) {
            OpEntry op = adapter.getItem( i );
            if( op.getId() == id )
                return op;
        }
        return null;
    }

    private String sendToServer( String request ) throws IOException {
        String result = null;
        maybeCreateHttpClient();
        HttpPost post = new HttpPost( Config.APP_BASE_URI );
        post.addHeader( "Content-Type", "text/vnd.aexp.json.req" );
        post.setEntity( new StringEntity( request ) );    
        HttpResponse resp = httpClient.execute( post );
// Execute the POST transaction and read the results
        int status = resp.getStatusLine().getStatusCode(); 
        if( status != HttpStatus.SC_OK )
                throw new IOException( "HTTP status: "+Integer.toString( status ) );
        DataInputStream is = new DataInputStream( resp.getEntity().getContent() );
        result = is.readLine();
        return result;
    }

    private void maybeCreateHttpClient() {
        if ( httpClient == null) {
            httpClient = new DefaultHttpClient();
            HttpParams params = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, REGISTRATION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, REGISTRATION_TIMEOUT);
            ConnManagerParams.setTimeout(params, REGISTRATION_TIMEOUT);
        }
    }


    private static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
    private ArrayAdapter<OpEntry> adapter;
    private DefaultHttpClient httpClient = null;
    private Handler uiHandler;

    class ProcessList extends Thread {
        public void run() {
            String errMsg = process();
            ProcessListUIUpdate plu = new ProcessListUIUpdate( errMsg );
            uiHandler.post( plu );
        }
    }

    class ProcessListUIUpdate implements Runnable {
        ProcessListUIUpdate( String errMsg ) {
            this.errMsg = errMsg;
        }

        public void run() {
            setProgressBarIndeterminateVisibility( false );
            if( errMsg != null )
                Toast.makeText( JsonCalc.this, 
                        errMsg,
                        Toast.LENGTH_SHORT).show();
            else
                adapter.notifyDataSetChanged();
        }

        String errMsg;
    }
}

class OpEntry {
    public static final int OP_ADD = 1;
    public static final int OP_SUB = 2;
    public static final int OP_MUL = 3;
    public static final int OP_DIV = 4;


    public static final String ID_KEY = "id";
    public static final String V1_KEY = "v1";
    public static final String V2_KEY = "v2";
    public static final String OP_KEY = "op";
    public static final String RESULT_KEY = "result";

    OpEntry( double v1, double v2, int op ) {
            this.v1 = v1;
            this.v2 = v2;
            this.op = op;
            resultSet = false;
            id = nextId++;
    }

    public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append( Double.toString( v1 ) );
            sb.append( opChar() );
            sb.append( Double.toString( v2 ) );
            if( resultSet ) {
                sb.append( '=' );
                sb.append( Double.toString( result ) );
            }
            return new String( sb );
    }

    public JSONObject toJSON() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put( ID_KEY,id );
            obj.put( V1_KEY,v1 );
            obj.put( V2_KEY,v2 );
            obj.put( OP_KEY,op );
            return obj;
    }

    
    public void setResult( double result ) {
        this.result = result;
        resultSet = true;
    }

    public int getId() {
        return id;
    }

    private char opChar() {
            char c = '?';
            switch( op ) {
                case OP_ADD:
                    c = '+';
                    break;

                case OP_SUB:
                    c = '-';
                    break;

                case OP_MUL:
                    c='*';
                    break;

                case OP_DIV:
                    c='/';
                    break;
            }
            return c;
    }

    private double v1,v2,result;
    private boolean resultSet;
    private int op;
    private int id;
    private static int nextId = 0;
}

