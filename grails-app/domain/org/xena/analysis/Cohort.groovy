package org.xena.analysis

class Cohort {


  String name
  Tpm tpm

  static hasMany = [
    results: Result,
    gmts: Gmt,
  ]

  static constraints = {
    name blank: false,nullable: false
    tpm nullable: true
  }

  static mapping = {
    data: 'text'
  }
}
