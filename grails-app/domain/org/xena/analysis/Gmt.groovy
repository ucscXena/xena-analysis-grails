package org.xena.analysis

class Gmt {

  String name
  String method
  String hash
  String data
  int geneSetCount
  int availableTpmCount // count from defaultGeneSet on initial load

  // use gene names as key
  String stats // { 'ABC':{mean: 0.11212, std: 0.272 },  'DEF': { mean:0.17, std:0.3 } }

  static constraints = {
    name blank: false, unique: true
    stats nullable: true
  }

  static mapping = {
    data type: 'text'
    stats type: 'text'
  }

  static hasMany = [
    results: TpmGmtResult,
  ]
}
