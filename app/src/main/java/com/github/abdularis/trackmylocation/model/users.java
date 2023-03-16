package com.github.abdularis.trackmylocation.model;

public class users {
    String useremail, username, usertype, useruid;

    public users(String useremail, String username, String usertype, String useruid) {
        this.useremail=useremail;
        this.username=username;
        this.usertype=usertype;
        this.useruid=useruid;
    }
    public users(){}

    public String getUseremail() {
        return useremail;
    }

    public String getUsername() {
        return username;
    }

    public String getUsertype() {
        return usertype;
    }

    public String getUseruid() {
        return useruid;
    }
}
