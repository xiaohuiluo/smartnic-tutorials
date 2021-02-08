package cn.pcl.firewall.rtecli;

import cn.pcl.firewall.rtecli.bean.StatusBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NfpRteCliController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final Pattern pTableEntry = Pattern.compile("TableEntry\\(priority=(\\d+), rule_name='([^']+)', default_rule=(True|False), actions='\\{ ([^']*) \\}', match='\\{ ([^']*) \\}'");
    private static final Pattern pMatch = Pattern.compile("\"([^\"]+)\" : \\{  \"value\" : \"([^\"]+)\" \\}");
    private static final Pattern pAction = Pattern.compile("\"type\" : \"([^\"]+)\",  \"data\" : \\{ (.*) \\}");
    private static final Pattern pData = Pattern.compile("\"([^\"]+)\" : \\{ \"value\" : \"([^\"]+)\" \\}");

    public static final String REG_SINGER_QUOTE = "\'";
    public static final String REG_TRUE = "True";
    public static final String REG_FALSE = "False";
    public static final String REG_NONE = "None";

    public static final String SHELL = "/bin/sh";
    public static final String SHELL_FILE_ARG = "-c";
    public static final String RTE_CLI_PATH = "/opt/netronome/p4/bin/rtecli";
    public static final String SPACE = " ";

    public static final long DEFAULT_RTECLI_TIMEOUT = 10000; //10000ms
    public static final int DEFAULT_RULE_PRIORITY = 0;
    public static final long DEFAULT_RULE_TIMEOUT = 0;//0s no timeout

    public static final String RTE_HOST_ARG = "-r";
    public static final String RTE_PORT_ARG = "-p";

    public static final String RTE_JSON_RS_ARG = "-j";

    public static final String RTE_STATUS = "status";
    public static final String RTE_PORTS = "ports";
    public static final String RTE_LIST = "list";

    public static final String DESIGN_LOAD = "design-load";
    public static final String NFFW_ARG = "-f";
    public static final String DESIGN_ARG = "-p";
    public static final String P4CFG_ARG = "-c";

    public static final String TABLES = "tables";
    public static final String ADD = "add";
    public static final String EDIT = "edit";
    public static final String DELETE = "delete";

    public static final String TABLE_ARG = "-t";
    public static final String RULE_NAME_ARG = "-r";
    public static final String RULE_MATCH_ARG = "-m";
    public static final String RULE_ACTION_ARG = "-a";
    public static final String RULE_PRIORITY_ARG = "-p";
    public static final String RULE_TIMEOUT_ARG = "-o";



    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static File rteCliFile;

    static {
        rteCliFile = new File(RTE_CLI_PATH);
    }

    private boolean checkRteCli() {
        if (!rteCliFile.exists()) {
            log.error("rtecli is not exist in path={}", RTE_CLI_PATH);
            return false;
        }

        return true;
    }

    private RteCliResponse execute(CommandLine commandLine, long timeout) {
        DefaultExecutor exec = new DefaultExecutor();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
        exec.setWatchdog(watchdog);
        exec.setStreamHandler(streamHandler);
        exec.setExitValue(0);

        try {
            exec.execute(commandLine);
            String reply = outputStream.toString().trim();
            log.debug("RTECLI - {}", reply);

            return new RteCliResponse(true, reply);
        }
        catch (IOException e) {
            String cmdLine = commandLine.toString().trim();
            if (!cmdLine.isEmpty()) {
                log.debug(cmdLine);
            }
            log.warn("Error during RTECLI cmd={}, execution: {}", cmdLine, e.getMessage(), e);

            String outStr = outputStream.toString().trim();
            if (!outStr.isEmpty()) {
                log.error("RTECLI execute cmd={}, error={}", cmdLine, outStr);
            }

            return new RteCliResponse(false, outStr);
        }
    }

    public RteCliResponse execute(CommandLine commandLine) {
        return execute(commandLine, DEFAULT_RTECLI_TIMEOUT);
    }

    /**
     * /opt/netronome/p4/bin/rtecli -r rteHost -p -j rtePort status
     * @param rteHost
     * @param rtePort
     * @return
     */
    public synchronized boolean connectNic(String rteHost, String rtePort) {
        return getStatus(rteHost, rtePort).isResult();
    }

    private synchronized RteCliResponse getStatus(String rteHost, String rtePort) {
        if (!checkRteCli()) {
            return new RteCliResponse(false);
        }

        CommandLine commandLine = new CommandLine(RTE_CLI_PATH);

        commandLine.addArgument(RTE_HOST_ARG);
        commandLine.addArgument(rteHost);
        commandLine.addArgument(RTE_PORT_ARG);
        commandLine.addArgument(rtePort);
        commandLine.addArgument(RTE_JSON_RS_ARG);
        commandLine.addArgument(RTE_STATUS);

        RteCliResponse response = execute(commandLine);

        return response;
    }

    // /opt/netronome/p4/bin/rtecli -r 192.168.67.143 -p 20206 -j ports list
    public synchronized String getNicPorts(String rteHost, String rtePort) {
        if (!checkRteCli()) {
            return "[]";
        }

        CommandLine commandLine = new CommandLine(RTE_CLI_PATH);

        commandLine.addArgument(RTE_HOST_ARG);
        commandLine.addArgument(rteHost);
        commandLine.addArgument(RTE_PORT_ARG);
        commandLine.addArgument(rtePort);
        commandLine.addArgument(RTE_JSON_RS_ARG);
        commandLine.addArgument(RTE_PORTS);
        commandLine.addArgument(RTE_LIST);

        RteCliResponse response = execute(commandLine);
        if (response.isResult()) {
            return response.getMessage();
        }

        return "[]";
    }

    /**
     * /opt/netronome/p4/bin/rtecli -r RTE_HOST -p RTE_PORT -j design-load -f nffwPath -p designPath -c p4cfgPath
     * @param rteHost
     * @param rtePort
     * @param nffwPath
     * @param designPath
     * @param p4cfgPath
     * @return
     */
    public synchronized RteCliResponse designLoad(String rteHost, String rtePort, String nffwPath, String designPath, String p4cfgPath) {
        // check status
        RteCliResponse statusRs = getStatus(rteHost, rtePort);
        if (!statusRs.isResult()) {
            return statusRs;
        }

        String statusMsg = statusRs.getMessage();
        try {
            StatusBean bean = OBJECT_MAPPER.readValue(statusMsg, StatusBean.class);
            if (bean.isIs_loaded()) {
                log.warn("rteHost={}, rtePort={} design is loaded", rteHost, rtePort);
                return new RteCliResponse(true, "nfp nic is already design loaded");
            }
        }
        catch (IOException e) {
            log.error("Failed to get status of rteHost={}, rtePort={}, exception={}", rteHost, rtePort, e);
            return new RteCliResponse(false, e.getMessage());
        }

        // design load
        CommandLine commandLine = new CommandLine(RTE_CLI_PATH);

        commandLine.addArgument(RTE_HOST_ARG);
        commandLine.addArgument(rteHost);
        commandLine.addArgument(RTE_PORT_ARG);
        commandLine.addArgument(rtePort);
        commandLine.addArgument(RTE_JSON_RS_ARG);
        commandLine.addArgument(DESIGN_LOAD);
        commandLine.addArgument(NFFW_ARG);
        commandLine.addArgument(nffwPath);
        commandLine.addArgument(DESIGN_ARG);
        commandLine.addArgument(designPath);

        if (p4cfgPath != null && !p4cfgPath.isEmpty()) {
            commandLine.addArgument(P4CFG_ARG);
            commandLine.addArgument(p4cfgPath);
        }

        RteCliResponse response = execute(commandLine, 500000);
        return response;
    }

    public synchronized String getPortStats(String rteHost, String rtePort) {
        return null;
    }

    public synchronized RteCliResponse addFlowRule(String rteHost, String rtePort, String table, String ruleName, List<Match> matchList, Action action) {
        return addFlowRule(rteHost, rtePort, table, ruleName, DEFAULT_RULE_PRIORITY, DEFAULT_RULE_TIMEOUT, matchList, action);
    }

    public synchronized RteCliResponse addFlowRule(String rteHost, String rtePort, String table, String ruleName, int priority, long timeout, List<Match> matchList, Action action) {
        // build match
        List<String> matchStrList = new LinkedList<>();
        for (Match match : matchList)
        {
            if (match.getType() == Match.Type.EXACT) {
                matchStrList.add(format("'%s': { 'value': '%s' }", match.getKey(), match.getValue()));
            }
            else if (match.getType() == Match.Type.TERNARY) {
                matchStrList.add(format("'%s': { 'value': '%s' , 'mask': '%s'}", match.getKey(), match.getValue(), match.getMask()));
            }
            else if (match.getType() == Match.Type.LPM) {
                matchStrList.add(format("'%s': { 'value': '%s'}", match.getKey(), match.getValue()));
            }
            else {
                log.error("Error match type={}", match.getType());
            }
        }

        String matchArgStr = "{ " + matchStrList.stream().collect(Collectors.joining(", ")) + " }";

        // build action
        LinkedList<String> actionData = new LinkedList<>();
        for (Action.ActionParam actionParam : action.getParamList()) {
            actionData.add(format("'%s': { 'value': '%s' }", actionParam.getParamName(), actionParam.getParamValue()));
        }

        String actionArgStr;
        if (actionData.isEmpty()) {
            actionArgStr = format("{'type': '%s'}", action.getAction());
        }
        else {
            actionArgStr = format("{'type': '%s', 'data': { %s }}", action.getAction(), actionData.stream().collect(Collectors.joining(", ")));
        }

        return addFlowRule(rteHost, rtePort, table, ruleName, priority, timeout, matchArgStr, actionArgStr);
    }

    public synchronized RteCliResponse deleteFlowRule(String rteHost, String rtePort, String table, String ruleName) {
        String cmd = buildCmd(RTE_CLI_PATH, RTE_HOST_ARG, rteHost, RTE_PORT_ARG, rtePort, RTE_JSON_RS_ARG,
                TABLES, DELETE, TABLE_ARG, table, RULE_NAME_ARG, ruleName);

        CommandLine commandLine = new CommandLine(SHELL);
        commandLine.addArgument(SHELL_FILE_ARG);
        commandLine.addArgument(cmd, false);

        log.info("delete flow rule: rteHost={}, rtePort={}, table={}. ruleName={}",
                rteHost, rtePort, table, ruleName);
        RteCliResponse response = execute(commandLine);
        return response;
    }

    private String buildCmd(String... argments) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : argments) {
            stringBuilder.append(arg);
            stringBuilder.append(SPACE);
        }

        return stringBuilder.toString();
    }

    private synchronized RteCliResponse addFlowRule(String rteHost, String rtePort, String table, String ruleName, int priority, long timeout, String match, String action) {
        if (!checkRteCli()) {
            return new RteCliResponse(false, RTE_CLI_PATH + " file not exist");
        }

        String cmd = buildCmd(RTE_CLI_PATH, RTE_HOST_ARG, rteHost, RTE_PORT_ARG, rtePort, RTE_JSON_RS_ARG,
                TABLES, ADD, TABLE_ARG, table, RULE_NAME_ARG, ruleName, RULE_MATCH_ARG, "'" + match + "'", RULE_ACTION_ARG, "'" + action + "'");

        if (priority > 0) {
            cmd = buildCmd(cmd, priority + "");
        }

        if (timeout > 0) {
            cmd = buildCmd(cmd, timeout + "");
        }

        CommandLine commandLine = new CommandLine(SHELL);
        commandLine.addArgument(SHELL_FILE_ARG);
        commandLine.addArgument(cmd, false);

        log.info("add flow rule: rteHost={}, rtePort={}, table={}. ruleName={}, priority={}, timeout={}, match={}, action={}",
                rteHost, rtePort, table, ruleName, priority, timeout, match, action);
        RteCliResponse response = execute(commandLine);

        log.debug("cmd={}", commandLine.toString());
        return response;
    }

    public synchronized String getFlowRules(String rteHost, String rtePort, String table) {
        return null;
    }

    private String format(String format, String... params) {
        return String.format(format, (Object[]) params).replaceAll("'", "\"");
    }
}
