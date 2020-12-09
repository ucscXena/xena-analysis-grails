package org.xena.analysis

class Result {



  String method
  Gmt gmt
  String gmtHash
  Cohort cohort
  String result

  static constraints = {
  }

  static mapping = {
    result type:  'text'
  }
}
