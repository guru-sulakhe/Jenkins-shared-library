static final String region(){
    return "us-east-1"
}
static final String nexusURL(){
    return "nexus.guru97s.cloud:8081"
}
static final String account_id(){
    return "637423540068"
}

def getAccountID(String environment){
    switch(environment) { 
        case 'dev': 
            return "315069654700"
        case 'qa':
            return "315069654700"
        case 'uat':
            return "315069654700"
        case 'pre-prod':
            return "315069654700"
        case 'prod':
            return "315069654700"
        default:
            return "nothing"
    } 
}