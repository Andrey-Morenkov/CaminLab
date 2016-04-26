package ru.unn.caminlab;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.*;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.UUID;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.os.Handler;

import ru.unn.caminlab.R;

public class MainActivity extends AppCompatActivity {

    //-----------------------------------------Переменные-------------------------------------------------
    public BluetoothAdapter bluetooth_adapter;
    public TextView log_screen;
    public TextView log_screen2;
    public Button on_button;
    public TextView temp_screen;
    public BluetoothDevice Camin;
    private BluetoothSocket socket;
    private NewThread IO_Tread;

    final int ArduinoMessage = 1;

    private Handler h;


    private static final int REQUEST_ENABLE_BT = 0;
    private boolean Is_Bluetooth_Enabled = false;

    public boolean IsTemp = false;
    public boolean IsCommand = false;
    public char degree = 176;


    private static String MacAdress = "20:16:01:06:43:24";
    private static final UUID ID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //-----------------------------------------------------------------------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //------------------------------------default-------------------------------
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //-----------------------------------/default--------------------------------

        bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        log_screen = (TextView) findViewById(R.id.Log_Screen);
        log_screen2 = (TextView) findViewById(R.id.Log_Screen2);
        on_button = (Button) findViewById(R.id.On_Button);
        temp_screen = (TextView) findViewById(R.id.Temp_Out);

        if (!bluetooth_adapter.isEnabled())
        {
            Intent bluetooth_enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bluetooth_enabler, REQUEST_ENABLE_BT);
            Is_Bluetooth_Enabled = true;
        }

        on_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                SetLogText(GetTime() + "Кнопка нажата ");
                if (MacAdress != "00:00:00:00:00:00")
                {
                    IO_Tread.Bluetooth_send("1");
                    SetLogText(GetTime() + "Отправлены данные: 1");
                }
                else
                    SetLogText(GetTime() + "Данные не отправлены");
            }
        });

        h = new Handler()
        {
            public void handleMessage(Message _message)
            {
                switch (_message.what)
                {
                    case ArduinoMessage:
                        byte[] readBuf = (byte[]) _message.obj;
                        String strIncom = new String(readBuf, 0, _message.arg1);
                        CommandParcer(strIncom);
                        SetLogText("Данные от Arduino: " + strIncom);
                        break;
                }
            }
        };
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (Is_Bluetooth_Enabled)
        {
            SetLogText(GetTime() + " : " + "Bluetooth enabled");
        }
        if (MacAdress == "00:00:00:00:00:00")
        {
            on_button.setBackgroundColor(Color.RED);
            on_button.setText("BAD MacAdress");
            SetLogText(GetTime() + "Неправильный MacAdress");
        }
        else
        {
            SetLogText(GetTime() + "Пытаемся соединиться с " + MacAdress);

            Camin = bluetooth_adapter.getRemoteDevice(MacAdress);

            if (Camin.getName() != null)
            {
                SetLogText(GetTime() + "Соединение успешно с " + Camin.getName());

                try
                {
                    socket = Camin.createRfcommSocketToServiceRecord(ID);
                    SetLogText(GetTime() + "Создали сокет");                               // Студия не дала сделать if
                }
                catch (IOException e)
                {
                    SetLogText(GetTime() + "Ошибка создания сокета " + e.getMessage());
                }

                SetLogText(GetTime() + "Устанавливается соединение");
                try
                {
                    socket.connect();
                    SetLogText(GetTime() + "Соединение установлено");
                }
                catch (IOException e)
                {
                    try
                    {
                        socket.close();
                        SetLogText(GetTime() + "Сокет закрыт");
                    }
                    catch (IOException e2)
                    {
                        SetLogText(GetTime() + "Ошибка закрытия сокета");
                    }
                }

                IO_Tread = new NewThread(socket);
                IO_Tread.start();

                on_button.setText("Выключить");
            }
            else
            {
                SetLogText(GetTime() + "Невозможно установить соединение с " + MacAdress);
            }

        }
    }


    public String GetTime()
    {
        return (DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + " : ");
    }

    public void SetLogText(String message)
    {
        if (log_screen.getText() != "NULL")
        {
            log_screen2.setText(log_screen.getText());
        }
        log_screen.setText(message);

    }

    public void CommandParcer(String ArduinoData)
    {
        switch (ArduinoData)
        {
            case "t":
                IsTemp = true;
                break;
            case "c":
                IsCommand = true;
                break;
            default:
                if(IsTemp)
                {
                    temp_screen.setText(ArduinoData+" "+degree+"C");
                    IsTemp=false;
                    break;
                }
                if(IsCommand)
                {
                    SetLogText("КАМИН ВКЛЮЧЕН СКА");
                    SetLogText("КАМИН ВКЛЮЧЕН СКА");
                    IsCommand=false;
                    break;
                }

        }



    }

    @Override
    public void onPause()
    {
        super.onPause();
        try     {
            socket.close();
            SetLogText("Сокет закрыт");
        } catch (IOException e2) {
            SetLogText("OnPause сокет не закрыт");
        }
    }



    //-----------------------------------------------------Новый поток------------------------------------------------------
    private class NewThread extends Thread {
        private BluetoothSocket thread_socket = null;
        private OutputStream OutStr = null;
        private InputStream InStr = null;


        public NewThread(BluetoothSocket _socket)
        {
            thread_socket = _socket;
            OutputStream tmp_outstream;
            InputStream tmp_instream;
            try
            {
                tmp_outstream = thread_socket.getOutputStream();
                tmp_instream = thread_socket.getInputStream();
                OutStr = tmp_outstream;
                InStr = tmp_instream;
            }
            catch (IOException e)
            {
                SetLogText(GetTime() + "Ошибка связки потоков " + e.getMessage());
            }
        }

        public void run() {
            byte[] buffer = new byte[100];
            int bytes;

            while (true)
            {
                try
                {
                    bytes = InStr.read(buffer);
                    h.obtainMessage(ArduinoMessage, bytes, -1, buffer).sendToTarget();
                }
                catch (IOException e)
                {
                    break;
                }

            }

        }

        public void Bluetooth_send(String _message)
        {
            byte[] message = _message.getBytes();

            SetLogText(GetTime() + "Отправляем данные:" + _message);
            try
            {
                OutStr.write(message);
            }
            catch (IOException e)
            {
                SetLogText(GetTime() + "Ош. отпр:" + e.getMessage());
            }
        }

        public Object status_OutStrem() {
            if (OutStr == null) {
                return null;
            } else {
                return OutStr;
            }
        }
    }
    //----------------------------------------------------/Новый поток------------------------------------------------------


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
