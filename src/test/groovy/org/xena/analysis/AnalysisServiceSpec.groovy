package org.xena.analysis

import grails.testing.services.ServiceUnitTest
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
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

  void "convert mean map to values"(){
    given:
    def inputA = new JSONObject(new File("src/test/data/inputA.json").text)
    def inputB = new JSONObject(new File("src/test/data/inputB.json").text)
    assert inputA.samples.size() == 89
    assert inputB.samples.size() == 548
    assert inputA.data.size() == 9
    assert inputB.data.size() == 9
    assert inputA.data[0].data.size() == 89
    assert inputB.data[0].data.size() == 548

    when:
    def valuesA = AnalysisService.extractValuesByCohort(inputA)

    then:
    assert valuesA.size() == 9
    assert valuesA[0].size() == 89


    when:
    def values = AnalysisService.extractValues(inputA,inputB)

    then:
    assert values.size() == 2
    assert values[0].size() == 9
    assert values[1].size() == 9
    assert values[0][0].size() == 89
    assert values[1][0].size() == 548
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

  void "handle gmt data"(){

    expect:
    def gmtData = new File("src/test/data/gmtData.gmt").text
    def meanMap = new JSONObject(new File("src/test/data/meanMap.json").text) as Map
    JSONArray outputArray = AnalysisService.generateResult(gmtData,meanMap)
    assert outputArray.length()==9

  }

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

//  String inputTpmFile = "\tTCGA-FA-8693-01\tTCGA-FA-A4BB-01\tTCGA-FA-A4XK-01\tTCGA-FA-A6HN-01\tTCGA-FA-A6HO-01\tTCGA-FA-A7DS-01\tTCGA-FA-A7Q1-01\tTCGA-FA-A82F-01\tTCGA-FA-A86F-01\tTCGA-FF-8041-01\tTCGA-FF-8042-01\tTCGA-FF-8043-01\tTCGA-FF-8046-01\tTCGA-FF-8047-01\tTCGA-FF-8061-01\tTCGA-FF-8062-01\tTCGA-FF-A7CQ-01\tTCGA-FF-A7CR-01\tTCGA-FF-A7CW-01\tTCGA-FF-A7CX-01\tTCGA-FM-8000-01\tTCGA-G8-6324-01\tTCGA-G8-6325-01\tTCGA-G8-6326-01\tTCGA-G8-6906-01\tTCGA-G8-6907-01\tTCGA-G8-6909-01\tTCGA-G8-6914-01\tTCGA-GR-7351-01\tTCGA-GR-7353-01\tTCGA-GR-A4D4-01\tTCGA-GR-A4D5-01\tTCGA-GR-A4D6-01\tTCGA-GR-A4D9-01\tTCGA-GS-A9TQ-01\tTCGA-GS-A9TT-01\tTCGA-GS-A9TU-01\tTCGA-GS-A9TV-01\tTCGA-GS-A9TW-01\tTCGA-GS-A9TX-01\tTCGA-GS-A9TY-01\tTCGA-GS-A9TZ-01\tTCGA-GS-A9U3-01\tTCGA-GS-A9U4-01\tTCGA-RQ-A68N-01\tTCGA-RQ-A6JB-01\tTCGA-RQ-AAAT-01\tTCGA-VB-A8QN-01\n" +
//    "5_8S_rRNA\t0\t0.106254798732142\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0.0559778472990654\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0.0336212627509489\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0.0543801232040295\t0\t0\t0\t0\t0\t0\t0\n" +
//    "5S_rRNA\t0.0258167664457611\t0.440074472899759\t0.335425626657636\t0.157818149327173\t0.24124224014973\t0.26351444986388\t0.210263810880641\t0.126125495392285\t0.229772296045377\t0.130372695180532\t0.162831852295796\t0\t0.109864588702799\t0.217683815557175\t0.345908678639463\t0.126554582144845\t0.30198772491816\t0.418740282713079\t0.264097593209621\t0.317511247884031\t0.346582533087183\t0.241234664575699\t0.270270699385006\t0.523765145311035\t0.681544289335051\t0.200277587538424\t0.0906153510239814\t0.385724805365107\t0.152031491794368\t0.252655018095624\t0.330002327508096\t0.298873632947886\t0.215933928506381\t0.439667037483651\t0.384492619875497\t0.443755945320967\t0.207308506973252\t0.471958621804167\t0.371245423577946\t0.301898549368932\t0.157352285374501\t0.216064060314675\t0.0762838865062203\t0.262048919651295\t0.0293744679264015\t0.337058447213174\t0\t0.0664411542557478\n" +
//    "7SK\t0\t0.0362947685962892\t0\t0.0545469172631972\t0\t0\t0\t0\t0.0445911969672675\t0\t0.0340698616792856\t0\t0.480921398698744\t0.0973402364780288\t0\t0.0326945354081395\t0\t0.0267418253837147\t0.0318475807831187\t0.0693389235080846\t0.0549070093149821\t0.0370314927880097\t0\t0\t0\t0\t0.0460189998246264\t0.0336212627509489\t0\t0\t0.0486110706810784\t0\t0\t0\t0\t0.0224151052368053\t0\t0.449420521161164\t0.0519756713105345\t0\t0\t0\t0\t0.031580356221135\t0.0293744679264015\t0\t0\t0\n" +
//    "A1BG\t0.28293872918626\t0.327419950685163\t0.648119456966863\t1.48118512446862\t0.933610402005059\t0.794767384080294\t0.835518749244772\t0.60910545062142\t1.79903743277199\t0.604233012641713\t1.08619583567547\t0.865323212984939\t0.439432984183205\t0.551224639854269\t0.345908678640581\t0.715264447294833\t0.809215407686299\t1.04712382088662\t0.920537765867287\t1.02428532957012\t0.73071946809712\t0.873433156666492\t0.657087038610782\t0.856436092968817\t1.08504807878892\t0.49521248454839\t0.175873970681287\t0.97578009300151\t1.20527523245518\t0.968635092581469\t0.995515553800539\t0.500119243600318\t0.872113851107927\t1.1096591833234\t0.744243854685889\t0.555569596096959\t1.18929342061144\t0.895287432480103\t0.9389184231662\t0.733342889820824\t0.428373473931345\t0.92414342478936\t0.888057162150779\t0.695702485469891\t0.0293744679264015\t0.729397095319533\t0.499216222843376\t0.557095656694254\n" +
//    "A1BG-AS1\t1.7035376563893\t1.1358019593997\t2.44618823926302\t3.31039332099103\t2.40204218349594\t2.21776475434131\t1.96445213462063\t1.77442163718879\t3.74071255556152\t2.42603923800681\t2.66739525890049\t1.83047786656207\t1.8365809007818\t2.12883295855816\t1.19820797330154\t2.22259991402682\t2.17901712415031\t2.83521920537151\t2.30112261281051\t2.77801064653064\t1.87839497313681\t2.6383308113881\t2.27882860247412\t2.84327401897482\t2.77342063885773\t1.51676780153317\t1.09384721106486\t2.24042071836894\t1.84150697375843\t1.81123913297263\t2.90901592013858\t1.96925504659686\t1.84128425471969\t2.87266528641465\t2.21683775876025\t2.20207520699648\t2.82639328876355\t2.61785866370085\t2.50516976008275\t2.23460711113699\t1.67334461853364\t2.73435428567794\t2.46594971604008\t2.45167200472192\t1.42583581503389\t2.76339576162438\t1.29104896553396\t2.15072907362515\n" +
//    "A1CF\t0\t0\t0\t0\t0.114675945668738\t0\t0.0555079434279488\t0.0956127034970187\t0.0224678711788893\t0.0666585280295507\t0.0340698616792856\t0\t0.0559778472990654\t0.188525888995378\t0\t0\t0.170407579998223\t0.0529969711566488\t0\t0.0350859932711854\t0.0549070093149821\t0.209145454846249\t0.0582566666940305\t0.0232002735329757\t0.193803468218725\t0\t0\t0\t0\t0\t0\t0\t0\t0.0254747395273176\t0\t0.0224151052368053\t0.207308506973451\t1.09725618283384\t0.0262218875726562\t0.0754849120133351\t0\t0\t0.148735952588547\t0.179876678223204\t0.0581627624468507\t0.0919294405726864\t0\t0.0985484383817703\n"
//
//  String filteredTpmFile = "\tTCGA-FA-8693-01\tTCGA-FA-A4BB-01\tTCGA-FA-A4XK-01\tTCGA-FA-A6HN-01\tTCGA-FA-A6HO-01\tTCGA-FA-A7DS-01\tTCGA-FA-A7Q1-01\tTCGA-FA-A82F-01\tTCGA-FA-A86F-01\tTCGA-FF-8041-01\tTCGA-FF-8042-01\tTCGA-FF-8043-01\tTCGA-FF-8046-01\tTCGA-FF-8047-01\tTCGA-FF-8061-01\tTCGA-FF-8062-01\tTCGA-FF-A7CQ-01\tTCGA-FF-A7CR-01\tTCGA-FF-A7CW-01\tTCGA-FF-A7CX-01\tTCGA-FM-8000-01\tTCGA-G8-6324-01\tTCGA-G8-6325-01\tTCGA-G8-6326-01\tTCGA-G8-6906-01\tTCGA-G8-6907-01\tTCGA-G8-6909-01\tTCGA-G8-6914-01\tTCGA-GR-7351-01\tTCGA-GR-7353-01\tTCGA-GR-A4D4-01\tTCGA-GR-A4D5-01\tTCGA-GR-A4D6-01\tTCGA-GR-A4D9-01\tTCGA-GS-A9TQ-01\tTCGA-GS-A9TT-01\tTCGA-GS-A9TU-01\tTCGA-GS-A9TV-01\tTCGA-GS-A9TW-01\tTCGA-GS-A9TX-01\tTCGA-GS-A9TY-01\tTCGA-GS-A9TZ-01\tTCGA-GS-A9U3-01\tTCGA-GS-A9U4-01\tTCGA-RQ-A68N-01\tTCGA-RQ-A6JB-01\tTCGA-RQ-AAAT-01\tTCGA-VB-A8QN-01\n" +
//    "5_8S_rRNA\t0\t0.106254798732142\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0.0559778472990654\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0.0336212627509489\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0.0543801232040295\t0\t0\t0\t0\t0\t0\t0\n" +
//    "5S_rRNA\t0.0258167664457611\t0.440074472899759\t0.335425626657636\t0.157818149327173\t0.24124224014973\t0.26351444986388\t0.210263810880641\t0.126125495392285\t0.229772296045377\t0.130372695180532\t0.162831852295796\t0\t0.109864588702799\t0.217683815557175\t0.345908678639463\t0.126554582144845\t0.30198772491816\t0.418740282713079\t0.264097593209621\t0.317511247884031\t0.346582533087183\t0.241234664575699\t0.270270699385006\t0.523765145311035\t0.681544289335051\t0.200277587538424\t0.0906153510239814\t0.385724805365107\t0.152031491794368\t0.252655018095624\t0.330002327508096\t0.298873632947886\t0.215933928506381\t0.439667037483651\t0.384492619875497\t0.443755945320967\t0.207308506973252\t0.471958621804167\t0.371245423577946\t0.301898549368932\t0.157352285374501\t0.216064060314675\t0.0762838865062203\t0.262048919651295\t0.0293744679264015\t0.337058447213174\t0\t0.0664411542557478\n" +
//    "7SK\t0\t0.0362947685962892\t0\t0.0545469172631972\t0\t0\t0\t0\t0.0445911969672675\t0\t0.0340698616792856\t0\t0.480921398698744\t0.0973402364780288\t0\t0.0326945354081395\t0\t0.0267418253837147\t0.0318475807831187\t0.0693389235080846\t0.0549070093149821\t0.0370314927880097\t0\t0\t0\t0\t0.0460189998246264\t0.0336212627509489\t0\t0\t0.0486110706810784\t0\t0\t0\t0\t0.0224151052368053\t0\t0.449420521161164\t0.0519756713105345\t0\t0\t0\t0\t0.031580356221135\t0.0293744679264015\t0\t0\t0\n" +
//    "A1BG\t0.28293872918626\t0.327419950685163\t0.648119456966863\t1.48118512446862\t0.933610402005059\t0.794767384080294\t0.835518749244772\t0.60910545062142\t1.79903743277199\t0.604233012641713\t1.08619583567547\t0.865323212984939\t0.439432984183205\t0.551224639854269\t0.345908678640581\t0.715264447294833\t0.809215407686299\t1.04712382088662\t0.920537765867287\t1.02428532957012\t0.73071946809712\t0.873433156666492\t0.657087038610782\t0.856436092968817\t1.08504807878892\t0.49521248454839\t0.175873970681287\t0.97578009300151\t1.20527523245518\t0.968635092581469\t0.995515553800539\t0.500119243600318\t0.872113851107927\t1.1096591833234\t0.744243854685889\t0.555569596096959\t1.18929342061144\t0.895287432480103\t0.9389184231662\t0.733342889820824\t0.428373473931345\t0.92414342478936\t0.888057162150779\t0.695702485469891\t0.0293744679264015\t0.729397095319533\t0.499216222843376\t0.557095656694254\n" +
//    "A1BG-AS1\t1.7035376563893\t1.1358019593997\t2.44618823926302\t3.31039332099103\t2.40204218349594\t2.21776475434131\t1.96445213462063\t1.77442163718879\t3.74071255556152\t2.42603923800681\t2.66739525890049\t1.83047786656207\t1.8365809007818\t2.12883295855816\t1.19820797330154\t2.22259991402682\t2.17901712415031\t2.83521920537151\t2.30112261281051\t2.77801064653064\t1.87839497313681\t2.6383308113881\t2.27882860247412\t2.84327401897482\t2.77342063885773\t1.51676780153317\t1.09384721106486\t2.24042071836894\t1.84150697375843\t1.81123913297263\t2.90901592013858\t1.96925504659686\t1.84128425471969\t2.87266528641465\t2.21683775876025\t2.20207520699648\t2.82639328876355\t2.61785866370085\t2.50516976008275\t2.23460711113699\t1.67334461853364\t2.73435428567794\t2.46594971604008\t2.45167200472192\t1.42583581503389\t2.76339576162438\t1.29104896553396\t2.15072907362515\n" +
//    "A1CF\t0\t0\t0\t0\t0.114675945668738\t0\t0.0555079434279488\t0.0956127034970187\t0.0224678711788893\t0.0666585280295507\t0.0340698616792856\t0\t0.0559778472990654\t0.188525888995378\t0\t0\t0.170407579998223\t0.0529969711566488\t0\t0.0350859932711854\t0.0549070093149821\t0.209145454846249\t0.0582566666940305\t0.0232002735329757\t0.193803468218725\t0\t0\t0\t0\t0\t0\t0\t0\t0.0254747395273176\t0\t0.0224151052368053\t0.207308506973451\t1.09725618283384\t0.0262218875726562\t0.0754849120133351\t0\t0\t0.148735952588547\t0.179876678223204\t0.0581627624468507\t0.0919294405726864\t0\t0.0985484383817703\n"
}

