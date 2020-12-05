package org.xena.analysis

class Gmt {

  static constraints = {
  }

  Long id
  String name
  String hash
  String data

  static mapping = {
    data: 'text'
  }
}
