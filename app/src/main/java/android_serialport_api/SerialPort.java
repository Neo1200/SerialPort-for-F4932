package android_serialport_api;

import java.io.*;

public class SerialPort {
    static { System.loadLibrary("serial_port"); }

    private static native FileDescriptor open(String path, int baudrate, int flags);
    public native void close();

    private FileDescriptor mFd;
    private FileInputStream mInputStream;
    private FileOutputStream mOutputStream;

    public SerialPort(File device, int baudrate, int flags) throws IOException {
        mFd = open(device.getAbsolutePath(), baudrate, flags);
        if (mFd == null) throw new IOException("native open failed: " + device);
        mInputStream = new FileInputStream(mFd);
        mOutputStream = new FileOutputStream(mFd);
    }

    public InputStream getInputStream() { return mInputStream; }
    public OutputStream getOutputStream() { return mOutputStream; }
}