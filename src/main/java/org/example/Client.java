package org.example;

public class Client {
    private int id;
    private String clientName;
    private String contactNumber;
    private String address;

    public Client(int id, String clientName, String contactNumber, String address) {
        this.id = id;
        this.clientName = clientName;
        this.contactNumber = contactNumber;
        this.address = address;
    }

    public int getId() { return id; }
    public String getClientName() { return clientName; }
    public String getContactNumber() { return  contactNumber; }
    public String getAddress() { return address; }

}