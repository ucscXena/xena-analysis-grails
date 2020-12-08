package org.xena.analysis

class Gmt {

  String name
  String hash
  String data

  static constraints = {
  }

  static mapping = {
    data type: 'text'
  }

  static hasMany = [
    results: Result,
  ]
}
