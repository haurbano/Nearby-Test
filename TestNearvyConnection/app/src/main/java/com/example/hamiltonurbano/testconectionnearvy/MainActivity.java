package com.example.hamiltonurbano.testconectionnearvy;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AppIdentifier;
import com.google.android.gms.nearby.connection.AppMetadata;
import com.google.android.gms.nearby.connection.Connections;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, Connections.ConnectionRequestListener, Connections.EndpointDiscoveryListener, AdapterView.OnItemClickListener, Connections.MessageListener {

    //Google Service
    GoogleApiClient mGoogleApiClient;
    boolean mIsHost = false;
    List<String> endPointClients = new ArrayList<>();

    //GUI
    Button btnIniciarPartida,btnUnircePartida,btnEnviartexto;
    ListView listaHost;
    EditText editEnviar;
    TextView txtRecibido;

    //Utiles
    List<String> listHost;

    //VariablesHost
    String endPointName,endPointId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();

        //region GUI
        btnIniciarPartida = (Button) findViewById(R.id.btn_inicar_partida);
        btnUnircePartida = (Button) findViewById(R.id.btn_unirce_partida);
        btnEnviartexto = (Button) findViewById(R.id.btn_enviar_msj);
        editEnviar = (EditText) findViewById(R.id.edit_texto_enviar);
        listaHost = (ListView) findViewById(R.id.lista_partidas);
        txtRecibido = (TextView) findViewById(R.id.txt_recibido);

        listaHost.setOnItemClickListener(this);

        listHost = new ArrayList<>();

        btnIniciarPartida.setOnClickListener(this);
        btnUnircePartida.setOnClickListener(this);
        btnEnviartexto.setOnClickListener(this);
        //endregion

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //region onstart y onStop
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }
    //endregion

    //region Eventos para conocer el resultado de la conexion
    @Override
    public void onConnected(Bundle bundle) {
        //Cuando se conecta
        Log.i("haur","Conexion con el api correcto");
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Conexion suspendida
        Log.i("haur", "Conexion suspendida");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Conexion fallida
        Log.i("haur", "Conexion fallida");
    }
    //endregion

    //region Probar Conexion
    private boolean estoyConectado()
    {
        int[] NETWORK_TYPES = {ConnectivityManager.TYPE_WIFI,ConnectivityManager.TYPE_ETHERNET};

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        for (int networkType: NETWORK_TYPES)
        {
            NetworkInfo info = connectivityManager.getNetworkInfo(networkType);
            if (info != null && info.isConnectedOrConnecting())
            {
                return true;
            }
        }
        return false;
    }
    //endregion

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_inicar_partida:
                iniciarPartida();
                break;
            case R.id.btn_unirce_partida:
                unirsePartida();
                break;
            case R.id.btn_enviar_msj:
                enviarTexto();
                break;
        }
    }

    //region Enviar Mensaje
    private void enviarTexto() {
        if (mIsHost){
            String msj = editEnviar.getText().toString();
            Nearby.Connections.sendReliableMessage(mGoogleApiClient,endPointClients,msj.getBytes());
        }
    }
    //endregion

    //region Unirce partida
    private void unirsePartida() {
        if (!estoyConectado()){
            Toast.makeText(this,"Debes estar conectado a una red",Toast.LENGTH_SHORT).show();
        }else{
            descubrirPartidas();
        }
    }
    //endregion

    //region descubrir partidas
    private void descubrirPartidas() {
        String serviceId = getString(R.string.service_id);

        long DISCOVER_TIMEOUT = 4000L;

        Nearby.Connections.startDiscovery(mGoogleApiClient,serviceId,DISCOVER_TIMEOUT,this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()){
                            Log.i("haur","dispositivo encontrado");
                            Toast.makeText(getApplicationContext(),"Busqueda iniciada",Toast.LENGTH_SHORT).show();
                        }else{
                            Log.i("haur","No se encontraron dispositivos");
                            Toast.makeText(getApplicationContext(),"NO se puedo buscar partidas",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    //endregion

    //region Iniciar Partida
    private void iniciarPartida()
    {
        if (!estoyConectado()){
            Toast.makeText(this,"Debes estar conectado a una red",Toast.LENGTH_SHORT).show();
        }else{
            iniciarServer();
        }
    }
    //endregion

    //region Iniciar Server
    private void iniciarServer()
    {
        mIsHost = true;
        //Permite a los dispositivos cercanos que se encuentren en la misma red
        //enterarse de est solicitud y les pide que intalen esta aplicacion
        List<AppIdentifier> appIdentifierList = new ArrayList<>();
        appIdentifierList.add(new AppIdentifier(getPackageName()));
        AppMetadata appMetadata = new AppMetadata(appIdentifierList);

        //Tiempo de espera para conexiones
        long NO_TIMEOUT =  0L; //tiempo indefinido

        String name = null;

        Nearby.Connections.startAdvertising(mGoogleApiClient,name,appMetadata,NO_TIMEOUT,this)
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(Connections.StartAdvertisingResult startAdvertisingResult) {
                        if (startAdvertisingResult.getStatus().isSuccess()){
                            Log.i("haur","Servidor Activo");
                            Toast.makeText(getApplicationContext(),"Partida Casi Lista",Toast.LENGTH_SHORT).show();
                        }else{
                            Log.i("haur","Partida fallida:"+ startAdvertisingResult.getStatus().getStatusMessage());
                            Toast.makeText(getApplicationContext(),"Algo fallo, vuelva a intentar",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    //endregion

    //region request
    @Override
    public void onConnectionRequest(final String s, String s1, String s2, byte[] bytes) {
        Toast.makeText(this,"Llega un request",Toast.LENGTH_SHORT).show();
        if (mIsHost){
            Nearby.Connections.acceptConnectionRequest(mGoogleApiClient,s,null,this).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Toast.makeText(getApplicationContext(), "Un dispositivo conectado", Toast.LENGTH_SHORT).show();
                        if (!endPointClients.contains(s)){
                            endPointClients.add(s);
                        }

                    }else{
                        Toast.makeText(getApplicationContext(), "Fallido de conexion", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else {
            Nearby.Connections.rejectConnectionRequest(mGoogleApiClient,s);
        }
        Log.i("haur", "LLega Request" + s + s1 + s2);
    }
    //endregion

    //Llegan los host (Server)
    @Override
    public void onEndpointFound(String s, String s1, String s2, String s3) {
        //s = endpointId
        //s1 = deviceId
        //s2 = serviceId
        //s3 = endpoinName
        String host = "EndPointId: "+s +" deviceId: "+s1+ " serviceId: "+s2+ " endPointName: "+s3;
        endPointName = s3;
        endPointId = s;

        listHost.add(host);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,listHost);

        listaHost.setAdapter(adapter);
    }

    @Override
    public void onEndpointLost(String s) {
        Toast.makeText(this,"Llega EndPinLost ",Toast.LENGTH_SHORT).show();
        Log.i("haur", "LLega onEndPOinLost" + s);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        conectar();
    }

    private void conectar() {
        String name = null;
        Nearby.Connections.sendConnectionRequest(mGoogleApiClient, name, endPointId, null, new Connections.ConnectionResponseCallback() {
            @Override
            public void onConnectionResponse(String s, Status status, byte[] bytes) {
                if(status.isSuccess()){
                    Toast.makeText(getApplicationContext(),"Conexion exitosa",Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(),"Conexion Fallida",Toast.LENGTH_SHORT).show();
                }
            }
        },this);
    }

    @Override
    public void onMessageReceived(String s, byte[] bytes, boolean b) {
        String msjR = null;
        try {
            msjR = new String(bytes,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String msj = "endPointIdOrigen: "+s+ "--msj: "+msjR+"--Esconfiable: "+b;
            txtRecibido.setText(msj);
    }

    @Override
    public void onDisconnected(String s) {

    }
}
