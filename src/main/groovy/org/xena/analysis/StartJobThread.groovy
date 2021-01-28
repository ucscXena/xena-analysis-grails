package org.xena.analysis

class StartJobThread extends Thread{

  private Long jobId
  private AnalysisService analysisService

    StartJobThread(Long jobId, AnalysisService analysisService){
    this.jobId = jobId
    this.analysisService = analysisService
  }

  @Override
  void run() {
    TpmGmtAnalysisJob.withNewTransaction {
      analysisService.setJobState(jobId,RunState.RUNNING)
    }

  }
}
