package org.xena.analysis

class Gmt {

  String name
  String method
  String hash
  String data

  static constraints = {
//    name blank: false, unique: true
//    hash blank: false, unique: true
//    data blank: false, unique: true
  }

  static mapping = {
    data type: 'text'
  }

  static hasMany = [
    results: Result,
  ]
}
