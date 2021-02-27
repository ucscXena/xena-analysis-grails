package org.xena.analysis

class AuthenticatedUser {

    static constraints = {
        email email: true,nullable: false,blank: false,unique: true
        firstName nullable: true
        lastName nullable: true
        role blank: false,nullable: false
    }

    String firstName
    String lastName
    String email
    RoleEnum role //

    static hasMany = [
            gmts:Gmt
    ]

}
