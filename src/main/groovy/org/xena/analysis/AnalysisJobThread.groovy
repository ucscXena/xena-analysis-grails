package org.xena.analysis

class AnalysisJobThread extends Thread{

  private Long jobId
  private AnalysisService analysisService

  AnalysisJobThread(Long jobId,AnalysisService analysisService){
    this.jobId = jobId
    this.analysisService = analysisService
  }

  @Override
  void run() {
    TpmGmtAnalysisJob.withNewTransaction {
      try {
        TpmGmtAnalysisJob jobToRun = TpmGmtAnalysisJob.findById(jobId)
        println "set job to running, $jobToRun.cohort.name and $jobToRun.gmt.name"
        analysisService.doBpaAnalysis2(jobToRun)
        println "did analysis, setting to finished, $jobToRun.cohort.name and $jobToRun.gmt.name"
        analysisService.setJobState(jobToRun.id,RunState.FINISHED)
        println "set to finished, $jobToRun.cohort.name and $jobToRun.gmt.name"
      } catch (e) {
        analysisService.setJobState(jobId,RunState.ERROR,e.message)
      }
    }

  }
}
