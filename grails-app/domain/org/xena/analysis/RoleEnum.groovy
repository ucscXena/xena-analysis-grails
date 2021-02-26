package org.xena.analysis

enum RoleEnum {

    USER("user",10),
    BANNED("banned",1),
    INACTIVE("inactive",5),
    ADMIN("admin",100);

    private String display; // pertains to the 1.0 value
    private Integer rank;

    RoleEnum(String display , int rank) {
        this.display = display;
        this.rank = rank;
    }


}
