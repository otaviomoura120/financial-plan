package com.devhouse.financial_plan.domain;

public class Address {

    private String street;
    private String zipCode;
    private String country;
    private String neighborhood;
    private String state;
    private String city;

    public Address() {}

    public Address(String street, String zipCode, String country, String state, String city, String neighborhood) {
        this.street = street;
        this.zipCode = zipCode;
        this.country = country;
        this.state = state;
        this.city = city;
        this.neighborhood = neighborhood;
    }

    public String getStreet() { return street; }
    public String getZipCode() { return zipCode; }
    public String getCountry() { return country; }
    public String getState() { return state; }
    public String getCity() { return city; }
    public String getNeighborhood() { return neighborhood; }
}
