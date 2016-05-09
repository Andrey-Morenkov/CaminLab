int Relay = 4;
int incomingByte;
int Status = 0;
float Temp;
char str[8];
int tmp1;
int tmp2;
float stat[96];
int Time_Counter = 0;
int Last_stat = 0;
int Target_temp;

String cmd = String(5);
String data = String(5);
String message = String(16);


void Prepare_to_save(void)
{
  for (int i = 0;i<96;i++)
  stat[i]=-1;
}

void Save_Stat(int Time_Counter,int Last_stat) // УКАЗАТЕЛЬ НА Last_stat
{
  if (Time_Counter == 900)
  {
    stat[Last_stat] = Temp;
    Last_stat++;                       //МЕНЯТЬ ПО УКАЗАТЕЛЮ
    if(Last_stat == 97) Last_stat = 0; // МЕНЯТЬ ПО УКАЗАТЕЛЮ
  }
}

void Get_stat(void)
{
  for (int i = 0;i<96;i++)
  Serial.println(stat[i]);
}

void setup() {
   
   Time_Counter = 0;
   Last_stat = 0;
   Serial.begin(9600);
   pinMode(Relay, OUTPUT);
   digitalWrite(Relay, HIGH);
   str[4] = '5';
   str[3] = '4';
   str[2] = '3';
   str[1] = '2';
   str[0] = '1';
   Status = 0;
}

void loop() 
{

tmp1 = random(-100,101);
tmp2 = random(1,15);
Temp = tmp1/tmp2;
delay(1000);
Time_Counter++;
Save_Stat(Time_Counter,Last_stat);

if (Serial.available() > 0) 
        {
        incomingByte = Serial.read();


        
        
        if (incomingByte == 49) //1 вкл
        {
        //  Serial.println("ON/OFF");

          if (!Status) 
          {
            digitalWrite(Relay, LOW);
            Status = 1;
            delay(1000);
            Time_Counter++;
          }
          else
          {
            digitalWrite(Relay, HIGH);
            Status = 0;
            delay(1000);
            Time_Counter++;
          }
        }
        
        if (incomingByte == 50)  //2 температура
        {
        //  Serial.println("TEMP");
        
        /*  Serial.println(str[1]);
          delay(50);
          Serial.println(Temp, 2);
          delay(50);
          */
          cmd = "t";
          data = (String)Temp;
          message = cmd + data;
          Serial.print("-");
          Serial.println(message);
        }
  
if (incomingByte == 51) //3 
{ 
int mnoj = 0; 
int ost = 0; 
int result = 0; 

incomingByte = Serial.read(); 
while (incomingByte < 0)
{ 
delay(100); 
incomingByte = Serial.read(); 
Serial.println("wating input"); 
} 
mnoj = (int)incomingByte; 

incomingByte = Serial.read(); 
while (incomingByte < 0)
{ 
delay(100); 
incomingByte = Serial.read(); 
Serial.println("wating input"); 
} 
mnoj = (int)incomingByte;; 
ost = (int)incomingByte; 
result = mnoj * 256 + ost; 
Serial.print("-"); 
Serial.println(result); 
}

        if (incomingByte == 52)  //4 заглушка
        {
        //  Serial.println("SET TEMP");
          Target_temp = Serial.read();
          
        }

        if (incomingByte == 53)  //5 статус
        {
        //  Serial.println("STATUS IS");
        /*  Serial.println(str[5]);
          delay(50);
          Serial.println(Status,2);
          delay(50);
          */
          cmd = "s";
          data = (String)Status;
          message = cmd + data;
          Serial.print("-");
          Serial.print(message);
        }
        
        if (incomingByte == 54) //6 температура
        { 
        float cash; 
        Serial.print("-"); 
        cash = (float)Serial.read(); 
        Serial.println (cash); 
        }
    }
}
