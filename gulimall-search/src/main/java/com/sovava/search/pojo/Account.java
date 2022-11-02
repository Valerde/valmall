package com.sovava.search.pojo;


import lombok.Data;

/**
 * bank账户信息
 */
@Data
public class Account {

    private int account_number;
    private int balance;
    private String firstname;
    private String lastname;
    private int age;
    private String gender;
    private String address;
    private String employer;
    private String email;
    private String city;
    private String state;


}
