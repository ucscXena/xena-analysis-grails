package org.xena.analysis

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.web.json.JSONObject
import org.springframework.scheduling.annotation.Scheduled

import java.text.SimpleDateFormat

@Slf4j
@Transactional
class TpmAnalysisService {

  // this needs to be here for quarts
  static lazyInit = false
//  static List<TpmGmtAnalysisJob> analysisServiceJobs = []
  final int MAX_JOB_SIZE = Math.max(0,(System.getenv("XENA_ANALYSIS_PROCESSORS") ? Integer.parseInt(System.getenv("XENA_ANALYSIS_PROCESSORS")): Runtime.getRuntime().availableProcessors() - 1 ))
  int counter = 0
  int possibleCohortCount

  TpmAnalysisService() {
    possibleCohortCount = new JSONObject(new URL(CohortService.COHORT_URL).text).keySet().size()
    println "max job size: $MAX_JOB_SIZE"
    println "possible cohorts: $possibleCohortCount"
  }

  AnalysisService analysisService

//  @Scheduled(fixedDelay = 5000L)
//  void executeEveryTen() {
//    log.info "Simple Job every 5 seconds :{}", new SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(new Date())
//  }

  @Scheduled(fixedDelay = 10000L)
  void calculateGmtStats() {
    log.info("checking gmt ready to calculate ")
    println "possible cohort count: ${possibleCohortCount}"
    def gmt = Gmt.findByStatsIsNull()
    int resultCount = TpmGmtResult.countByGmt(gmt)
    println "result count: ${resultCount}"
    if (resultCount == possibleCohortCount) {
      println "creating gmt stats for ${gmt.name}"
      analysisService.createGmtStats(gmt)
      println "CREATED gmt stats for ${gmt.name}"
    }
  }

  @Scheduled(fixedDelay = 10000L)
  void checkJobQueue() {
    log.debug("checking job queue: " + counter)
    ++counter

    int jobsRunning = TpmGmtAnalysisJob.executeQuery("select count(*) from TpmGmtAnalysisJob t where t.runState = :runState", [runState: RunState.RUNNING])[0] as int
    log.debug("jobs running: ${jobsRunning} vs $MAX_JOB_SIZE")
    if (jobsRunning >= MAX_JOB_SIZE) {
      log.debug("already have " + jobsRunning + " max allowed is $MAX_JOB_SIZE")
      return
    }
    int numJobsToRun = TpmGmtAnalysisJob.executeQuery("select count(*) from TpmGmtAnalysisJob t where t.runState = :runState", [runState: RunState.NOT_STARTED])[0] as int
    log.debug("number of jobs to run $numJobsToRun")
    def foundJobsToRun = TpmGmtAnalysisJob.executeQuery("select t from TpmGmtAnalysisJob t where t.runState = :runState order by t.createdDate", [runState: RunState.NOT_STARTED, max: MAX_JOB_SIZE - jobsRunning])
    log.debug("job to try and run run: ${foundJobsToRun.size()}")
    for (TpmGmtAnalysisJob jobToRun in foundJobsToRun) {
      log.info "running job, $jobToRun.cohort.name and $jobToRun.gmt.name"
      new SetJobStatThread(jobToRun.id, analysisService).start()
      new AnalysisJobThread(jobToRun.id, analysisService).start()
      log.info "running thread, $jobToRun.cohort.name and $jobToRun.gmt.name"
    }

  }

  void loadTpmForGmtFiles(Gmt gmt) {
    Cohort.all.each {
      Date now = new Date()
      TpmGmtAnalysisJob analysisServiceJob = new TpmGmtAnalysisJob()
      analysisServiceJob.gmt = gmt
      analysisServiceJob.cohort = it
      analysisServiceJob.createdDate = now
      analysisServiceJob.lastUpdated = now
//      analysisServiceJob.method = "BPA Gene Expression"
      analysisServiceJob.save(insert: true, failOnError: true)
//      analysisServiceJobs.add(analysisServiceJob)
    }
  }
}


