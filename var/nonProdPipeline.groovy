pipeline {
    agent {
        label 'AGENT-1'
    }
    options {
        timeout(time: 30, unit: 'MINUTES') 
        disableConcurrentBuilds()
        ansiColor('xterm')
    }
    environment{
        def appVersion = '' // global variable which can be accessed anywhere within the file
        nexusUrl = pipelineGlobals.nexusURL()
        region = pipelineGlobals.region()
        account_id = pipelineGlobals.account_id()
        component = configMap.get("component")
        project = configMap.get("project")
        releaseExists = ''
    }
    stages {
        stage('Deploy'){
            steps {
                script{
                     // Deploy to specific environment like QA,UAT,PERF,etc.
                } 
            }
        }
        stage('Integration Testing') {
            steps { //installing nodejs dependencies and printing appVersion
                
            }
        }

        post { 
        always { 
            echo 'I will always say Hello again!'
            //deleteDir()
        }
        success { 
            echo 'I will run only when pipeline is success'
        }
        failure { 
            echo 'I will run only when pipeline is failure'
        }
    }
    }
}