package org.xena.analysis

import grails.converters.JSON
import grails.testing.services.ServiceUnitTest
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import spock.lang.Ignore
import spock.lang.Specification

class AnalysisServiceSpec extends Specification implements ServiceUnitTest<AnalysisService>{

    def setup() {
    }

    def cleanup() {
    }

    void "extract gene set names"() {
        expect:"gene set names to be extracted"
        println new File(".").absolutePath
        File file = new File("./src/test/data/resultA.json")

        println file.exists()
        def jsonObject = new JSONObject(file.text)
        def genes = jsonObject.data.collect{ it.geneset}
        assert genes.size()==9
        assert genes[0]=="Notch signaling (GO:0007219)"
        assert genes[8]=="Nucleotide excision repair (GO:0006281)"

//        def genes = service.getGeneSetNames(new JSONObject(file.text))
        println "genes: ${genes}"
    }

  void "convert string array to a float array"(){
    expect:
    def input = [ "0.22332","5.2323424","-2.23234324"]
    def output = input.collect{ Float.parseFloat(it) }
    println output
  }

  void "combine arrays"(){
    expect:
    def a = ["a","a1"]
    def b = ["b","b1"]
    def c = [a,b].flatten()
    assert c == ["a","a1","b","b1"]
  }


  void "get data statistics"(){

    given:
    def input = new JSONArray(new File("src/test/data/fullInputDataSet.json").text)
    assert input.length()==2
    assert input[0].length()==9
    assert input[0][0].length()==89
    assert input[1].length()==9
    assert input[1][0].length()==548

    when:
    def values = AnalysisService.getDataStatisticsPerGeneSet(input)

    then:
    assert values.size()==9
    assert Math.abs(values[0].mean - 4.023820362794348) < 0.00001
    assert Math.abs(values[0].variance - 0.0464846114492264) < 0.00001
    assert Math.abs(values[5].mean - 2.0414515651491367) < 0.00001
    assert Math.abs(values[5].variance - 0.05060722132448916) < 0.00001
    assert Math.abs(values[8].mean - 4.231456808634223) < 0.00001
    assert Math.abs(values[8].variance - 0.012692699004756416) < 0.00001

  }

  void "small data statistics"(){

    given:
    def input = new JSONArray(new File("src/test/data/smallInputData.json").text)
    assert input.length()==2
    assert input[0].length()==2
    assert input[0][0].length()==3
    assert input[1].length()==2
    assert input[1][0].length()==3

    when:
    def values0 = AnalysisService.getValuesForIndex(input,0)
    def values1 = AnalysisService.getValuesForIndex(input,1)
    println "values 0 $values0"
    println "values 1 $values1"

    then:
    assert values0==[3,4,5,7,-2,3]
    assert values1==[-1,-2,-3,-8,-9,-10]

    when:
    def values = AnalysisService.getDataStatisticsPerGeneSet(input)
    println values

    then:
    assert values.size()==2
    assert Math.abs(values[0].mean - (3 + 4 + 5 + 7 -2 +3 ) / 6.0) < 0.01
    assert Math.abs(values[0].variance - 9.06666666) < 0.01
    assert Math.abs(values[1].mean - (-1-2-3 -8-9-10) / 6.0) < 0.01
    assert Math.abs(values[1].variance - 15.5) < 0.01

  }

  void "get values for index"(){
    given:
    def input = new JSONArray(new File("src/test/data/fullInputDataSet.json").text)
//    def inputValues = [inputValuesA,inputValuesB]

    when:
    def values = AnalysisService.getValuesForIndex(input,0)

    then:
    assert values.size() == 89 + 548

  }

  void "input values"(){
    expect:
    def input = new JSONArray(new File("src/test/data/inputDataStats.json").text)
    def dataStatistics = new JSONArray(new File("src/test/data/dataDats.json").text) as List
    def values = AnalysisService.getZSampleScores(input,dataStatistics)
    assert input.size()==9
    assert values.size()==9
    assert input[0].size()==89
    assert values[0].size()==89
    assert Math.abs(values[0][0]+15.37768369837939) < 0.0001
    assert Math.abs(values[0][1]+33.00384044872373) < 0.0001
    assert Math.abs(values[0][88]+32.813116629986624) < 0.0001

  }

  void "calc z pathway scores"(){
    expect:
    def input = new JSONArray(new File("src/test/data/inputPathwaySampleScores.json").text) as List
    def values = AnalysisService.getZPathwayScoresForCohort(input)
    assert values.size()==50
  }

  void "sample z-scores"(){

    expect:
    def input = new JSONArray(new File("src/test/data/sampleZScores.json").text)
    assert input.size()==2
    assert input[0].size()==50
    assert input[0][0].size()==89
    assert input[1].size()==50
    assert input[1][0].size()==548
    def values = AnalysisService.getZPathwayScores(input)
    assert values.size()==2
    assert values[0].size()==50
    assert values[1].size()==50

  }

//  void "handle gmt data"(){
//
//    expect:
//    def gmtData = new File("src/test/data/gmtData.gmt").text
//    def meanMap = new JSONObject(new File("src/test/data/meanMap.json").text) as Map
//    JSONArray outputArray = AnalysisService.generateResult(gmtData,meanMap)
//    assert outputArray.length()==9
//
//  }

  void "generate new filename"(){

    given:
    String inputFileName = "/Users/nathandunn/repositories/XENA/xena-analysis-grails/data/tpm/TCGA_Ovarian_Cancer__OV_.tpm.gz"
    String sampleHash = "sampleHash"
    File file = new File(inputFileName)

    when:
    String outputFileName = AnalysisService.getNewFileName(file,sampleHash)

    then:
    assert outputFileName == "/Users/nathandunn/repositories/XENA/xena-analysis-grails/data/tpm/TCGA_Ovarian_Cancer__OV_${sampleHash}.tpm.gz"

  }

  void "filter tpm file"(){

    given:
    def inputTpmFile = new File("src/test/data/inputTpmFile.tpm")
    def expectedTpmFile = new File("src/test/data/filteredTpmFile.tpm")
    JSONArray samplesArray = new JSONArray("['TCGA-FA-8693-01','TCGA-G8-6909-01','TCGA-VB-A8QN-01']")

    when:
    String testFilteredText = AnalysisService.filterTpmForSamples(inputTpmFile,samplesArray)
    String expectedFileText = expectedTpmFile.text
    println "test filtered text"
    println testFilteredText

    println "expected filtered text"
    println expectedFileText

    then:

    List<String> expectedLines = expectedTpmFile.readLines()
    def testLines = testFilteredText.split("\n") as List<String>

    assert expectedLines.size()== testLines.size()
    expectedLines.eachWithIndex { String entry, int i ->
      assert expectedLines[i]== testLines[i]
    }

  }


  void "get mean and total data from gmt / tpm output file"(){
    given:
    String inputString = "{\"data\":[{\"geneset\":\"Direct reversal of DNA damage (GO:0006281)\",\"data\":[\"2.09239545970997\",\"1.97132727825758\",\"1.60032397375245\",\"1.89290704983659\",\"2.05232326772132\",\"2.09796860838949\",\"1.79190256521988\",\"2.14448933629716\",\"1.74457578177277\",\"1.99359188540912\",\"2.01853895035876\",\"1.92537832642487\",\"2.10943578829161\",\"1.99813233782849\",\"2.04960459374312\",\"2.0730528883914\",\"1.97383878176961\",\"2.19762710994431\",\"1.83273959655064\",\"2.15407722970703\",\"2.12125498735962\",\"1.94958071914254\",\"1.8791965675884\",\"1.8377030533969\",\"1.72201033582089\",\"1.84135428974202\",\"1.82405237420156\",\"1.8579727638253\",\"2.32120082134546\",\"1.57792074231198\",\"1.86989296636481\",\"1.58548846374016\",\"2.10565992595919\",\"2.1972910121179\",\"2.03653369215717\",\"1.85088082024206\",\"2.10406881046339\",\"1.91525776548913\",\"2.14120965833188\",\"1.84221728419151\",\"1.83489431597673\",\"2.09477656066847\",\"1.73671448393877\",\"1.96327174528387\",\"1.8638132168861\",\"2.06735435553049\",\"1.99950398763171\",\"2.06484819423504\",\"1.75531340340595\",\"1.95142979370068\",\"2.07275105539119\",\"2.06405948797351\",\"1.94236154846873\",\"2.34306666743437\",\"2.31874027543505\",\"2.114824495813\",\"2.00340892217389\",\"2.5718342713317\",\"1.72543865837037\",\"2.09954437244427\",\"1.89520558361076\",\"2.09495736473171\",\"2.04562053735889\",\"1.8702735779412\",\"2.0646924876297\",\"1.98716754566897\",\"1.83043297810821\",\"1.85314075615717\",\"1.88457599664042\",\"1.97214888486011\",\"1.76869311197387\",\"1.84656911588848\",\"1.90923466103609\",\"2.09628941476721\",\"1.89918885587281\",\"1.96995277092846\",\"1.89002923425366\",\"1.98567588576069\",\"1.81856205189438\"]}],\"samples\":[\"TCGA-OR-A5J1-01\",\"TCGA-OR-A5J2-01\",\"TCGA-OR-A5J3-01\",\"TCGA-OR-A5J5-01\",\"TCGA-OR-A5J6-01\",\"TCGA-OR-A5J7-01\",\"TCGA-OR-A5J8-01\",\"TCGA-OR-A5J9-01\",\"TCGA-OR-A5JA-01\",\"TCGA-OR-A5JB-01\",\"TCGA-OR-A5JC-01\",\"TCGA-OR-A5JD-01\",\"TCGA-OR-A5JE-01\",\"TCGA-OR-A5JF-01\",\"TCGA-OR-A5JG-01\",\"TCGA-OR-A5JI-01\",\"TCGA-OR-A5JJ-01\",\"TCGA-OR-A5JK-01\",\"TCGA-OR-A5JL-01\",\"TCGA-OR-A5JM-01\",\"TCGA-OR-A5JO-01\",\"TCGA-OR-A5JP-01\",\"TCGA-OR-A5JQ-01\",\"TCGA-OR-A5JR-01\",\"TCGA-OR-A5JS-01\",\"TCGA-OR-A5JT-01\",\"TCGA-OR-A5JV-01\",\"TCGA-OR-A5JW-01\",\"TCGA-OR-A5JX-01\",\"TCGA-OR-A5JY-01\",\"TCGA-OR-A5JZ-01\",\"TCGA-OR-A5K0-01\",\"TCGA-OR-A5K1-01\",\"TCGA-OR-A5K2-01\",\"TCGA-OR-A5K3-01\",\"TCGA-OR-A5K4-01\",\"TCGA-OR-A5K5-01\",\"TCGA-OR-A5K6-01\",\"TCGA-OR-A5K8-01\",\"TCGA-OR-A5K9-01\",\"TCGA-OR-A5KO-01\",\"TCGA-OR-A5KT-01\",\"TCGA-OR-A5KU-01\",\"TCGA-OR-A5KV-01\",\"TCGA-OR-A5KW-01\",\"TCGA-OR-A5KX-01\",\"TCGA-OR-A5KY-01\",\"TCGA-OR-A5KZ-01\",\"TCGA-OR-A5L3-01\",\"TCGA-OR-A5L4-01\",\"TCGA-OR-A5L5-01\",\"TCGA-OR-A5L6-01\",\"TCGA-OR-A5L8-01\",\"TCGA-OR-A5L9-01\",\"TCGA-OR-A5LA-01\",\"TCGA-OR-A5LB-01\",\"TCGA-OR-A5LC-01\",\"TCGA-OR-A5LD-01\",\"TCGA-OR-A5LE-01\",\"TCGA-OR-A5LG-01\",\"TCGA-OR-A5LH-01\",\"TCGA-OR-A5LJ-01\",\"TCGA-OR-A5LK-01\",\"TCGA-OR-A5LL-01\",\"TCGA-OR-A5LM-01\",\"TCGA-OR-A5LN-01\",\"TCGA-OR-A5LO-01\",\"TCGA-OR-A5LP-01\",\"TCGA-OR-A5LR-01\",\"TCGA-OR-A5LS-01\",\"TCGA-OR-A5LT-01\",\"TCGA-OU-A5PI-01\",\"TCGA-P6-A5OF-01\",\"TCGA-P6-A5OG-01\",\"TCGA-PA-A5YG-01\",\"TCGA-PK-A5H8-01\",\"TCGA-PK-A5H9-01\",\"TCGA-PK-A5HA-01\",\"TCGA-PK-A5HB-01\"]}"

    when:
    JSONObject inputObject = JSON.parse(inputString)
//    println inputObject.toString(2)
    List<Double> dataValues = inputObject.getJSONArray("data").getJSONObject(0).getJSONArray("data").collect{
      Double.parseDouble(it)
    }
//    println "data values: ${dataValues}"
    double sum = dataValues.sum()
    long count = dataValues.size()
    double mean = sum / count
    println "sum: $sum"
    println "count: $count"
    println "mean: $mean"

    then:
    assert sum == 155.5653384903447
    assert count == 79
    assert mean == sum / count


    when:
    TpmStatMap tpmStatMap = TpmStatGenerator.getPathwayStatMap(inputObject)
    assert tpmStatMap.size()==1
    Map.Entry<String,TpmStat> tpmStatEntry = tpmStatMap.iterator().next()
    TpmStat tpmStat = tpmStatEntry.value

    then:
    assert tpmStatEntry.key =='Direct reversal of DNA damage (GO:0006281)'
    assert tpmStat.numDataValues()==79
    assert Math.abs(tpmStat.mean()-mean)< 0.0000001
    assert tpmStat.standardDeviation()==0.17154089594970198
    assert tpmStat.variance()==0.029426278983226483

  }

  void "handle 2 tpmstats"(){
    given: "2 input files "
    // -3,-5,2 AND 7, 7, -3
//    String inputStringA = "{ \"data\": [{ \"geneset\": \"Direct reversal of DNA damage (GO:0006281)\", \"data\": [ \"-3\", \"-5\", \"2\" ] }], \"samples\": [ \"TCGA-OR-A5Jc1-01\", \"TCGA-OR-A5Jb2-01\", \"TCGA-OR-A5J3-0a1\" ] }\n"
    String inputStringA = "{ \"data\": [ { \"geneset\": \"Direct reversal of DNA damage (GO:0006281)\", \"data\": [ \"-3\", \"-5\", \"2\" ] }, { \"geneset\": \"GeneSet B (GO:0006283)\", \"data\": [ \"7\", \"7\", \"-3\" ] } ], \"samples\": [ \"TCGA-OR-A5Jc1-01\", \"TCGA-OR-A5Jb2-01\", \"TCGA-OR-A5J3-0a1\" ] }"
    JSONObject inputObjectA = JSON.parse(inputStringA)
//    println "input object A"
//    println inputObjectA.toString(2)
    // 3, 4, 5, 6   AND   3,3,8,6
    String inputStringB = "{ \"data\": [ { \"geneset\": \"Direct reversal of DNA damage (GO:0006281)\", \"data\": [ \"3\", \"4\", \"5\", \"6\" ] }, { \"geneset\": \"GeneSet B (GO:0006283)\", \"data\": [ \"3\", \"3\", \"8\", \"6\" ] } ], \"samples\": [ \"TCGA-OR-A5J1-01\", \"TCGA-OR-A5J2-01\", \"TCGA-OR-A5J3-01\", \"TCGA-OR-A5J5-01\" ]} "
    JSONObject inputObjectB = JSON.parse(inputStringB)

    when: "we try to calculate 1 stats"
    TpmStatMap tpmStatMap = TpmStatGenerator.getPathwayStatMap(inputObjectA)
    TpmStat tpmStat = tpmStatMap.get("Direct reversal of DNA damage (GO:0006281)")
    TpmStat tpmStat2 = tpmStatMap.get("GeneSet B (GO:0006283)")

    then:
    tpmStatMap.size()==2
    tpmStat.numDataValues()==3
    tpmStat.mean()==-2
    tpmStat.standardDeviation()==3.605551275463989
    tpmStat2.numDataValues()==3
    tpmStat2.mean()==3.6666666666666665
    tpmStat2.standardDeviation()==5.773502691896257


    when: "we add the other one "
    tpmStatMap = TpmStatGenerator.getPathwayStatMap(inputObjectB,tpmStatMap)
    tpmStat = tpmStatMap.get("Direct reversal of DNA damage (GO:0006281)")
    tpmStat2 = tpmStatMap.get("GeneSet B (GO:0006283)")

    then:
    tpmStatMap.size()==2
    tpmStat.numDataValues()==7
    tpmStat.mean()==1.7142857142857142
    tpmStat.standardDeviation()==4.15187851918806
    tpmStat2.numDataValues()==7
    tpmStat2.mean()==4.428571428571428
    tpmStat2.standardDeviation()==3.8234863173611093
  }

  void "process for multiple output"(){
    given:
    String inputPath = "src/test/data/resultA.json"
    File inputFile = new File(inputPath)
    JSONObject inputObject = JSON.parse(inputFile.text)

    when:
    TpmStatMap tpmStatMap = TpmStatGenerator.getPathwayStatMap(inputObject)
    assert tpmStatMap.size()==9
    Map.Entry<String,TpmStat> tpmStatEntry = tpmStatMap.iterator().next()
    TpmStat tpmStat = tpmStatEntry.value

    then:
    assert tpmStatEntry.key =='DNA base excision repair (GO:0006281)'
    assert tpmStat.numDataValues()==379
    assert tpmStat.mean()==3.899832197820581
    assert tpmStat.standardDeviation()==0.21746447738787406
    assert tpmStat.variance()==0.04729079892558119

    when: "we see if we can re-hydrate it"
    def tpmStatMapObject = JSON.parse(tpmStatMap.toString()) as JSONObject
    TpmStatMap tpmStatMapCopy = new TpmStatMap(tpmStatMapObject)

    then:
    assert tpmStatMap.size()==9
    Map.Entry<String,TpmStat> tpmStatEntry2 = tpmStatMap.iterator().next()
    TpmStat tpmStat2 = tpmStatEntry2.value
    assert tpmStatEntry.key =='DNA base excision repair (GO:0006281)'
    assert tpmStat2.numDataValues()==379
    assert tpmStat2.mean()==3.899832197820581
    assert tpmStat2.standardDeviation()==0.21746447738787406
    assert tpmStat2.variance()==0.04729079892558119



  }


  void "get mean, variance, and  from 1 file"(){

    given:
    File tpmFile1 = new File("src/test/data/sampleFile1.tpm")

    when:
    TpmStatMap tpmStatMap = TpmStatGenerator.getGeneStatMap(tpmFile1)
    println tpmStatMap
    Set<String> geneSet = tpmStatMap.keySet() as List<String>
    println "gene sets: ${geneSet}"
    TpmStat geneA = tpmStatMap.get("GeneA")
    TpmStat geneC = tpmStatMap.get("GeneC")

    then:
    assert geneSet.size()==3
    assert geneA.numDataValues()==3
    assert geneA.mean()==5
    assert geneA.variance()==1.0
    assert geneC.numDataValues()==3
    assert geneC.mean()==5
    assert geneC.variance()==4.0

  }

  void "get mean, variance, from 2 files"(){

    given:
    File tpmFile1 = new File("src/test/data/sampleFile1.tpm")
    File tpmFile2 = new File("src/test/data/sampleFile2.tpm")

    when:
    TpmStatMap tpmStatMap = TpmStatGenerator.getGeneStatMap(tpmFile1)
    tpmStatMap = TpmStatGenerator.getGeneStatMap(tpmFile2,tpmStatMap)
    println tpmStatMap
    Set<String> geneSet = tpmStatMap.keySet() as List<String>
    println "gene sets: ${geneSet}"
    TpmStat geneA = tpmStatMap.get("GeneA")
    TpmStat geneC = tpmStatMap.get("GeneC")

    then:
    assert geneSet.size()==3
    assert geneA.numDataValues()==5
    assert geneC.numDataValues()==5
    assert geneA.mean()==6.4
    assert geneC.mean()==3.6
    assert geneA.standardDeviation()==2.073644135332772
    assert geneC.standardDeviation()==2.4083189157584592

  }



  void "rewrite gene expression cohort data to add z-scores"(){

    given:
    String cohortUrl = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"
    def cohorts = new JSONObject(new URL(cohortUrl).text)

    when:
    cohorts.keySet().each { String cohortName ->
      JSONObject cohortObject = cohorts.getJSONObject(cohortName)
      JSONObject geneExpressionObject = cohortObject.getJSONObject("gene expression")
      JSONObject rawGeneExpression = new JSONObject()
      rawGeneExpression.host = geneExpressionObject.host
      rawGeneExpression.dataset = geneExpressionObject.dataset
      cohortObject.put("gene expression raw",rawGeneExpression)
//      String dataset = geneExpressionObject.dataset

//      println "for cohort $cohortName"
//      println "for cohort object: ${cohortObject['gene expression'].dataset}"
      String mangledCohortName = cohortName == "TCGA Pheochromocytoma & Paraganglioma (PCPG)" ? "TCGA_PCPG" : cohortName.replaceAll("[ |\\(|\\)]", "_")
      String newDataSet = "tpm_z/${mangledCohortName}.z.tpm"
      geneExpressionObject.dataset = newDataSet
      String remoteUrl = AnalysisService.generateTpmRemoteUrl(cohortObject)
      println "remote url: $remoteUrl"
      def metadata = new URL(remoteUrl).openConnection().contentLength
      assert metadata!=9

      remoteUrl = AnalysisService.generateTpmRawRemoteUrl(cohortObject)
      println "remote url raw: $remoteUrl"
      metadata = new URL(remoteUrl).openConnection().contentLength
      assert metadata!=9
//      println metadata


//      https://xenago.xenahubs.net/download/expr_tpm/TCGA-LUAD_tpm_tab.tsv.gz
//      https://xenago.xenahubs.net/download/tpm_z/TCGA-LUAD_z.tpm.gz

      // convert "expr_tpm/TCGA-LIHC_tpm_tab.tsv" to "tpm_z/TCGA-LIHC_z.tsv
//      https://xenago.xenahubs.net/download/tpm_z/TCGA_Breast_Cancer__BRCA_.z.tpm.gz
      // convert "expr_tpm/TCGA-BRCA_tpm_tab.tsv" to "tpm_z/TCGA-BRCA_z.tsv

    }

    then:
    println "output cohort object"
    File outputFile = new File("output_defaultDatasetForGeneset.json")
    outputFile.write(cohorts.toString(2))


  }

  // NOTE: this is just run as a script and not really for testing
  @Ignore
  void "collect all TPM files to get stats"(){

    given:
    String cohortUrl = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"
    File convertedTPMFile = new File("${AnalysisService.TPM_DIRECTORY}/../allTpmGeneStats.json")
    println "abs path: ${convertedTPMFile.absolutePath}"
    def cohorts = new JSONObject(new URL(cohortUrl).text)
    println "keys size: ${cohorts.size()}"
    int numCohorts = cohorts.size()
    TpmStatMap tpmStatMap  = new TpmStatMap()

    when:
    if(!convertedTPMFile.exists() || convertedTPMFile.size()==0){
      convertedTPMFile.write("")
      cohorts.keySet().eachWithIndex { String entry, int i ->
        println "processing $entry: ${i+1} of $numCohorts"
        String localFileName = AnalysisService.generateLocalTpmName(entry)
        File unzippedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.tpm")
        assert unzippedTpmFile.exists() && unzippedTpmFile.size()>0
        tpmStatMap = TpmStatGenerator.getGeneStatMap(unzippedTpmFile,tpmStatMap)
      }
      convertedTPMFile.write(tpmStatMap.toString())
    }

    then:
    assert convertedTPMFile.exists()
    assert convertedTPMFile.size()>0

    when: "we ingest the file again"
    JSONObject geneFile = JSON.parse(convertedTPMFile.text)


    then: "we should have genes and stats"
    assert geneFile.keySet().size()==33

  }

//  void "get Z-scores from the TPM files"(){
  @Ignore
    void "generateZScore"(){

    given:
    String cohortUrl = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"
    File convertedTPMFile = new File("${AnalysisService.TPM_DIRECTORY}/../allTpmGeneStats.json")
    println "abs path: ${convertedTPMFile.absolutePath}"
    def cohorts = new JSONObject(new URL(cohortUrl).text)
    println "keys size: ${cohorts.size()}"
    int numCohorts = cohorts.size()
    TpmStatMap tpmStatMap  = new TpmStatMap()
    assert convertedTPMFile.exists() && convertedTPMFile.size()>0

    when:
    JSONObject geneFile = JSON.parse(convertedTPMFile.text)
    int numGenes = geneFile.keySet().size()
    println "numbeer of genes $numGenes"
    println "cohorts sorted: ${cohorts.keySet().sort().join(" ")}"
    // NOTE: requires 12 GB of memory to run larger files, or need to split files for more memory
    boolean doRegenerateFiles = false
    if(doRegenerateFiles){
      cohorts.keySet().sort().eachWithIndex { String cohort, int i ->
        println "processing $cohort: ${i+1} of $numCohorts"
        String localFileName = AnalysisService.generateLocalTpmName(cohort)
        File unzippedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.tpm")
        File zTransformedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.z.tpm")
        zTransformedTpmFile.write("")
        boolean isHeader = true
        StringBuilder stringBuilder = new StringBuilder()
        int geneCounter = 0
        unzippedTpmFile.splitEachLine("\t"){List<String> entries ->
          if(isHeader){
            stringBuilder.append(entries.join("\t")).append("\n")
            isHeader = false
          }
          else{
            String gene = entries.get(0)
            TpmStat tpmStat = new TpmStat(geneFile.getJSONObject(gene))
            stringBuilder.append(gene)
            entries.subList(1,entries.size()).each {String value ->
              double dValue = Double.parseDouble(value)
              stringBuilder.append("\t").append(tpmStat.getZValue(dValue))
            }
            stringBuilder.append("\n")
            if(geneCounter % 5000 == 0){
              println (geneCounter / numGenes * 100.0) +"%"
//              OutputHandler.printMemory()
//              System.gc()
            }
            ++geneCounter
          }
        }
        println "writing file output to $zTransformedTpmFile.absolutePath"
        zTransformedTpmFile.write(stringBuilder.toString())
        stringBuilder = null
        unzippedTpmFile = null
        zTransformedTpmFile = null

      }
    }

    then:
    assert true

  }


  @Ignore
  void "get stats for one cohort to confirm that mean of z-score is near 0"(){

    given:
    String cohortUrl = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"
    def cohorts = new JSONObject(new URL(cohortUrl).text)
    println "keys size: ${cohorts.size()}"
    int numCohorts = cohorts.size()
    TpmStat tpmStat1 = new TpmStat()

    when:
    println "cohorts sorted: ${cohorts.keySet().sort().join(" ")}"
    // get first 1
    String cohortKey = cohorts.keySet().first()
    File deCompressedFile = AnalysisService.getActivityTpmFile(cohorts,cohortKey)
    tpmStat1 = AnalysisService.analysizeTpmFile(deCompressedFile,tpmStat1)
    println tpmStat1.toString()

    // NOTE: requires 12 GB of memory to run larger files, or need to split files for more memory

    then:
    assert numCohorts==33
    assert tpmStat1.count == 32576796
    assert tpmStat1.mean() != Double.NaN
    assert tpmStat1.standardDeviation() != Double.NaN
    assert tpmStat1.variance()  != Double.NaN
//    assert tpmStat1.count == geneCounter * sampleCounter

  }

  @Ignore
  void "run all stats on all Z-Scores"(){

    given:
    String cohortUrl = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"
    def cohorts = new JSONObject(new URL(cohortUrl).text)
    println "keys size: ${cohorts.size()}"
    int numCohorts = cohorts.size()
    TpmStat tpmStat1 = new TpmStat()

    when:
    println "cohorts sorted: ${cohorts.keySet().sort().join(" ")}"
    // get first 1
    cohorts.keySet().each { String cohortKey ->
      println("processing: "+cohortKey)
      File deCompressedFile = AnalysisService.getActivityTpmFile(cohorts,cohortKey)
      tpmStat1 = AnalysisService.analysizeTpmFile(deCompressedFile,tpmStat1)
      println tpmStat1.toString()
    }

    // NOTE: requires 12 GB of memory to run larger files, or need to split files for more memory

    then:
    assert numCohorts==33
    assert tpmStat1.count == 32576796
    assert tpmStat1.mean() != Double.NaN
    assert tpmStat1.standardDeviation() != Double.NaN
    assert tpmStat1.variance()  != Double.NaN
//    assert tpmStat1.count == geneCounter * sampleCounter
  }

}

