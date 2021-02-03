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
import java.util.regex.Pattern;

public class NfpRteCliController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final Pattern pTableEntry = Pattern.compile("TableEntry\\(priority=(\\d+), rule_name='([^']+)', default_rule=(True|False), actions='\\{ ([^']*) \\}', match='\\{ ([^']*) \\}'");
    private static final Pattern pMatch = Pattern.compile("\"([^\"]+)\" : \\{  \"value\" : \"([^\"]+)\" \\}");
    private static final Pattern pAction = Pattern.compile("\"type\" : \"([^\"]+)\",  \"data\" : \\{ (.*) \\}");
    private static final Pattern pData = Pattern.compile("\"([^\"]+)\" : \\{ \"value\" : \"([^\"]+)\" \\}");

    public static final String REG_SINGER_QUOTE = "\'";
    public static final String REG_TRUE = "True";

    public static final String RTE_CLI_PATH = "/opt/netronome/p4/bin/rtecli";
    public static final String BLANK = " ";

    public static final long DEFAULT_TIMEOUT = 10000;

    public static final String RTE_HOST_ARG = "-r";
    public static final String RTE_PORT_ARG = "-p";

    public static final String RTE_STATUS = "status";
    public static final String RTE_PORTS = "ports";
    public static final String RTE_LIST = "list";

    public static final String DESIGN_LOAD = "design-load";
    public static final String NFFW_ARG = "-f";
    public static final String DESIGN_ARG = "-p";
    public static final String P4CFG_ARG = "-c";

    public static final String TABLES = "tables";
    public static final String TABLE_ARG = "-t";
    public static final String RULE_NAME_ARG = "-r";
    public static final String RULE_MATCH_ARG = "-m";
    public static final String RULE_ACTION_ARG = "-a";



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
            log.warn("Error during RTECLI execution: {}", e.getMessage(), e);
            String cmdLine = commandLine.toString().trim();
            if (!cmdLine.isEmpty()) log.debug(cmdLine);
            String outStr = outputStream.toString().trim();
            if (!outStr.isEmpty()) log.debug(outStr);
            return new RteCliResponse(false);
        }
    }

    private RteCliResponse execute(CommandLine commandLine) {
        return execute(commandLine, DEFAULT_TIMEOUT);
    }

    /**
     * /opt/netronome/p4/bin/rtecli -r rteHost -p rtePort status
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
        commandLine.addArgument(RTE_STATUS);

        RteCliResponse response = execute(commandLine);
        String msg = response.getMessage().replaceAll(REG_SINGER_QUOTE, "\"").replaceAll(REG_TRUE, "true");
        response.setMessage(msg);

        return response;
    }

    // /opt/netronome/p4/bin/rtecli -r 192.168.67.143 -p 20206 ports list
    public synchronized String getNicPorts(String rteHost, String rtePort) {
        if (!checkRteCli()) {
            return "[]";
        }

        CommandLine commandLine = new CommandLine(RTE_CLI_PATH);

        commandLine.addArgument(RTE_HOST_ARG);
        commandLine.addArgument(rteHost);
        commandLine.addArgument(RTE_PORT_ARG);
        commandLine.addArgument(rtePort);
        commandLine.addArgument(RTE_PORTS);
        commandLine.addArgument(RTE_LIST);

        RteCliResponse response = execute(commandLine);
        if (response.isResult()) {
            String msg = response.getMessage();
            return msg.replaceAll(REG_SINGER_QUOTE, "\"");
        }

        return "[]";
    }

    /**
     * /opt/netronome/p4/bin/rtecli -r RTE_HOST -p RTE_PORT design-load -f nffwPath -p designPath -c p4cfgPath
     * @param rteHost
     * @param rtePort
     * @param nffwPath
     * @param designPath
     * @param p4cfgPath
     * @return
     */
    public synchronized boolean designLoad(String rteHost, String rtePort, String nffwPath, String designPath, String p4cfgPath) {
        // check status
        RteCliResponse statusRs = getStatus(rteHost, rtePort);
        if (!statusRs.isResult()) {
            return false;
        }

        String statusMsg = statusRs.getMessage();
        try {
            StatusBean bean = OBJECT_MAPPER.readValue(statusMsg, StatusBean.class);
            if (bean.isIs_loaded()) {
                log.warn("rteHost={}, rtePort={} design is loaded", rteHost, rtePort);
                return true;
            }
        }
        catch (IOException e) {
            log.error("Failed to get status of rteHost={}, rtePort={}, exception={}", rteHost, rtePort, e);
            return false;
        }

        // design load
        CommandLine commandLine = new CommandLine(RTE_CLI_PATH);

        commandLine.addArgument(RTE_HOST_ARG);
        commandLine.addArgument(rteHost);
        commandLine.addArgument(RTE_PORT_ARG);
        commandLine.addArgument(rtePort);
        commandLine.addArgument(DESIGN_LOAD);
        commandLine.addArgument(NFFW_ARG);
        commandLine.addArgument(nffwPath);
        commandLine.addArgument(DESIGN_ARG);
        commandLine.addArgument(designPath);

        if (p4cfgPath == null || p4cfgPath.isEmpty()) {
            commandLine.addArgument(P4CFG_ARG);
            commandLine.addArgument(p4cfgPath);
        }

        RteCliResponse response = execute(commandLine, 20000);
        return response.isResult();
    }

    public synchronized String getPortStats(String rteHost, String rtePort) {
        return null;
    }


    public synchronized String addFlowRule(String rteHost, String rtePort, String table, String ruleName, String priority, String match, String action) {
        CommandLine commandLine = new CommandLine(RTE_CLI_PATH);

        commandLine.addArgument(RTE_HOST_ARG);
        commandLine.addArgument(rteHost);
        commandLine.addArgument(RTE_PORT_ARG);
        commandLine.addArgument(rtePort);
        commandLine.addArgument(TABLES);

        commandLine.addArgument(TABLE_ARG);
        commandLine.addArgument(table);

        commandLine.addArgument(RULE_NAME_ARG);
        commandLine.addArgument(ruleName);

        commandLine.addArgument(RULE_MATCH_ARG);
        commandLine.addArgument(match);

        commandLine.addArgument(RULE_ACTION_ARG);
        commandLine.addArgument(action);

        RteCliResponse response = execute(commandLine);
        if (response.isResult()) {
            return response.getMessage();
        }

        return "{}";
    }

    public synchronized String getFlowRules(String rteHost, String rtePort, String table) {
        return null;
    }
}
