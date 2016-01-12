package com.obzen.cep;

import com.obzen.cep.core.definition.ComponentDefinition;
import com.obzen.cep.core.definition.DefinitionId;
import com.obzen.cep.core.definition.DefinitionMeta;
import com.obzen.cep.core.definition.StartupOptions;
import com.obzen.cep.core.deployment.*;
import com.obzen.cep.core.util.JsonUtil;
import com.obzen.cep.eventprocessor.EventProcessorConfig;
import com.owlab.restful.weakrest.WeakRestClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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

    private static final String baseUrl = "http://172.17.8.101:8082/rest/nodes/";
    private static final String nodeId = "node.172.17.8.101.5800";
    //private static final String baseUrl = "http://localhost:8082/rest/nodes/";
    //private static final String nodeId = "node.localhost.5800";
    //private static final String baseUrl = "http://192.168.10.83:8082/rest/nodes/";
    //private static final String nodeId = "node.192.168.10.83.5800";
    private static final String targetUrl = baseUrl + nodeId + "/services";

    public static void main(String... args) throws Exception {

        AdminRestAPITester tester = new AdminRestAPITester();
        tester.startComponents();
        //tester.stopComponents();
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

    public void startComponents() throws Exception {

        String fileUri = AdminRestAPITester.class.getClassLoader().getResource("meetup_event_cq.json").toURI().toString();
        String content = readFile(fileUri.split(":")[1]);

        JsonObject compDefJson = new JsonObject(content);

        System.out.println(compDefJson.encodePrettily());

        ComponentDefinition epCompDef = ComponentDefinition.fromJson(compDefJson);
        //Test start 
        WeakRestClient.RestResponse res21 = WeakRestClient.put(targetUrl)
            .body(new JsonObject().put("body", new JsonArray().add(epCompDef.toJson())).toString())
            .execute();
    }

    public void stopComponents() throws Exception {
        //assertThat(res21.statusCode, is(200));

        // //Test get 
        // WeakRestClient.RestResponse res22 = WeakRestClient.get(targetUrl)
        //         .execute();
        // assertThat(res22.statusCode, is(200));
        // System.out.println("Running deployments: \n" + new JsonObject(res22.responseBody).encodePrettily());

        ////Test stop with invalid id
        //WeakRestClient.RestResponse res23 = WeakRestClient.delete(targetUrl + "/sampleEventProcessor:0.0")
        //        .execute();
        //System.out.println("Status code: " + res23.statusCode);
        //System.out.println("Reply: \n" + new JsonObject(res23.responseBody).encodePrettily());

        //Test stop 
        WeakRestClient.RestResponse res24 = WeakRestClient.delete(targetUrl + "/" + "meetup-event-cq:0.1")
            .execute();
        //assertThat(res24.statusCode, is(200));
    }
}
