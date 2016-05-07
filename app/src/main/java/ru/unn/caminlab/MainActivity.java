package ru.unn.caminlab;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.UUID;
import java.util.Date;
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

    public  Button        on_button;
    public  Button        plus_temp;
    public  Button        minus_temp;
    public  Button        set_button;
    public  CheckBox      check_cam_status;
    public  TextView      temp_screen;
    public  EditText      set_temp;
    private NewThread     IO_Tread;
    private RequestThread R_Thread;
    private CheckThread   Ch_Thread;
    private String        currTemp = null;
    public ImageView      online;



    public final int ArduinoMessage = 666;
    public final int CaminOn = 111;

    private Handler h;
    private Handler ch;


    private static final int REQUEST_ENABLE_BT = 0;
    private boolean Is_Bluetooth_Enabled = false;
    private boolean ThreadQuit = false;
    private boolean isOn = false;

    public boolean IsConnect = false;
    public char degree = 176;

    final String LogPrefix = "****CaminLab**** ";
      private static String MacAdress = "20:16:01:06:43:24";        // Arduino
      //private static String MacAdress = "00:0B:0D:06:75:42";          // COMP
      private static final UUID ID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //-----------------------------------------------------------------------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //------------------------------------default-------------------------------
        super.onCreate(savedInstanceState);
        Log.d(LogPrefix, "<<< On Create >>>");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //-----------------------------------/default--------------------------------

        bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();
        on_button   = (Button)   findViewById(R.id.On_Button);
        temp_screen = (TextView) findViewById(R.id.Temp_Out);
        check_cam_status = (CheckBox) findViewById(R.id.Camin_Status);
        set_temp = (EditText) findViewById(R.id.Set_Temp);
        plus_temp = (Button) findViewById(R.id.Plus_Temp);
        minus_temp = (Button) findViewById(R.id.Minus_Temp);
        set_button = (Button) findViewById(R.id.Set_Button);
        online = (ImageView) findViewById(R.id.Online);

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

        check_cam_status.setChecked(false);  // Камин выключен

        //set_temp.setOnEditorActionListener(new View.);

        on_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d(LogPrefix, "Кнопка ВКЛ / ВЫКЛ нажата");
                if ((MacAdress != "00:00:00:00:00:00") && (IsConnect))
                {
                    Log.d(LogPrefix, "Попытаемся отправить команду 1");
                    IO_Tread.Bluetooth_send("1");
                    Log.d(LogPrefix, "Отправлены данные : 1");
                    Log.d(LogPrefix, "Проверяем статус камина");
                    CheckStatus();
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

        set_temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_temp.setCursorVisible(true);
            }
        });



        plus_temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (set_temp.getText().length()==0)
                {
                    float temp = Float.parseFloat(currTemp);
                    temp+=0.1;
                    set_temp.setText(String.valueOf(temp));
                    currTemp=new DecimalFormat("0.0").format(temp);
                }
                else
                {
                    float temp = Float.parseFloat(set_temp.getText().toString());
                    temp+=0.1;
                    set_temp.setText(String.valueOf(temp));
                    currTemp=new DecimalFormat("0.0").format(temp);
                }
            }
        });

        minus_temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (set_temp.getText().length()==0)
                {
                    float temp = Float.parseFloat(currTemp);
                    temp-=0.1;
                    set_temp.setText(String.valueOf(temp));
                    currTemp=new DecimalFormat("0.0").format(temp);
                }
                else
                {
                    float temp = Float.parseFloat(set_temp.getText().toString());
                    temp-=0.1;
                    set_temp.setText(String.valueOf(temp));
                    currTemp=new DecimalFormat("0.0").format(temp);
                }
            }
        });

        set_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });



        h = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case ArduinoMessage:
                        Log.d(LogPrefix, "$$$$ Пришедшая строка от Ардуино $$$$ -->"+ msg.obj.toString());
                        CommandParcer(msg.obj.toString());
                        break;
                }
            }
        };

        ch = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case CaminOn:
                        online.setImageResource(android.R.drawable.presence_online);
                        break;
                }
            }
        };
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(LogPrefix, "<<< On Resume >>>");

        ThreadQuit = false;
        online.setImageResource(android.R.drawable.presence_offline);

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
                    Log.d(LogPrefix, "Ошибка создания сокета " + e.getMessage());
                }

                bluetooth_adapter.cancelDiscovery();

                Log.d(LogPrefix, "Попытка установки соединения");

                try
                {
                    socket.connect();
                    Log.d(LogPrefix, "Соединение установлено");
                    online.setImageResource(android.R.drawable.presence_online);
                    IsConnect = true;
                }
                catch (IOException e)
                {
                    Log.d(LogPrefix, "Ошибка " + e.getMessage());
                    Log.d(LogPrefix, "Создаю поток опроса устройства");
                    Ch_Thread = new CheckThread(socket);
                    Ch_Thread.start();
                    /*try
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
                    }*/
                }


                if (IsConnect)
                {
                    Log.d(LogPrefix,"Запускаю отдельный поток ввода-вывода");
                    IO_Tread = new NewThread(socket);
                    IO_Tread.start();

                    Log.d(LogPrefix,"Запускаю отдельный поток опроса температуры");
                    R_Thread = new RequestThread(socket);
                    R_Thread.start();

                    Log.d(LogPrefix,"Запрашиваем статус камина");
                    CheckStatus();
                }
            }
            else
            {
                Log.d(LogPrefix,"Устройство "+MacAdress+" не обнаружено");
            }

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(LogPrefix, "<<< On Stop >>>");
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Log.d(LogPrefix, "<<< On Restart >>>");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LogPrefix, "<<< On Destroy >>>");
    }



    public void CheckStatus()
    {
        IO_Tread.Bluetooth_send("5"); // статус камина
    }


    /*public String GetTime()
    {
        return (DateFormat.getTimeInstance().format(Calendar.getInstance().getTime()) + " : ");
    }*/

    public void CommandParcer(String ArduinoData)
    {
        switch (ArduinoData.charAt(0))
        {
            case 't':
            {
                Log.d(LogPrefix, "<CommandParcer> Пришла температура от Arduino: "+ArduinoData.substring(1));
                temp_screen.setText(ArduinoData.substring(1) + " " + degree + "C");
                currTemp = ArduinoData.substring(1);
                break;
            }
            case 'c':
            {
                Log.d(LogPrefix, "<CommandParcer> Пришла команда от Arduino: "+ArduinoData.substring(1));
                break;
            }
            case 's':
            {

                Log.d(LogPrefix, "<CommandParcer> Пришел статус от Arduino: "+ArduinoData.substring(1));
                switch (ArduinoData.substring(1))
                {
                    case "0":
                    {
                        Log.d(LogPrefix, "<CommandParcer> Камин выключен");
                        check_cam_status.setChecked(false);
                        on_button.setText("Включить");
                        break;
                    }
                    case "1":
                    {
                        Log.d(LogPrefix, "<CommandParcer> Камин включен");
                        check_cam_status.setChecked(true);
                        on_button.setText("Выключить");
                        break;
                    }
                    default:
                    {
                        Log.d(LogPrefix, "<CommandParcer> Неизвестный статус " + ArduinoData);
                        break;
                    }
                }
                break;
            }
            default:
            {
                if (ArduinoData.charAt(0)!='-')
                    Log.d(LogPrefix,"<CommandParcer> Неизвестные данные:" + ArduinoData);
            }
        }
    }

    public void SyncTime()
    {
        Date date = new Date();
        long time = date.getTime();
    }

   /* public boolean IsTemp = false;
    public boolean IsCommand = false;
    public boolean IsStatus = false;

    public void CommandParcer(String ArduinoData)
    {
        switch (ArduinoData.charAt(0))
        {
            case 't':
            {
                Log.d(LogPrefix, "<CommandParcer> Пришла температура от Arduino");
                IsTemp=true;
                break;
            }
            case 'c':
            {
                Log.d(LogPrefix, "<CommandParcer> Пришла команда от Arduino");
                IsCommand = true;
                break;
            }
            case 's':
            {

                Log.d(LogPrefix, "<CommandParcer> Пришел статус от Arduino");
                IsStatus = true;
                break;
            }
            default:
            {
                if(IsTemp)
                {
                    Log.d(LogPrefix, "<CommandParcer> Температура: "+ArduinoData);
                    temp_screen.setText(ArduinoData + " " + degree + "C");
                    IsTemp=false;
                }
                if(IsCommand)
                {
                    Log.d(LogPrefix, "<CommandParcer> Команда: "+ArduinoData);
                    IsCommand=false;
                }
                if(IsStatus)
                {
                    Log.d(LogPrefix, "<CommandParcer> Статус: "+ArduinoData);
                    if(ArduinoData.charAt(0) == '0')
                    {
                        Log.d(LogPrefix, "<CommandParcer> Камин выключен");
                        check_cam_status.setChecked(false);
                    }
                    if(ArduinoData.charAt(0) == '1')
                    {
                        Log.d(LogPrefix, "<CommandParcer> Камин включен");
                        check_cam_status.setChecked(true);
                    }
                    IsStatus=false;
                }
                    //Log.d(LogPrefix,"<CommandParcer> Неизвестные данные:" + ArduinoData);
            }
        }
    }
    */
    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(LogPrefix, "<<< On Pause >>>");
        if (socket != null)
        {
            Log.d(LogPrefix,"Сокет был создан, попробую закрыть в OnPause");
            try
            {
                ThreadQuit = true;
                Log.d(LogPrefix, "Пытаюсь закрыть потоки IO / R");
                try
                {
                    IO_Tread.join();
                    Log.d(LogPrefix, "IO поток завершен");
                    R_Thread.join();
                    Log.d(LogPrefix, "R поток завершен");
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
                    String _message = new String(buffer, 0, bytes);
                    h.obtainMessage(ArduinoMessage, bytes, -1111, _message).sendToTarget();
                }
                catch (IOException e)
                {
                    Log.d(LogPrefix,"Ошибка "+e.getMessage());
                    break;
                }
            }
            Log.d(LogPrefix,"Вызвано принудительное завершение, конец IO потока");

        }

        public synchronized void Bluetooth_send(String _message)
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
                Bluetooth_send("2");
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

    //-----------------------------------------------------Новый поток Check------------------------------------------------------
    private class CheckThread extends Thread
    {
        private BluetoothSocket thread_socket = null;
        //private OutputStream OutStr = null;
       // private InputStream InStr = null;


        public CheckThread(BluetoothSocket _socket)
        {
            thread_socket = _socket;
        }

        public void run()
        {
            while(!IsConnect)
            {
                try
                {
                    Log.d(LogPrefix, "<CheckThread> Пытаюсь достучаться");
                    thread_socket.connect();
                    Log.d(LogPrefix, "<CheckThread> Соединение установлено");
                    ch.obtainMessage(CaminOn, -1, -1, -1).sendToTarget();
                    IsConnect = true;
                }
                catch (IOException e)
                {
                    Log.d(LogPrefix, "<CheckThread> Ошибка " + e.getMessage());
                    try
                    {
                        Log.d(LogPrefix, "<CheckThread> Заснули на 3 сек");
                        sleep(3000);
                    }
                    catch (InterruptedException e1)
                    {
                        Log.d(LogPrefix, "<CheckThread> Ошибка в засыпании потока опроса" + e1.getMessage());
                    }
                }
                if(IsConnect)
                {
                    {
                        Log.d(LogPrefix,"<CheckThread> Запускаю отдельный поток ввода-вывода");
                        IO_Tread = new NewThread(socket);
                        IO_Tread.start();

                        Log.d(LogPrefix,"<CheckThread> Запускаю отдельный поток опроса температуры");
                        R_Thread = new RequestThread(socket);
                        R_Thread.start();

                        Log.d(LogPrefix,"<CheckThread> Запрашиваем статус камина");
                        CheckStatus();
                    }
                }
            }
        }
    }
    //----------------------------------------------------/Новый поток Check------------------------------------------------------


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
