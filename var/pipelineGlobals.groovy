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
            return "637423540068" 
        case 'qa':
            return "637423540068"
        case 'uat':
            return "637423540068"
        case 'pre-prod':
            return "637423540068"
        case 'prod':
            return "637423540068"
        default:
            return "nothing"
    } 
}