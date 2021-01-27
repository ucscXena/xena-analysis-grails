package org.xena.analysis

import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

class TpmStatGenerator {

//  https://www.johndcook.com/blog/standard_deviation/
  static TpmStatMap getGeneStatMap(File inputFile,TpmStatMap tpmStatMap = new TpmStatMap()) {

    boolean header = true
//    println "input tpmStatMap ${tpmStatMap}"
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

  static TpmStatMap getPathwayStatMap(JSONObject inputObject,TpmStatMap tpmStatMap = new TpmStatMap()) {

    JSONArray dataArray = inputObject.getJSONArray("data")
    // for each geneset
    for(int i = 0 ; i < dataArray.size() ; i++){
      JSONObject jsonObject = dataArray.getJSONObject(i)
      TpmStat tpmStat = new TpmStat()

      // for all of the data
      jsonObject.getJSONArray("data").collect {
        tpmStat.addStat( Double.parseDouble(it))
      }

      tpmStatMap.put(jsonObject.geneset,tpmStat)
    }
    return tpmStatMap
  }
}
