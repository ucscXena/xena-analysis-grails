package org.xena.analysis

class TpmGmtResult {


  String method
  Gmt gmt
  Cohort cohort
//  String url
  String gmtHash
  String localFile // pointer to file
  String result

  static constraints = {
  }

  static mapping = {
    result type:  'blob'
  }
}
