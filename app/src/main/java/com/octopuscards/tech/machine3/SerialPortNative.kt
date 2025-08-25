package com.octopuscards.tech.machine3

class SerialPortNative {
    companion object { init { System.loadLibrary("serial_port") } }

    external fun open(path: String, baudrate: Int, dataBits: Int, stopBits: Int, parity: Int, flags: Int): Int
    external fun close(): Int
    external fun write(buffer: ByteArray, length: Int): Int
    external fun read(buffer: ByteArray, length: Int): Int
}