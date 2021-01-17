package org.xena.analysis

import grails.async.Promise
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.web.json.JSONObject
import org.springframework.scheduling.annotation.Scheduled

import java.text.SimpleDateFormat

import static grails.async.Promises.task


@Slf4j
@CompileStatic
@Transactional
class TpmAnalysisService {

  static lazyInit = false
  static List<TpmGmtAnalysisJob> analysisServiceJobs = []
  final int MAX_JOB_SIZE =  1

  AnalysisService analysisService

  @Scheduled(fixedDelay = 10000L)
  void executeEveryTen() {
    log.info "Simple Job every 10 seconds :{}", new SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(new Date())
  }

  @Scheduled(fixedDelay = 10000L)
  void checkJobQueue() {

    int jobsRunning = TpmGmtAnalysisJob.countByRunState(RunState.RUNNING)
    if(jobsRunning>=MAX_JOB_SIZE) {
      log.info("already have "+jobsRunning + " max allowed is $MAX_JOB_SIZE")
    }
    TpmGmtAnalysisJob jobToRun = TpmGmtAnalysisJob.findByRunState(RunState.NOT_STARTED)
    if(jobToRun){
      log.info "running job"

      Promise p = task {
        analysisService.doBpaAnalysis2(jobToRun)
      }

    }
    else{
    log.info "No job found to run "
    }


  }

  void loadTpmForGmtFiles(Gmt gmt) {
    Cohort.all.each {
      TpmGmtAnalysisJob analysisServiceJob = new TpmGmtAnalysisJob()
      analysisServiceJob.gmt = gmt

      analysisServiceJobs.add(analysisServiceJob)
    }
  }
}


