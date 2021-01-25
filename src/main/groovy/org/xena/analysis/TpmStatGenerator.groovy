package org.xena.analysis

class TpmStatGenerator {

//  https://www.johndcook.com/blog/standard_deviation/
  static TpmStatMap getGeneStatMap(File inputFile,TpmStatMap tpmStatMap = new TpmStatMap()) {

    boolean header = true
    println "input tpmStatMap ${tpmStatMap}"
    inputFile.splitEachLine("\t"){List<String> tokenLines ->
      if(!header) {
        String geneName = tokenLines[0]
        TpmStat tpmStat = tpmStatMap.get(geneName) ?: new TpmStat()
        tokenLines.subList(1, tokenLines.size()).each {
          tpmStat.addStat(Double.parseDouble(it))
        }
        tpmStatMap.put(geneName, tpmStat)
      }
      header = false
    }
    return tpmStatMap
  }
}
