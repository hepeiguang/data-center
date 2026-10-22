package org.apache.dolphinscheduler.plugin.task.flexiblehttp;

public enum FlexibleHttpTaskType {
    /**
     * 0 simple_request: simple http request
     * 1 loop_request: loop query until condition met
     * 2
     */
    SIMPLE_REQUEST, LOOP_REQUEST, ITERATE_REQUEST
}
