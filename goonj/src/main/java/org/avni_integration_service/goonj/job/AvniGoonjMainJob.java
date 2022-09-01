package org.avni_integration_service.goonj.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.SyncDirection;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.client.AvniSession;
import org.avni_integration_service.goonj.worker.AvniGoonjErrorRecordsWorker;
import org.avni_integration_service.goonj.worker.avni.ActivityWorker;
import org.avni_integration_service.goonj.worker.avni.DispatchReceiptWorker;
import org.avni_integration_service.goonj.worker.avni.DistributionWorker;
import org.avni_integration_service.goonj.worker.goonj.DemandWorker;
import org.avni_integration_service.goonj.worker.goonj.DispatchWorker;
import org.avni_integration_service.integration_data.repository.ConstantsRepository;
import org.avni_integration_service.util.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AvniGoonjMainJob {
    private static final Logger logger = Logger.getLogger(AvniGoonjMainJob.class);

    @Value("${healthcheck.mainJob}")
    private String mainJobId;

    @Autowired
    private DemandWorker demandWorker;

    @Autowired
    private DispatchWorker dispatchWorker;

    @Autowired
    private DispatchReceiptWorker dispatchReceiptWorker;

    @Autowired
    private DistributionWorker distributionWorker;

    @Autowired
    private ActivityWorker activityWorker;

    @Autowired
    private ConstantsRepository constantsRepository;

    @Value("${goonj.app.tasks}")
    private String tasks;

    @Autowired
    private AvniGoonjErrorRecordsWorker errorRecordsWorker;

    @Autowired
    private Bugsnag bugsnag;

    @Autowired
    private AvniHttpClient avniHttpClient;

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    @Qualifier("GoonjAvniSession")
    private AvniSession goonjAvniSession;

    public void execute() {
        try {
            logger.info("Executing Goonj Main Job");
            avniHttpClient.setAvniSession(goonjAvniSession);

            List<IntegrationTask> tasks = IntegrationTask.getTasks(this.tasks);

            if (hasTask(tasks, IntegrationTask.GoonjDemand)) {
                logger.info("Processing GoonjDemand");
                demandWorker.process();
                /*
                  We are triggering deletion tagged along with Demand creations, as the Goonj System sends
                  the Deleted Demands info as part of the same getDemands API, but as a separate list,
                  without any TimeStamp and other minimal information details required to make an Update Subject as Voided call.
                  Therefore, we invoke the Delete API for subject using DemandId as externalId to mark a Demand as Voided.
                 */
                demandWorker.processDeletions();
            }
            if (hasTask(tasks, IntegrationTask.GoonjDispatch)) {
                logger.info("Processing GoonjDispatch");
                dispatchWorker.process();
                /*
                  We are triggering deletion tagged along with DispatchStatus creations, as the Goonj System sends
                  the Deleted DispatchStatuses info as part of the same getDispatchStatus API, but as a separate list,
                  without any TimeStamp and other minimal information details required to make an Update DispatchStatus as Voided call.
                  Therefore, we invoke the Delete API for DispatchStatus using DispatchStatusId as externalId to mark a DispatchStatus as Voided.
                 */
                dispatchWorker.processDeletions();
                dispatchWorker.processDispatchLineItemDeletions();
            }
            if (hasTask(tasks, IntegrationTask.AvniDispatchReceipt)) {
                logger.info("Processing AvniDispatchReceipt");
                dispatchReceiptWorker.process();
            }
            if (hasTask(tasks, IntegrationTask.AvniActivity)) {
                logger.info("Processing AvniActivity");
                activityWorker.process();
            }
            if (hasTask(tasks, IntegrationTask.AvniDistribution)) {
                logger.info("Processing AvniDistribution");
                distributionWorker.process();
            }
            if (hasTask(tasks, IntegrationTask.GoonjErrorRecords)) {
                logger.info("Processing GoonjErrorRecords");
                processErrorRecords(SyncDirection.GoonjToAvni);
            }
            if (hasTask(tasks, IntegrationTask.AvniErrorRecords)) {
                logger.info("Processing AvniErrorRecords");
                processErrorRecords(SyncDirection.AvniToGoonj);
            }
        } catch (Throwable e) {
            logger.error("Failed", e);
            bugsnag.notify(e);
        } finally {
            healthCheckService.verify(mainJobId);
        }
    }

    private void processErrorRecords(SyncDirection syncDirection) {
        errorRecordsWorker.process(syncDirection, false);
    }

    private boolean hasTask(List<IntegrationTask> tasks, IntegrationTask task) {
        return tasks.stream().filter(integrationTask -> integrationTask.equals(task)).findAny().orElse(null) != null;
    }
}
