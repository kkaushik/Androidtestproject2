package aexp.jsoncalc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class Entry extends Activity {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView( R.layout.entry );
        Spinner s = (Spinner)findViewById( R.id.opselector );
        ArrayAdapter adapter = ArrayAdapter.createFromResource(
            this, 
            R.array.ops,
            android.R.layout.simple_spinner_item );
        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
        s.setAdapter( adapter );
        Button addEntryButton = (Button)findViewById( R.id.add_entry );
        addEntryButton.setOnClickListener( new View.OnClickListener() {
            public void onClick( View view ) {
                double v1 = 0.0,v2 = 0.0;
                String value = null;
                try {
                    value = ((EditText)findViewById( R.id.v1 )).getText().toString();
                    v1 = Double.parseDouble( value );
                    value = ((EditText)findViewById( R.id.v2 )).getText().toString();
                    v2 = Double.parseDouble( value );
                    Spinner s = (Spinner)findViewById( R.id.opselector );
                    int pos = s.getSelectedItemPosition();
                    if( pos == AdapterView.INVALID_POSITION )
                        Toast.makeText( Entry.this, 
                                "No operation selected",
                                Toast.LENGTH_SHORT).show();
                    else {
                        Intent extras = new Intent();
                        extras.putExtra( "v1",v1 );
                        extras.putExtra( "v2",v2 );
                        extras.putExtra( "op", pos+1 );
                        setResult( RESULT_OK, extras );
                        finish();
                    }
                } catch( NumberFormatException ex ) {
                    Toast.makeText( Entry.this, 
                                "Invalid number: "+value,
                                Toast.LENGTH_SHORT).show();
                }
            }
        } );

    }
}
