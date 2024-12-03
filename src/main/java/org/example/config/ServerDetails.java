package org.example.config;

public class ServerDetails {
    private int port;
    private String address;
    private int timeout;
    private int hostNumber;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getHostNumber() {
        return hostNumber;
    }

    public void setHostNumber(int hostNumber) {
        this.hostNumber = hostNumber;
    }
}