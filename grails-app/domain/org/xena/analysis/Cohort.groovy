package org.xena.analysis

class Cohort {


  String name
  String remoteUrl
  String localTpmFile
  String tpmUrl
//  Tpm tpm

  static hasMany = [
    results: Result,
    gmts: Gmt,
  ]

  static constraints = {
    name blank: false,nullable: false
    tpmUrl blank: false,nullable: true

//    remoteUrl
//    tpm nullable: true
  }

  static mapping = {
    data: 'text'
  }
}
