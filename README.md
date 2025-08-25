# Octopus Cards Tech Machine3 Vending Machine Controller

This project provides an Android application to control and interact with a vending machine via serial port communication.

## Overview
The app interfaces with a vending machine hardware using the `SerialPortController` class, allowing users to:
- Query machine status (e.g., payment info, slot info, temperature).
- Perform operations (e.g., select goods, process payments, control lights and HVAC).
- Manage menu settings and one-tap vending setups.

## Setup
1. Ensure the device has a serial port (e.g., `/dev/ttymxc2`) and the `libserial_port.so` library is available in the `jniLibs` directory.
2. Build the app with the provided `MainActivity.java`, `SerialPortController.java`, `Protocol.java`, and `SerialPortNative.kt` files.
3. Install the APK on an Android device with appropriate permissions for serial port access.

## Usage
- Use the UI elements (spinners, edit texts, buttons) to input parameters and trigger commands.
- Logs are displayed in the `tvLog` TextView for debugging and monitoring.
- Refresh ports or open/close the serial connection as needed.

## Main Functions
- **Query Status**: Use buttons like `btnB` (payment info), `btnC` (slot info), `btnD` (temperature) to retrieve machine data via `Protocol` commands.
- **Goods Selection**: Select a slot with `btnSelect` (using `etSlot`) and confirm with `btnOK` or cancel with `btnCancel` using `Protocol.selectGoods`, `selectOk`, `selectCancel`.
- **Payment Processing**: Initiate shipping with `btnShip` using `etPrice`, `etSlot`, `etPayMode`, and `etCard` via `Protocol.ship`.
- **Environmental Control**: Toggle lights (`btnLightOn`, `btnLightOff`) and HVAC (`btnCool`, `btnHeat`, `btnStop`) with corresponding `Protocol` commands.
- **Menu Management**: Read/write menu settings with `btnO` and `btnP` using `etOMenu` and `etPData` via `Protocol.readMenu` and `writeMenu`.
- **One-Tap Setup**: Configure vending with `btnSetupVend` or check flags with `btnCheckVendFlags` using predefined `Protocol` sequences.

## Dependencies
- Android SDK
- `android_serialport_api` library
- Native library `libserial_port.so` for ARM architectures

## Notes
- The app scans for serial ports under `/dev` and `/sys/class/tty`.
- Ensure proper baud rate (default 115200) and slot configurations match the vending machine protocol.
- Error handling is implemented for invalid inputs and serial port issues.

