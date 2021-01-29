package cn.pcl.firewall.rtecli;

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

    public static final String RTE_CLI_PATH = "/opt/netronome/p4/bin/rtecli";
    public static final String BLANK = " ";

    public static final String RTE_HOST_ARG = "-r";
    public static final String RTE_PORT_ARG = "-p";

    public static final String RTE_STATUS = "status";
    public static final String RTE_PORTS = "ports";
    public static final String RTE_LIST = "list";

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

    private RteCliResponse execute(CommandLine commandLine) {
        DefaultExecutor exec = new DefaultExecutor();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

        ExecuteWatchdog watchdog = new ExecuteWatchdog(5*1000);
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

    /**
     * /opt/netronome/p4/bin/rtecli -r rteHost -p rtePort status
     * @param rteHost
     * @param rtePort
     * @return
     */
    public synchronized boolean connectNic(String rteHost, String rtePort) {
        if (!checkRteCli()) {
            return false;
        }

        CommandLine commandLine = new CommandLine(RTE_CLI_PATH);

        commandLine.addArgument(RTE_HOST_ARG);
        commandLine.addArgument(rteHost);
        commandLine.addArgument(RTE_PORT_ARG);
        commandLine.addArgument(rtePort);
        commandLine.addArgument(RTE_STATUS);

        RteCliResponse response = execute(commandLine);
        return response.isResult();
    }

    // /opt/netronome/p4/bin/rtecli -r 192.168.67.143 -p 20206 ports list
    public synchronized String getNicPorts(String rteHost, String rtePort) {
        if (!checkRteCli()) {
            return null;
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
            return response.getMessage();
        }

        return null;
    }

    /**
     * /opt/netronome/p4/bin/rtecli -r RTE_HOST -p RTE_PORT design-load -f nffwPath -p designPath -c p4cfgPath
     * @param nffwPath
     * @param designPath
     * @param p4cfgPath
     * @return
     */
    public synchronized String setPipeline(String nffwPath, String designPath, String p4cfgPath) {
        return null;
    }

    public synchronized String getPortStats(String id) {
        return null;
    }

    public synchronized String applyFlowRule(String id, String table, String method, String priority, String match, String action) {
        return null;
    }

    public synchronized String getFlowRules(String id, String table) {
        return null;
    }
}
