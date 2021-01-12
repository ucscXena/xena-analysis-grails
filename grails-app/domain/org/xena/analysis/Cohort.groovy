package org.xena.analysis

class Cohort {


  String name
  String remoteUrl
  String localFile
//  Tpm tpm

  static hasMany = [
    results: Result,
    gmts: Gmt,
  ]

  static constraints = {
    name blank: false,nullable: false
    remoteUrl
//    tpm nullable: true
  }

  static mapping = {
    data: 'text'
  }
}
