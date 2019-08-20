package org.tron.service.task;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.MessageCode;
import org.tron.common.config.SystemSetting;
import org.tron.db.Manager;
import org.tron.service.eventactuator.Actuator;
import org.tron.service.eventactuator.Actuator.CheckTxRet;

@Slf4j(topic = "checkTxTask")
public class CheckTransactionTask {

  private static CheckTransactionTask instance = new CheckTransactionTask();

  public static CheckTransactionTask getInstance() {
    return instance;
  }

  private CheckTransactionTask() {
  }

  private final ScheduledExecutorService checkPool = Executors
      .newScheduledThreadPool(SystemSetting.CHECK_POOL_SIZE);

  public void submitCheck(Actuator eventActuator, long delay) {
    if (logger.isInfoEnabled()) {
      logger.info("check tx submit txId is {} , delay is {} ",
          eventActuator.getTransactionExtensionCapsule().getTransactionId(), delay);
    }
    checkPool.schedule(() -> instance.checkTransaction(eventActuator), delay, TimeUnit.SECONDS);
  }

  private void checkTransaction(Actuator eventActuator) {
    CheckTxRet checkTxRet = eventActuator.checkTxInfo();
    String transactionId = eventActuator.getTransactionExtensionCapsule().getTransactionId();
    String chain = eventActuator.getTaskEnum().name();
    if (checkTxRet == CheckTxRet.SUCCESS) {
      Manager.getInstance().setProcessSuccess(eventActuator.getNonceKey());
      if (logger.isInfoEnabled()) {
        String msg = MessageCode.CHECK_TRANSACTION_SUCCESS
            .getMsg(chain, transactionId);
        logger.info(msg);
      }
    } else {
      String msg = MessageCode.CHECK_TRANSACTION_FAIL
          .getMsg(chain, transactionId);
      RetryTransactionTask.getInstance().processAndSubmit(eventActuator, msg);
    }
  }
}
