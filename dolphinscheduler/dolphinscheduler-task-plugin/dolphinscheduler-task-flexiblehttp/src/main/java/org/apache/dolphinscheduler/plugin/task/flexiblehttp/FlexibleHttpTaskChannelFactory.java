package org.apache.dolphinscheduler.plugin.task.flexiblehttp;

import com.google.auto.service.AutoService;
import org.apache.dolphinscheduler.plugin.task.api.TaskChannel;
import org.apache.dolphinscheduler.plugin.task.api.TaskChannelFactory;
import org.apache.dolphinscheduler.spi.params.base.PluginParams;

import java.util.List;

@AutoService(TaskChannelFactory.class)
public class FlexibleHttpTaskChannelFactory implements TaskChannelFactory {

    @Override
    public String getName() {
        return "FLEXIBLE_HTTP";
    }

    @Override
    public List<PluginParams> getParams() {
        return null;
    }

    @Override
    public TaskChannel create() {
        return new FlexibleHttpTaskChannel();
    }
}