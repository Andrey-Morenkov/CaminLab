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
import android.widget.Switch;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

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
    public  BluetoothAdapter bluetooth_adapter;
    public  BluetoothDevice Camin;
    private BluetoothSocket socket = null;

    public  Button on_button;
    public  Switch on_switch;
    public  TextView temp_screen;
    private NewThread IO_Tread;
    private RequestThread R_Thread;



    final int ArduinoMessage = 1;

    private Handler h;


    private static final int REQUEST_ENABLE_BT = 0;
    private boolean Is_Bluetooth_Enabled = false;
    private boolean ThreadQuit = false;

    public boolean IsTemp = false;
    public boolean IsCommand = false;
    public boolean IsConnect = false;
    public char degree = 176;

    final String LogPrefix = "****CaminLab**** ";
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
        on_button   = (Button)   findViewById(R.id.On_Button);
        temp_screen = (TextView) findViewById(R.id.Temp_Out);
        on_switch   = (Switch)   findViewById(R.id.On_Switch);

        if (!bluetooth_adapter.isEnabled())
        {
            Log.d(LogPrefix, "Bluetooth выключен, запрос на включение");
            Intent bluetooth_enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bluetooth_enabler, REQUEST_ENABLE_BT);
            Is_Bluetooth_Enabled = true;
        }
        else
        {
            Log.d(LogPrefix, "Bluetooth включен изначально");
            Is_Bluetooth_Enabled = true;
        }

        on_switch.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(LogPrefix, "Свитчер нажат");
                if ((MacAdress != "00:00:00:00:00:00") && (IsConnect))
                {
                    Log.d(LogPrefix, "Попытаемся отправить команду 1");
                    IO_Tread.Bluetooth_send("1");
                    Log.d(LogPrefix, "Отправлены данные : 1");
                    on_switch.setChecked(true);
                }
                else
                {
                    if (!IsConnect)
                        Log.d(LogPrefix, "Устройство не подключено, не могу отправить данные");
                    else
                        Log.d(LogPrefix, "MAC ADRESS не задан, не могу отправить");
                    on_switch.setChecked(true);
                }

            }
        });

        on_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(LogPrefix, "Кнопка ВКЛ нажата");
                if ((MacAdress != "00:00:00:00:00:00") && (IsConnect))
                {
                    Log.d(LogPrefix, "Попытаемся отправить команду 1");
                    IO_Tread.Bluetooth_send("1");
                    Log.d(LogPrefix, "Отправлены данные : 1");
                }
                else
                {
                    if (!IsConnect)
                        Log.d(LogPrefix, "Устройство не подключено, не могу отправить данные");
                    else
                        Log.d(LogPrefix, "MAC ADRESS не задан, не могу отправить");
                }

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
                        Log.d(LogPrefix, "Данные от Arduino" + strIncom);
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
            Log.d(LogPrefix, "Bluetooth включен");
        }
        if (MacAdress == "00:00:00:00:00:00")
        {
            on_button.setBackgroundColor(Color.RED);
            on_button.setText("BAD MacAdress");
            Log.d(LogPrefix, "MAC ADRESS не задан");
        }
        else
        {
            Log.d(LogPrefix, "Пытаемся обнаружить девайс "+ MacAdress);

            Camin = bluetooth_adapter.getRemoteDevice(MacAdress);

            if (Camin.getName() != null)
            {
                Log.d(LogPrefix, "Устройство обнаружено:" + Camin.getName());

                try
                {
                    socket = Camin.createRfcommSocketToServiceRecord(ID);
                    Log.d(LogPrefix, "Создали сокет");                               // Студия не дала сделать if
                }
                catch (IOException e)
                {
                    Log.d(LogPrefix, "Ошибка создания сокета "+ e.getMessage());
                }

                Log.d(LogPrefix, "Попытка установки соединения");
                try
                {
                    socket.connect();
                    Log.d(LogPrefix,"Соединение установлено");
                    IsConnect = true;
                }
                catch (IOException e)
                {
                    try
                    {
                        Log.d(LogPrefix,"Ошибка "+e.getMessage());
                        Log.d(LogPrefix,"Не смогли соединиться, пытаюсь закрыть сокет");
                        socket.close();
                        Log.d(LogPrefix,"Сокет закрыт");
                        socket = null;
                    }
                    catch (IOException e2)
                    {
                        Log.d(LogPrefix,"Ошибка закрытия сокета " +e2.getMessage());
                    }
                }

                if (IsConnect)
                {
                    Log.d(LogPrefix,"Запускаю отдельный поток ввода-вывода");
                    IO_Tread = new NewThread(socket);
                    IO_Tread.start();

                    Log.d(LogPrefix,"Запускаю отдельный поток опроса температуры");
                    R_Thread = new RequestThread(socket);
                    R_Thread.start();


                    on_button.setText("Выключить");
                }
            }
            else
            {
                Log.d(LogPrefix,"Устройство "+MacAdress+" не обнаружено");
            }

        }
    }


    /*public String GetTime()
    {
        return (DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + " : ");
    }*/

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
                    Log.d("Команда от ардуино TEST",ArduinoData);
                    IsCommand=false;
                    break;
                }

        }



    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(LogPrefix,"Приложение ушло в OnPause");
        if (socket != null)
        {
            Log.d(LogPrefix,"Сокет был создан, попробую закрыть в OnPause");
            try
            {
                ThreadQuit = true;
                Log.d(LogPrefix, "Пытаюсь закрыть поток IO");
                try
                {
                    IO_Tread.wait();
                    R_Thread.wait();
                }
                catch (InterruptedException e)
                {
                    Log.d(LogPrefix, "Не удалось закрыть поток IO / R "+e.getMessage());
                }

                socket.close();
                Log.d(LogPrefix,"Закрыли сокет в OnPause");
                socket = null;
            }
            catch (IOException e2)
            {
                Log.d(LogPrefix,"Сокет не закрыт в OnPause " + e2.getMessage());
            }
        }
        else
            Log.d(LogPrefix,"Сокет не был создан, закрывать в OnPause нечего");

    }


    //-----------------------------------------------------Новый поток IO------------------------------------------------------
    private class NewThread extends Thread
    {
        private BluetoothSocket thread_socket = null;
        private OutputStream OutStr = null;
        private InputStream InStr = null;


        public NewThread(BluetoothSocket _socket)
        {
            thread_socket = _socket;
            try
            {
                OutStr = thread_socket.getOutputStream();
                InStr = thread_socket.getInputStream();
                Log.d(LogPrefix,"Выделили IO потоки");
            }
            catch (IOException e)
            {
                Log.d(LogPrefix,"Ошибка выделения IO потоков "+e.getMessage());
            }
        }

        public void run()
        {
            byte[] buffer = new byte[100];
            int bytes;

            while (!ThreadQuit)
            {
                try
                {
                    bytes = InStr.read(buffer);
                    h.obtainMessage(ArduinoMessage, bytes, -1, buffer).sendToTarget();
                }
                catch (IOException e)
                {
                    Log.d(LogPrefix,"Ошибка "+e.getMessage());
                    break;
                }
            }
            Log.d(LogPrefix,"Вызвано принудительное завершение, конец IO потока");

        }

        public synchronized void Bluetooth_send(String _message)                          // можно сделать с семафорами
        {
            byte[] message = _message.getBytes();

            Log.d(LogPrefix, "Пытаюсь отправить данные "+ _message +" по Bluetooth");
            try
            {
                OutStr.write(message);
                Log.d(LogPrefix, " Данные "+_message+" отправлены");
            }
            catch (IOException e)
            {
                Log.d(LogPrefix, " Ошибка отправки данных "+ e.getMessage());
            }
        }

        /*public Object status_OutStr()
        {
            if (OutStr == null)
            {
                return null;
            }
            else
            {
                return OutStr;
            }
        }*/
    }
    //----------------------------------------------------/Новый поток IO------------------------------------------------------

    //-----------------------------------------------------Новый поток R------------------------------------------------------
    private class RequestThread extends Thread
    {
        private BluetoothSocket thread_socket = null;
        private OutputStream OutStr = null;
        private int QuitLevel = 0;

        public RequestThread(BluetoothSocket _socket)
        {
            thread_socket = _socket;
            try
            {
                OutStr = thread_socket.getOutputStream();
                Log.d(LogPrefix, " Выделили Request поток");
            }
            catch (IOException e)
            {
                Log.d(LogPrefix, " Ошибка выделения Request потока "+e.getMessage());
            }
        }

        public void run()
        {
            while ((QuitLevel != 5) && (!ThreadQuit))
            {
                Bluetooth_send("111");
                try
                {
                    Log.d(LogPrefix, " Заснули на 5 сек");
                    sleep(5000);
                }
                catch (InterruptedException e1)
                {
                    Log.d(LogPrefix, " Ошибка в засыпании потока опроса" + e1.getMessage());
                    QuitLevel++;
                }
            }
            if (QuitLevel == 5)
                Log.d(LogPrefix, "Превышено количество неудачных опросов температуры, конец потока");
            else
                Log.d(LogPrefix, "Вызвано принудительное завершение, конец R потока");

        }

        public synchronized void Bluetooth_send(String _message)                                         // можно сделать с семафорами
        {
            byte[] message = _message.getBytes();

            Log.d(LogPrefix," Пытаюсь отправить запрос температуры по Bluetooth");
            try
            {
                OutStr.write(message);
                Log.d(LogPrefix, " Запрос на температуру отправлен");
            }
            catch (IOException e)
            {
                Log.d(LogPrefix," Ошибка отправки запроса на температуру "+ e.getMessage());
            }
        }
    }
    //----------------------------------------------------/Новый поток R------------------------------------------------------


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
