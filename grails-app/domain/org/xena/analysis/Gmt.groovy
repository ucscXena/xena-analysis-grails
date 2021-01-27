package org.xena.analysis

class Gmt {

  String name
  String method
  String hash
  String data
  int geneSetCount
  int availableTpmCount // count from defaultGeneSet on initial load

//  Double mean  // deprecated
//  Double variance // deprecated
  // use gene names as key
  String stats // { 'ABC':{mean: 0.11212, std: 0.272 },  'DEF': { mean:0.17, std:0.3 } }

  Boolean ready(){
    return availableTpmCount == getLoadedResultCount() && stats != null
  }

  int getLoadedResultCount(){
    return results ? results.size() : 0
  }

  static constraints = {
    name blank: false, unique: true
//    variance nullable: true
//    mean nullable: true
    stats nullable: true
//    hash blank: false, unique: true
//    data blank: false, unique: true
  }

  static mapping = {
    data type: 'text'
    stats type: 'text'
  }

  static hasMany = [
//    results: Result,
    results: TpmGmtResult,
  ]
}
