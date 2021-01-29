package org.xena.analysis

class GmtStatThread extends Thread{

  private Long jobId
  private AnalysisService analysisService

    GmtStatThread(Long jobId, AnalysisService analysisService){
    this.jobId = jobId
    this.analysisService = analysisService
  }

  @Override
  void run() {
    Gmt.withNewTransaction {
      Gmt gmt = Gmt.findById(this.jobId)
      println "creating gmt stats for ${gmt.name}"
      analysisService.createGmtStats(gmt)
      println "CREATED gmt stats for ${gmt.name}"
    }

  }
}
