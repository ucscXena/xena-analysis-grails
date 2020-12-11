package org.xena.analysis

class CompareResult {

  String method
  Gmt gmt
  Cohort cohortA
  Cohort cohortB
  String samples


  String result

  static constraints = {
    samples nullable: true, blank: false
  }

  static mapping = {
    result type:  'text'
  }

  List<String> getSampleArray(){
    return samples ? samples.split(",") as List<String> : new ArrayList<String>()
  }

}
