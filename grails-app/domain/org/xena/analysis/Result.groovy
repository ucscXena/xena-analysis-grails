package org.xena.analysis

class Result {



  String method
  Gmt gmt
  String gmtHash
  Cohort cohort
  String result
  String samples // delimited list of samples

  static constraints = {
    samples nullable: true, blank: false
  }

  static mapping = {
    result type:  'text'
    samples type:  'text'
  }

  List<String> getSampleArray(){
    return samples ? samples.split(",") as List<String> : new ArrayList<String>()
  }
}
