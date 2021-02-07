package cn.pcl.firewall.rtecli;

import com.google.common.collect.Lists;

import java.util.List;

public class Action {
    private final String action;

    private final List<ActionParam> paramList;

    public Action(String action) {
        this.action = action;
        this.paramList = Lists.newArrayList();
    }

    public Action(String action, List<ActionParam> paramList) {
        this.action = action;
        this.paramList = paramList;
    }

    public String getAction() {
        return action;
    }

    public List<ActionParam> getParamList() {
        return paramList;
    }

    @Override
    public String toString() {
        return "Action{" +
                "action='" + action + '\'' +
                ", paramList=" + paramList +
                '}';
    }

    public static class ActionParam {
        private final String paramName;

        private final String paramValue;

        public String getParamName() {
            return paramName;
        }

        public String getParamValue() {
            return paramValue;
        }

        public ActionParam(String paramName, String paramValue) {
            this.paramName = paramName;
            this.paramValue = paramValue;
        }
    }
}
