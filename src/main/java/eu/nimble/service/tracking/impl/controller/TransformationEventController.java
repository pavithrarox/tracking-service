package eu.nimble.service.tracking.impl.controller;

import io.swagger.annotations.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Api(tags = {"Transformation Event Operation" })
@RestController
public class TransformationEventController {
    @Value("${spring.epcis.url}")
    private String epcisURL;

    @Autowired
    private RestTemplate restTemplate;

    @ApiOperation(value = "Get transformation event for the given EPC",
            notes = "Get transformation event by epc", response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "success"),
            @ApiResponse(code = 400, message = "epc is not valid?"),
            @ApiResponse(code = 401, message = "Unauthorized. Are the headers correct?"), })
    @PostMapping("/getEpcTransformationOutput")
    public ResponseEntity<?> getEpcTransformationOutput(@ApiParam(value = "EPC item value", required = true) @RequestParam("epc") String epc,
            @ApiParam(value = "The Bearer token provided by the identity service", required = true)
            @RequestHeader(value = "Authorization", required = true)  String bearerToken) {

        JSONArray transformationEventList = getJsonEPCList(bearerToken);
        JSONObject traceTreeObject = new JSONObject();

        JSONObject entityObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        while(true) {
            entityObject = getEntityObject(epc, transformationEventList);
            if(entityObject.length() == 0) {
                JSONObject object = new JSONObject();
                object.put("epc", epc);
                JSONObject object1 = new JSONObject();
                object1.put("Entity", object);
                jsonArray.put(object1);
               break;
            } else {
                jsonArray.put(entityObject);
                epc = entityObject.getJSONObject("Entity").getString("hasOutput");
            }
        }

        traceTreeObject.put("traceTree", jsonArray);
        return new ResponseEntity<>( traceTreeObject.toString(), HttpStatus.OK);
    }

    private JSONObject getEntityObject(String epc, JSONArray transformationEventList) {

        JSONObject obj = new JSONObject();
        for (int i = 0; i < transformationEventList.length(); i++) {
            String inputEPC = transformationEventList.getJSONObject(i).getJSONArray("inputEPCList")
                    .getJSONObject(0).getString("epc");
            JSONObject entityObject = new JSONObject();
            if(epc.equals(inputEPC)) {
                String outputEPC = transformationEventList.getJSONObject(i).getJSONArray("outputEPCList")
                        .getJSONObject(0).getString("epc");
                entityObject.put("epc", epc);
                entityObject.put("hasOutput", outputEPC);
                obj.put("Entity", entityObject);
            }
        }
        return obj;
    }

    private JSONArray getJsonEPCList( String bearerToken) {
        // define url
        String url = epcisURL.trim();
        if(!url.endsWith("/"))
        {
            url = url + "/";
        }
        String eventType = "TransformationEvent";
        url = url + "/Poll/SimpleEventQuery?format=JSON&eventType=" + eventType;

        // get the event data list from the epcis application
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerToken);

        HttpEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Object>(headers),
                String.class);
        JSONArray transformationEventList = new JSONArray(response.getBody());
        return transformationEventList;
    }
}