package org.xena.analysis

class AuthenticatedUser {

    static constraints = {
        email email: true,nullable: false,blank: false
        firstName nullable: true
        lastName nullable: true
    }

    String firstName
    String lastName
    String email

    static hasMany = [
            gmts:Gmt
    ]
}
