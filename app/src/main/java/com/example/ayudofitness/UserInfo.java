package com.example.ayudofitness;

public class UserInfo {
    private final String address;
    private final String alias;
    private final int gender;
    private final int age;
    private final int height;
    private final int weight;
    private final int type;

    private byte[] data = new byte[20];

    public UserInfo(String address, String alias, int gender, int age, int height, int weight, int type) {
        this.address = address;
        this.alias = alias;
        this.gender = gender;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }
}
