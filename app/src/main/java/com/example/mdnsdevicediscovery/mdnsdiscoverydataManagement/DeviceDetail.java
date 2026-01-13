package com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement;

public class DeviceDetail {

    private String name;
    private String ip;
    private boolean online;

    // Default constructor (Room requires)
    public DeviceDetail() {}

    // Constructor with parameters
    public DeviceDetail(String name, String ip, boolean online) {
        this.name = name;
        this.ip = ip;
        this.online = online;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return "Device{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", online=" + online +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DeviceDetail deviceDetail = (DeviceDetail) obj;
        return online == deviceDetail.online &&
                ip.equals(deviceDetail.ip) &&
                (name == null ? deviceDetail.name == null : name.equals(deviceDetail.name));
    }

}
