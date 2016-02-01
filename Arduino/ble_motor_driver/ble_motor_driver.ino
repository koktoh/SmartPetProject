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

void goForward() {
  Serial.write("go forward\n");

  allLow();
  digitalWrite(5, HIGH);
  digitalWrite(9, HIGH);
}

void goBackward() {
  Serial.write("go backward\n");

  allLow();
  digitalWrite(6, HIGH);
  digitalWrite(10, HIGH);
}

void turnRight() {
  Serial.write("turn right\n");

  allLow();
  digitalWrite(5, HIGH);
  digitalWrite(10, HIGH);
}

void turnLeft() {
  Serial.write("turn left\n");

  allLow();
  digitalWrite(6, HIGH);
  digitalWrite(9, HIGH);
}

void loop() {
  while (ble_available()) {
    byte cmd;
    cmd = ble_read();

    switch (cmd) {
      case 'F':
        goForward();
        break;
      case 'B':
        goBackward();
        break;
      case 'R':
        turnRight();
        break;
      case 'L':
        turnLeft();
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
