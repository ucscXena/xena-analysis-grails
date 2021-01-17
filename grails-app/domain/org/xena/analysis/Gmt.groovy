package org.xena.analysis

class Gmt {

  String name
  String method
  String hash
  String data
  int geneCount
  int availableTpmCount // count from defaultGeneSet on initial load

  Double mean  // calculated when
  Double variance

  Boolean ready(){
    return availableTpmCount == getLoadedResultCount() && mean != null && variance != null
  }

  int getLoadedResultCount(){
    return results ? results.size() : 0
  }

  static constraints = {
    name blank: false, unique: true
//    hash blank: false, unique: true
//    data blank: false, unique: true
  }

  static mapping = {
    data type: 'text'
  }

  static hasMany = [
//    results: Result,
    results: TpmGmtResult,
  ]
}
