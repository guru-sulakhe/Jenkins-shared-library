#!groovy

// Declaring a function
def decidePipeline(Map configMap) {
    type = configMap.get("type")
    switch(type) {
        case "nodejsEKS":
            nodejsEKS(configMap)   //this will call the nodejsEKS def call(Map configMap) pipeline
        break
        case "nodejsVM":
            nodejsVM(configMap)    //this will call the nodejsVM def call(Map configMap) pipeline
        break
        default:
            error "type is not matched"
            break
    }
}