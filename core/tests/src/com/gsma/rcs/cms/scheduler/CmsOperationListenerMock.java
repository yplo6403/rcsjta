package com.gsma.rcs.cms.scheduler;

import com.gsma.rcs.cms.scheduler.CmsScheduler.SyncType;
import com.gsma.rcs.utils.logger.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CmsOperationListenerMock implements CmsOperationListener {

    private static final Logger sLogger = Logger.getLogger(CmsOperationListenerMock.class.getSimpleName());

    private Map<CmsOperation,AtomicInteger> executions;

    CmsOperationListenerMock(){
        executions = new HashMap<>();
    }

    @Override
    public void onCmsOperationExecuted(CmsOperation operation, SyncType syncType, boolean result, Object param) {
        sLogger.info("onCmsOperationExecuted " + operation);
        AtomicInteger nb = executions.get(operation);
        if(nb == null){
            nb = new AtomicInteger(0);
        }
        nb.incrementAndGet();
        executions.put(operation, nb);
    }

    public int getExecutions(CmsOperation cmsOperation){
        return executions.get(cmsOperation) == null ? 0 : executions.get(cmsOperation).intValue();
    }
}
