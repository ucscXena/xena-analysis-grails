package org.xena.analysis

class SetJobStatThread extends Thread{

  private Long jobId
  private AnalysisService analysisService

  SetJobStatThread(Long jobId, AnalysisService analysisService){
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
