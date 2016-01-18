package com.obzen.cep;

import com.obzen.cep.core.definition.ComponentDefinition;
import com.obzen.cep.core.definition.DefinitionId;
import com.obzen.cep.core.definition.DefinitionMeta;
import com.obzen.cep.core.definition.StartupOptions;
import com.obzen.cep.core.util.JsonUtil;
//import com.obzen.cep.eventprocessor.EventProcessorConfig;
import com.owlab.restful.weakrest.WeakRestClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class AdminRestAPITester {

    public static void main(String... args) throws Exception {
        //Parse arguments
        OptionParser parser = new OptionParser();
        OptionSpec<String> restHost = parser.accepts("e").withRequiredArg().ofType(String.class).describedAs("RESTful endPoint host-port, ex: 192.168.10.83:8082");
        OptionSpec<String> node = parser.accepts("n").withRequiredArg().ofType(String.class).describedAs("Node ID, ex: node.192.168.10.83.5800");
        OptionSpec<String> compJson = parser.accepts("j").withRequiredArg().ofType(String.class).describedAs("a component json file path, ex: resources/example_component.json");
        OptionSpec<String> cmd = parser.accepts("c").withRequiredArg().ofType(String.class).describedAs("start or stop");

        OptionSet options = parser.parse(args);
        
        if(!(options.has(restHost) && options.hasArgument(restHost) && 
                    options.has(node) && options.hasArgument(node) &&
                    options.has(compJson) && options.hasArgument(compJson) &&
                    options.has(cmd) && options.hasArgument(cmd))) {
            parser.printHelpOn(System.out);
            System.exit(0);
        } 

        String targetUrl = "http://" + options.valueOf(restHost) + "/rest/nodes/" + options.valueOf(node) + "/services";
        System.out.println("Given target service: " + targetUrl);
        String filePath = options.valueOf(compJson);
        String command = options.valueOf(cmd);

        AdminRestAPITester tester = new AdminRestAPITester();
        
        if(command.equalsIgnoreCase("start")) {
            tester.startComponents(targetUrl, filePath);
        } else if(command.equalsIgnoreCase("stop")) {
            tester.stopComponents(targetUrl, filePath);
        }

    }

    private String readFile(String filePath) {
        Charset charset = Charset.forName("UTF-8");
        Path path = FileSystems.getDefault().getPath(filePath);
        String result = null;
        try(BufferedReader reader = Files.newBufferedReader(path, charset)) {
            result = reader.lines().reduce(new StringBuffer(), (sb, line) -> sb.append(line), (sb0, sb1) -> sb0.append(sb1)).toString();
        } catch(IOException e) {
            System.err.format("IOException: %s%n", e);
        }

        return result;
    }

    public void startComponents(String targetUrl, String filePath) throws Exception {

        //String fileUri = AdminRestAPITester.class.getClassLoader().getResource("meetup_event_cq.json").toURI().toString();
        //String content = readFile(fileUri.split(":")[1]);
        String content = readFile(filePath);

        JsonObject compDefJson = new JsonObject(content);

        System.out.println("Following will be submitted");
        System.out.println("============================");
        System.out.println(compDefJson.encodePrettily());
        System.out.println("============================");

        ComponentDefinition epCompDef = ComponentDefinition.fromJson(compDefJson);
        //Test start 
        WeakRestClient.RestResponse res21 = WeakRestClient.put(targetUrl)
            .body(new JsonObject().put("body", new JsonArray().add(epCompDef.toJson())).toString())
            .execute();
        System.out.println(res21.responseBody);
    }

    public void stopComponents(String targetUrl, String filePath) throws Exception {
        String content = readFile(filePath);
        JsonObject compDefJson = new JsonObject(content);
        String defId = getDefId(compDefJson);
        System.out.println("Stopping a component with definition Id: " + defId);

        WeakRestClient.RestResponse res24 = WeakRestClient.delete(targetUrl + "/" + defId)
            .execute();
        System.out.println(res24.responseBody);
    }

    private String getDefId(JsonObject compDefJson) {
        JsonObject defMetaJson = compDefJson.getJsonObject("definitionMeta");
        JsonObject defIdJson = defMetaJson.getJsonObject("definitionId");
        String defName = defIdJson.getString("name");
        String defVersion = defIdJson.getString("version");

        String defId = defName + ":" + defVersion;
        return defId;
    }
}
