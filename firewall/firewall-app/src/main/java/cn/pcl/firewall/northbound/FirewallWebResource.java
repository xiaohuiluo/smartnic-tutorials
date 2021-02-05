package cn.pcl.firewall.northbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.DeviceId;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.onlab.util.Tools.readTreeFromStream;

@Path("firewall")
public class FirewallWebResource extends AbstractWebResource {

    @POST
    @Path("firewall")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response configPortMeter(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonTree = readTreeFromStream(mapper, input);
            if (!Optional.ofNullable(jsonTree).isPresent()) {
                throw new IllegalArgumentException("request body is not illgal");
            }

            String deviceId = jsonTree.get("deviceId").asText();

            ObjectNode jsonNode = mapper.createObjectNode();
            jsonNode.put("result",true);
            jsonNode.put("msg", "success");

            return Response.ok(jsonNode).build();
        } catch (IOException e) {
            throw new IllegalArgumentException("request body is not illgal");
        }
    }
}
