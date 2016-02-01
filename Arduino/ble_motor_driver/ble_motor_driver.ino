#include <RBL_nRF8001.h>
#include <boards.h>
#include <SPI.h>
#include <EEPROM.h>

void setup() {
  ble_set_pins(3, 2);
  ble_begin();

  Serial.begin(57600);

  Serial.write("start setup\n");

  pinMode(5, OUTPUT);
  pinMode(6, OUTPUT);
  pinMode(9, OUTPUT);
  pinMode(10, OUTPUT);

  allLow();

  Serial.write("end setup\n");
}

void allLow() {
  Serial.write("all low\n");

  digitalWrite(5, LOW);
  digitalWrite(6, LOW);
  digitalWrite(9, LOW);
  digitalWrite(10, LOW);
}

void goForward(int i) {
  Serial.write("go forward\n");

  allLow();
  analogWrite(5, i);
  analogWrite(9, i);
}

void goBackward(int i) {
  Serial.write("go backward\n");

  allLow();
  analogWrite(6, i);
  analogWrite(10, i);
}

void turnRight(int i) {
  Serial.write("turn right\n");

  allLow();
  analogWrite(5, i);
  analogWrite(10, i);
}

void turnLeft(int i) {
  Serial.write("turn left\n");

  allLow();
  analogWrite(6, i);
  analogWrite(9, i);
}

void loop() {
  while (ble_available()) {
    byte cmd;
    cmd = ble_read();

    int data = ble_read() * 100 + ble_read() * 10 + ble_read();

    switch (cmd) {
      case 'F':
        goForward(data);
        break;
      case 'B':
        goBackward(data);
        break;
      case 'R':
        turnRight(data);
        break;
      case 'L':
        turnLeft(data);
        break;
      case 'S':
        allLow();
        break;
    }
  }

  if (!ble_connected()) {
    allLow();
  }

  ble_do_events();
}
