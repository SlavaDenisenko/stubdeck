package com.stubdeck.model;

public enum MappingTemplate {

    REST_GET("REST GET", "Simple GET endpoint returning JSON", """
            {
              "request": { "method": "GET", "url": "/api/resource" },
              "response": {
                "status": 200,
                "jsonBody": { "id": 1, "name": "example" },
                "headers": { "Content-Type": "application/json" }
              }
            }
            """),

    REST_POST("REST POST", "POST endpoint accepting and returning JSON", """
            {
              "request": {
                "method": "POST",
                "url": "/api/resource",
                "headers": { "Content-Type": { "equalTo": "application/json" } }
              },
              "response": {
                "status": 201,
                "jsonBody": { "id": 2, "created": true },
                "headers": { "Content-Type": "application/json" }
              }
            }
            """),

    REST_PUT("REST PUT", "Update endpoint", """
            {
              "request": {
                "method": "PUT",
                "url": "/api/resource/1"
              },
              "response": {
                "status": 200,
                "jsonBody": { "id": 1, "updated": true },
                "headers": { "Content-Type": "application/json" }
              }
            }
            """),

    REST_DELETE("REST DELETE", "Delete endpoint returning 204", """
            {
              "request": { "method": "DELETE", "url": "/api/resource/1" },
              "response": { "status": 204 }
            }
            """),

    ERROR_400("Error 400", "Bad request response", """
            {
              "request": { "method": "POST", "url": "/api/resource" },
              "response": {
                "status": 400,
                "jsonBody": { "error": "Bad Request", "message": "Invalid input" },
                "headers": { "Content-Type": "application/json" }
              }
            }
            """),

    ERROR_404("Error 404", "Not found response", """
            {
              "request": { "method": "GET", "url": "/api/resource/999" },
              "response": {
                "status": 404,
                "jsonBody": { "error": "Not Found" },
                "headers": { "Content-Type": "application/json" }
              }
            }
            """),

    ERROR_500("Error 500", "Internal server error", """
            {
              "request": { "method": "GET", "url": "/api/resource" },
              "response": {
                "status": 500,
                "jsonBody": { "error": "Internal Server Error" },
                "headers": { "Content-Type": "application/json" }
              }
            }
            """),

    RESPONSE_TEMPLATING("Response Templating", "Dynamic response using WireMock templating", """
            {
              "request": { "method": "GET", "urlPathPattern": "/api/resource/([0-9]+)" },
              "response": {
                "status": 200,
                "jsonBody": "{ \\"id\\": {{request.pathSegments.[1]}}, \\"path\\": \\"{{request.path}}\\", \\"timestamp\\": \\"{{now format='yyyy-MM-dd'}}\\" }",
                "headers": { "Content-Type": "application/json" },
                "transformers": ["response-template"]
              }
            }
            """),

    STATEFUL("Stateful / Scenarios", "Mapping with scenario state", """
            {
              "scenarioName": "my-scenario",
              "requiredScenarioState": "Started",
              "newScenarioState": "Step2",
              "request": { "method": "GET", "url": "/api/resource" },
              "response": {
                "status": 200,
                "jsonBody": { "state": "initial" },
                "headers": { "Content-Type": "application/json" }
              }
            }
            """);

    public final String label;
    public final String description;
    public final String json;

    MappingTemplate(String label, String description, String json) {
        this.label = label;
        this.description = description;
        this.json = json.strip();
    }
}
