package org.xena.analysis

class Result {



  String method
  Gmt gmt
  String gmtHash
  Cohort cohort
  String result
  String samples // delimited list of samples

  static constraints = {
  }

  static mapping = {
    result type:  'text'
    samples nullable: true, blank: false
  }

  List<String> getSampleArray(){
    return samples ? samples.split(",") as List<String> : new ArrayList<String>()
  }
}
